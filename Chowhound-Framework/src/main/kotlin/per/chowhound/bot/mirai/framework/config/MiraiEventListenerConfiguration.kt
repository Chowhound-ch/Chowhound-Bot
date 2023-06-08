package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.subscribeMessages
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import org.springframework.core.annotation.AnnotationUtils
import per.chowhound.bot.mirai.framework.common.utils.EventClassUtil.getIfEvent
import per.chowhound.bot.mirai.framework.common.utils.EventClassUtil.isEvent
import per.chowhound.bot.mirai.framework.common.utils.EventClassUtil.isMessageEvent
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
/**
 * @author Chowhound
 * @date   2023/6/8 - 16:45
 * @description key: 拥有监听方法的bean, value: list集合，其中的元素pair.first为监听方法，pair.second为监听的事件类型
 */
class ListenerFunctions: MutableMap<Any, MutableList<Pair<KFunction<*>, KClass<out Event>>>> by mutableMapOf()


/**
 * @author Chowhound
 * @date   2023/6/6 - 10:50
 */
@Configuration
class MiraiEventListenerConfiguration(
    val bot: Bot
) {

    val listenerFunctions = ListenerFunctions()

    @PostConstruct
    fun init() {
        ClassUtil.scanPackage("per.chowhound.bot.mirai").forEach { clazz ->

            if (clazz == MiraiEventListenerConfiguration::class.java){
                return@forEach
            }

            val bean = try {
                SpringUtil.getBean(clazz) // 监听方法所在的类的实例
            } catch (e: NoSuchBeanDefinitionException){
                return@forEach
            }
            parseEventOfClass(clazz, bean)

        }


        // 开始注册监听器
        bot.eventChannel.subscribeMessages {
            listenerFunctions.forEach{ entry ->
                entry.value.forEach { pair ->
                    if (pair.second.isMessageEvent()){
                        val listener = AnnotationUtils.getAnnotation(pair.first.javaMethod!!, Listener::class.java)
                            ?: throw RuntimeException("不是监听器")

                        when (listener.matchType) {
                            MatchType.TEXT_EQUALS -> case(listener.value) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.TEXT_EQUALS_IGNORE_CASE -> case(listener.value, ignoreCase = true) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.TEXT_STARTS_WITH -> startsWith(listener.value) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.TEXT_ENDS_WITH -> endsWith(listener.value) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.TEXT_CONTAINS -> contains(listener.value) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.REGEX_MATCHES -> matching(listener.value.parseReg().toRegex()) {

                                fun getParams(): Array<Any?> {
                                    val params = mutableListOf<Any?>()
                                    pair.first.parameters.forEach { param ->
                                        when (param.kind){
                                            KParameter.Kind.INSTANCE -> params.add(entry.key)
                                            KParameter.Kind.EXTENSION_RECEIVER -> params.add(this)
                                            else ->{
                                                val name =
                                                    param.annotations.find { it == FilterValue::class}
                                                        ?.let { it as FilterValue }?.value
                                                        ?: param.name
                                                        ?: throw RuntimeException("参数${param.name}没有指定名称")
                                                params.add(it.groups[name]?.value)
                                            }
                                        }
                                    }
                                    return params.toTypedArray()
                                }


//                                    pair.first.call(entry.key, this)
                                CoroutineScope(Dispatchers.IO).launch {
                                    logInfo("匹配到正则：参数: {}", pair.first.parameters)
                                    pair.first.callSuspend(*getParams())
                                }

                            }
                            MatchType.REGEX_CONTAINS -> finding(listener.value.parseReg().toRegex()) {
                                pair.first.call(entry.key, this)
                            }

                        }

                    }else{// 非MessageEvent
                        bot.eventChannel.subscribeAlways(eventClass = pair.second){
                            pair.first.call(entry.key, this)
                        }
                    }

                }
            }
        }

    }

    fun parseEventOfClass(clazz: Class<*>, bean: Any) {
        clazz.declaredMethods.forEach { functionJava ->
            val function = functionJava.kotlinFunction ?: return@forEach

            function.annotations.forEach { annotation ->
                if (annotation is Listener) {
                    // 获取扩展函数的被拓展的类型
                    val classifier = function.extensionReceiverParameter!!.type.classifier!! as KClass<*>
                    val event = classifier.getIfEvent() ?: throw RuntimeException("不是事件类型")

                    if (classifier.isEvent()){
                        listenerFunctions[bean]
                            ?.add(function to event)
                            ?: run {
                                listenerFunctions[bean] =
                                    mutableListOf(function to event)
                            }

                    }
                }
            }
        }


    }

    fun String.parseReg(): String {
        // 解析string中的{key,value}，并将其替换为正则表达式
        val builder = StringBuilder()
        this.split("{").withIndex().forEach { (index, str) ->
            if (index == 0) { builder.append(str); return@forEach }
            val strList = str.split("}")
            when (strList.size) {
                1 -> builder.append(str)
                2 -> {
                    val pattern = strList[0].split(",")
                    when (pattern.size) {
                        1 -> builder.append("(?<${pattern[0]}>.*?)")
                        2 -> {
                            builder.append("(?<${pattern[0]}>${pattern[1].replaceGroupNoCapture()})")
                        }
                    }

                }
            }

        }

        return builder.toString()
    }

    fun String.replaceGroupNoCapture() = this.replace("(", "(?:")



}



@Suppress("unused")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Listener(
    @get:AliasFor("pattern")
    val value: String = "",

    val matchType: MatchType = MatchType.REGEX_MATCHES,

    val priority: EventPriority = EventPriority.NORMAL,
    @get:AliasFor("value")
    val pattern: String = "", // 正则表达式，用于匹配消息内容

    val desc: String = "无描述",

    val isBoot: Boolean = false,// 监听是否需要开机，为 false 时关机不监听
    val permit: PermitEnum = PermitEnum.MEMBER,// 监听方法的权限
)

/**
 *
 * @author ForteScarlet
 */
enum class MatchType(
    val isPlainText: Boolean,
) {
    /**
     * 全等匹配
     *
     * @see StringMatchers.EQUALS
     */
    TEXT_EQUALS(true),

    /**
     * 忽略大小写的全等匹配
     *
     * @see StringMatchers.EQUALS_IGNORE_CASE
     */
    TEXT_EQUALS_IGNORE_CASE(true),


    /**
     * 首部匹配
     *
     * @see StringMatchers.STARTS_WITH
     */
    TEXT_STARTS_WITH(true),

    /**
     * 尾部匹配.
     *
     * @see StringMatchers.ENDS_WITH
     */
    TEXT_ENDS_WITH(true),

    /**
     * 包含匹配.
     *
     * @see StringMatchers.CONTAINS
     */
    TEXT_CONTAINS(true),

    /**
     * 正则完全匹配. `regex.matches(...)`
     *
     * @see KeywordRegexMatchers.MATCHES
     */
    REGEX_MATCHES(false),


    /**
     * 正则包含匹配. `regex.find(...)`
     *
     * @see KeywordRegexMatchers.CONTAINS
     */
    REGEX_CONTAINS(false),

    ;



}

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FilterValue(val value: String)