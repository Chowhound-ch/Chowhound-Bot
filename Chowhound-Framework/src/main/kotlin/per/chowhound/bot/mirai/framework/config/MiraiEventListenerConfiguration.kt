package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.MessageSubscribersBuilder
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.core.annotation.AliasFor
import org.springframework.core.annotation.AnnotationUtils
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.config.exception.BotException
import per.chowhound.bot.mirai.framework.config.exception.ListenerNoAnnotationException
import per.chowhound.bot.mirai.framework.config.exception.ListenerWithNoEventException
import per.chowhound.bot.mirai.framework.config.exception.PatternErrorException
import java.lang.reflect.Method
import kotlin.coroutines.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.createType
import kotlin.reflect.jvm.kotlinFunction
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * 持续会话
 */
suspend inline fun <reified E : Event> E.waitMessage(
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


//@Configuration
class MiraiEventListenerConfiguration(val bot: Bot) {

    @PostConstruct
    fun init() {

        ClassUtil.scanPackage("per.chowhound.bot.mirai").forEach { clazz ->
            if (clazz == MiraiEventListenerConfiguration::class.java) return@forEach
            val bean = try {
                SpringUtil.getBean(clazz) // 监听方法所在的类的实例
            } catch (e: NoSuchBeanDefinitionException){
                return@forEach
            }


            for (method in clazz.declaredMethods) {
                val listenerInfo = try {
                    if (method.isSynthetic) continue
                    ListenerInfo.create(bean, method)
                } catch (e: BotException) { continue }

                val listenerFunction = listenerInfo.listenerFunction

                GlobalEventChannel.filter { event ->
//                listenerFun.eventClass.isAssignableFrom(event::class.java)
                    if (event is MessageEvent){
                       return@filter listenerInfo.customPattern!!.getRegWithGroup().toRegex().find(event.message.content) != null
                    }else{
                        return@filter listenerFunction.eventClass.isAssignableFrom(event::class.java)
                    }
                }.subscribeAlways(listenerFunction.eventClass, priority = listenerFunction.listener.priority, handler = listenerInfo.getCallFunction())

//                bot.eventChannel.subscribeAlways(listenerFun.eventClass, priority = listenerFun.listener.priority, handler = listenerFun.getCallFunction())
                logInfo("注册监听器：{}.{}({})",listenerFunction.bean.javaClass, listenerFunction.method.name, listenerFunction.listener.desc)
            }
        }
    }

}



class ListenerInfo  private constructor(

    val listenerFunction: ListenerFunction,
    var customPattern: CustomPattern? = null, // 监听方法的pattern正则表达式, MessageEvent类型的事件才有
    var isMessageEvent: Boolean = false, // 是否是MessageEvent类型的事件


){

    @Suppress("UNCHECKED_CAST")
    companion object{
        fun create(bean: Any, method: Method): ListenerInfo {
            //AliasFor注解必须用该方法才能在反射时正常调用
            val listener = AnnotationUtils.getAnnotation(method, Listener::class.java)
                ?: throw ListenerNoAnnotationException("监听方法必须有Listener注解")
            val eventClass = method.parameterTypes.find { Event::class.java.isAssignableFrom(it) }
                ?: throw ListenerWithNoEventException("监听方法的参数中必须有Event类型的参数")

            method.isAccessible = true


            val listenerFunction = ListenerInfo(
                listenerFunction = ListenerFunction(bean,
                    method.kotlinFunction!!,
                    eventClass = eventClass as Class<out Event>,
                    listener
                ),
            )


            if (MessageEvent::class.java.isAssignableFrom(eventClass)){
                listenerFunction.isMessageEvent = true
                listenerFunction.customPattern = SyntaxUtil.spiltPattern(listener.pattern)
            }

            return listenerFunction
        }
    }


    // 调用监听方法的函数
    fun getCallFunction(): (Event) -> Unit = {event: Event ->
        if (event is MessageEvent && customPattern?.isExist == true){// 如果有pattern正则表达式，匹配事件消息内容
            getFilterValueArgsMap(event.message.content)?.let { argsMap ->
                // 创建一个协程并在协程中调用监听方法
                CoroutineScope(EmptyCoroutineContext).launch {
                    // 将customPattern.fieldMap.values的每一个特value都当作method的参数
                    listenerFunction.method.callSuspend(*getFunArgs(argsMap, event))
                }
            }

        }else{
            CoroutineScope(EmptyCoroutineContext).launch{
                listenerFunction.method.callSuspend(listenerFunction.bean, event)
            }
        }

    }



    /**
     * 获得反射调用该方法的参数
     */

    private fun getFunArgs(argsMap: MutableMap<String, String>, event: Event): Array<Any?>{
        val args = mutableListOf<Any?>()
        for ( kParameter in listenerFunction.method.parameters) {
            when (kParameter.kind) {
                KParameter.Kind.INSTANCE -> {
                    args.add(listenerFunction.bean)// 监听方法所在的类的实例
                }
                KParameter.Kind.EXTENSION_RECEIVER -> {
                    args.add(event) // 监听方法的第一个参数,event
                }
                else -> {
                    fun getArg(arg: String?, kParameter: KParameter): Any? {
                        var param = arg
                        param?.run {
                            if (this.startsWith("@")) {
                                param = param!!.substring(1)
                            }
                        }

                        return when(kParameter.type) {
                            Long::class.createType() -> param?.toLong()
                            Int::class.createType() -> param?.toInt()
                            Float::class.createType() -> param?.toFloat()
                            Double::class.createType() -> param?.toDouble()
                            Boolean::class.createType() -> param?.toBoolean()
                            else -> param
                        }
                    }

                    // 如果参数有FilterValue注解，则filterValue.value作为key从argsMap中取出对应的值，
                    // 否则kParameter.name作为key从argsMap中取出对应的值
                    (kParameter.annotations.find { annotation -> annotation is FilterValue } as? FilterValue)
                        ?.let { filterValue -> args.add(getArg(argsMap[filterValue.value], kParameter)) }
                        ?: run { args.add(getArg(argsMap[kParameter.name], kParameter)) }



                }
            }
        }

        return args.toTypedArray()
    }


    /**
     * 获得消息中的参数{{name,pattern}}的值
     */
    private fun getFilterValueArgsMap(msg: String): MutableMap<String, String>? {
        if (customPattern == null){// 非消息事件，
            return null
        }
        val argsMap = mutableMapOf<String, String>()
        if (!customPattern!!.isExist) {// 没有{{name,pattern}}语法
            return argsMap
        }

        val matchResult = customPattern!!.getRegWithGroup().toRegex().find(msg) ?: return null

        for (i in 1 until matchResult.groupValues.size) {
            argsMap[customPattern!!.fieldMap.keys.elementAt(i - 1)] = matchResult.groupValues[i]
        }

        return argsMap
    }

    /**
     * @Author: Chowhound
     * @Date: 2023/4/21 - 15:04
     * @Description:
     */
    @Suppress("MemberVisibilityCanBePrivate")
    class ListenerFunction (
        val bean: Any,// 监听方法所在的类的实例
        val method: KFunction<*>, // 监听方法
        var eventClass: Class<out Event>,// 监听事件的类型
        val listener: Listener, // 监听方法的Listener注解

    )

}




/**
 * @Author: Chowhound
 * @Date: 2023/4/20 - 12:47
 * @Description: pattern: /.+{{name,pattern}} 对应的CustomPattern:
 * regParts: ["/.+", "pattern"], fieldMap: ["name" to "pattern"]
 */
data class CustomPattern(
    val fieldMap: LinkedHashMap<String, String> = linkedMapOf(),// key:fieldName, value:pattern
    /**
     * 正则表达式的各个部分
     * #-为占位符，由SyntaxUtil.PLACEHOLDER定义，占位符后面的字符串为fieldName
     * abc{{name1,pattern}}def{{name2}}ghi -> [abc, #-#name1, def, #-#name2, ghi]
     */
    val regParts: MutableList<String> = ArrayList(),// 正则表达式的各个部分
    var isExist: Boolean = false,// 是否存在{{name,pattern}}语法
){
    // 用于匹配消息内容的正则表达式，带有分组 /.+(?<name>pattern)
    fun getRegWithGroup(): String {
        val regWithGroup = StringBuilder()

        val iterator = regParts.iterator()
        while (iterator.hasNext()){
            val next = iterator.next()
            if(next.startsWith(SyntaxUtil.PLACEHOLDER)){
                val fieldName = next.substring(SyntaxUtil.PLACEHOLDER.length)
                val groupPart = fieldMap[fieldName]

                // 将groupPart中的()替换为(?:)，要求不捕获
//                val replace = groupPart?.replace("\\([^(:?)]\\)", "\\(:?\\)")
                val replace = groupPart ?: throw RuntimeException("pattern中的fieldName: $fieldName 在fieldMap中不存在")



                regWithGroup.append("(?<${fieldName}>$replace)")
            }else{
                regWithGroup.append(next)
            }
        }

        return regWithGroup.toString()
    }
}

/**
 * @Author: Chowhound
 * @Date: 2023/4/21 - 15:48
 * @Description:
 */
object SyntaxUtil {
//    private const val DEFAULT_REG_PART = "\\S*" // 默认得正则表达式部分
    private const val DEFAULT_REG_PART = "\\S*" // 默认得正则表达式部分
    const val PLACEHOLDER= "#-#" // 占位符
    private val SPECIAL_CHAR = mapOf("@?" to "@?\\d{6,12}")

    // 解析{{name,pattern}}语法
    // 由于该方法仅在init阶段调用，所以不需要考虑性能，直接使用正则表达式解析{{name,pattern}}语法
    fun spiltPattern(pattern: String): CustomPattern {
        val customPattern = CustomPattern()

        val indexLe = pattern.indexOf("{{")
        val indexRi = pattern.indexOf("}}")

        val patternReal = SyntaxUtil.replaceGroup(pattern)

        if (indexLe == -1 || indexRi == -1) {// 没有{{name,pattern}}语法
            customPattern.regParts.add(patternReal)
            return customPattern
        }else if (indexLe > indexRi) { // {{name,pattern}}语法错误
            throw PatternErrorException("pattern语法错误")
        }

        patternReal.split("{{").apply {
            if (this.size > 1) {
                customPattern.isExist = true
            }
        }.withIndex().forEach{ (index, s) ->
            // abc{{name1,pattern}}def{{name2}}ghi
            // 0: abc, 1: name,pattern1}}def, 2: pattern2}}ghi
            if (index == 0) {//第一个元素为abc，不需要处理
                customPattern.regParts.add(s)
                return@forEach
            }
            val splitParts = s.split("}}")
            // 1: ["name1,pattern", "def"], 2: ["name2", "ghi"]
            when (splitParts.size) {
                1 -> customPattern.regParts.add(splitParts[0]) // 没有{{name,pattern}}语法
                2 -> {// 可能为["name1,pattern", "def"]或["name2", "ghi"]
                    val patternSplitComma = splitParts[0].split(",")

                    when(patternSplitComma.size) {
                        1 -> {
                            customPattern.fieldMap[patternSplitComma[0]] = DEFAULT_REG_PART
                        }
                        2 -> {
                            customPattern.fieldMap[patternSplitComma[0]] = SPECIAL_CHAR[patternSplitComma[1]] ?: patternSplitComma[1]
                        }
                        else -> throw PatternErrorException("pattern语法错误")
                    }
                    customPattern.regParts.add(PLACEHOLDER + patternSplitComma[0])

                    // 如果结束符}}后面还有内容，继续添加
                    if (splitParts[1] == "") { return@forEach }
                    customPattern.regParts.add(splitParts[1])
                }

                else -> throw PatternErrorException("pattern语法错误")
            }
        }



        // endregion
        return customPattern
    }

    // 将参数的reg中的()替换为(?:)，要求不捕获
    fun replaceGroup(reg: String): String {
//        val split = reg.split("(")
        // ()abc(def)g(hi(jkl))mn
        // split: [ , )abc, def)g, hi, jkl))mn]
        // abc(def)g(hi(jkl))mn
        // split: [abc, def, g, hi, jkl,))mn]

        // abc(defg(hi(jkl)mn
        // split: [abc, defg, hi, jkl)mn]

//        val stringBuilder = StringBuilder()
//
//        if (split.size == 1) {// 没有()，直接返回
//            return reg
//        }
//
//        for (element in split) {
//            if (element != "") {
//                stringBuilder.append("(?:$element")
//            }
//        }


        return  reg.replace("(", "(?:")

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