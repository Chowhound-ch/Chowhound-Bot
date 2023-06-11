package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import org.springframework.core.annotation.AnnotationUtils
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.config.EventClassUtil.getIfEvent
import per.chowhound.bot.mirai.framework.config.EventClassUtil.isEvent
import per.chowhound.bot.mirai.framework.config.EventClassUtil.isMessageEvent
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 使用该事件监听扫描注册方法:
 *  1.复制该文件所有内容
 *      可删除Listener注解中的属性“val permit: PermitEnum”。
 *      同时删除import中的“import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum”。
 *  2.yml中添加配置
 *
 */

/**
 * @author Chowhound
 * @date   2023/6/8 - 16:45
 * @description key: 拥有监听方法的bean, value: list集合，其中的元素pair.first为监听方法，pair.second为监听的事件类型
 */
class ListenerFunctions: MutableMap<Any, MutableList<Pair<KFunction<*>, KClass<out Event>>>> by mutableMapOf()

internal val log = LoggerFactory.getLogger(MiraiEventListenerConfiguration::class.java)

/**
 * @author Chowhound
 * @date   2023/6/11 - 19:26
 * @description  持续会话
 * 返回值为空则表示超时
 */
suspend inline fun  <reified E : Event> E.waitMessage(
    time: Duration = 15.seconds,
    noinline filter: (E) -> Boolean = { true }
): E? {
    val deferred = CoroutineScope(Dispatchers.IO).async {
        GlobalEventChannel.asFlow()
            .filterIsInstance<E>()
            .filter(filter)
            .first()
    }

    return withTimeoutOrNull(time) { deferred.await() }
}
suspend inline fun  <reified E : Event> waitMessage(
    time: Duration = 15.seconds,
    noinline filter: (E) -> Boolean = { true }
): E? {
    val deferred = CoroutineScope(Dispatchers.IO).async {
        GlobalEventChannel.asFlow()
            .filterIsInstance<E>()
            .filter(filter)
            .first()
    }

    return withTimeoutOrNull(time) { deferred.await() }
}
/**
 * @author Chowhound
 * @date   2023/6/6 - 10:50
 * 扫描并注册事件监听
 */
@Configuration
class MiraiEventListenerConfiguration(
    val bot: Bot
) {

    val listenerFunctions = ListenerFunctions()
    @Autowired
    lateinit var context: ApplicationContext



    @PostConstruct
    fun init() {
        // TODO: 根据SpringBoot启动类自动获取包扫描路径
        ClassUtil.scanPackage("per.chowhound.bot.mirai").forEach { clazz ->

            if (clazz == MiraiEventListenerConfiguration::class.java){
                return@forEach
            }

            val bean = try {
                context.getBean(clazz) // 监听方法所在的类的实例
            } catch (e: NoSuchBeanDefinitionException){
                return@forEach
            }
            parseEventOfClass(clazz, bean)

        }


        // 开始注册监听器
        bot.eventChannel.subscribeMessages {
            listenerFunctions.forEach{ entry ->
                entry.value.forEach { pair ->
                    val listener = AnnotationUtils.getAnnotation(pair.first.javaMethod!!, Listener::class.java)
                        ?: throw RuntimeException("不是监听器")


                    log.debug("注册事件监听({}):{} -> {}({})", pair.second.simpleName, entry.key.javaClass.simpleName,
                        pair.first.name, listener.desc)
                    // 正则匹配时获取方法参数
                    fun MessageEvent.getParams(matchResult: MatchResult): Array<Any?> {
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
                                    params.add(matchResult.groups[name]?.value)
                                }
                            }
                        }
                        return params.toTypedArray()
                    }


                    if (pair.second.isMessageEvent()){

                        when (listener.matchType) {
                            MatchType.TEXT_EQUALS -> case(listener.value) {
                                pair.first.callSuspend(entry.key, this)
                            }
                            MatchType.TEXT_EQUALS_IGNORE_CASE -> case(listener.value, ignoreCase = true) {
                                pair.first.callSuspend(entry.key, this)
                            }
                            MatchType.TEXT_STARTS_WITH -> startsWith(listener.value) {
                                pair.first.callSuspend(entry.key, this)
                            }
                            MatchType.TEXT_ENDS_WITH -> endsWith(listener.value) {
                                pair.first.callSuspend(entry.key, this)
                            }
                            MatchType.TEXT_CONTAINS -> contains(listener.value) {
                                pair.first.callSuspend(entry.key, this)
                            }
                            MatchType.REGEX_MATCHES -> matching(listener.value.parseReg().toRegex()) {
                                pair.first.callSuspend(*getParams(it))

                            }
                            MatchType.REGEX_CONTAINS -> finding(listener.value.parseReg().toRegex()) {
                                pair.first.callSuspend(*getParams(it))
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


    fun parseEventOfClass(clazz: Class<*>, bean: Any) = clazz.declaredMethods.forEach { functionJava ->
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


/**
 * @author Chowhound
 * @date   2023/6/6 - 14:33
 */
@Suppress("unused")
object EventClassUtil {
    fun KClass<*>.isGroupMessageEvent(): Boolean {
        return this == GroupMessageEvent::class
    }

    fun KClass<*>.isFriendMessageEvent(): Boolean {
        return this == FriendMessageEvent::class
    }

    fun KClass<*>.isMessageEvent(): Boolean {
        return this.isSubclassOf(MessageEvent::class)
    }

    fun KClass<*>.isEvent(): Boolean {
        return this.isSubclassOf(Event::class)
    }

    @Suppress("UNCHECKED_CAST")
    fun KClass<*>.getIfEvent(): KClass<out Event>? {
        return if (Event::class.java.isAssignableFrom(this.java)) {
            this as KClass<out Event>
        } else { null }
    }

    @Suppress("UNCHECKED_CAST")
    fun KClass<*>.getIfMessageEvent(): KClass<out MessageEvent>? {
        return if (this.isMessageEvent()) {
            this as KClass<out MessageEvent>
        }else{ null }
    }

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
enum class MatchType{
    /**
     * 全等匹配
     */
    TEXT_EQUALS,

    /**
     * 忽略大小写的全等匹配
     */
    TEXT_EQUALS_IGNORE_CASE,


    /**
     * 开头匹配
     */
    TEXT_STARTS_WITH,

    /**
     * 尾部匹配.
     */
    TEXT_ENDS_WITH,

    /**
     * 包含匹配.
     */
    TEXT_CONTAINS,

    /**
     * 正则完全匹配. `regex.matches(...)`
     */
    REGEX_MATCHES,


    /**
     * 正则包含匹配. `regex.find(...)`
     */
    REGEX_CONTAINS,

    ;



}

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FilterValue(val value: String)