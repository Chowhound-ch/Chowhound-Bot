@file:Suppress("MemberVisibilityCanBePrivate")

package per.chowhound.bot.mirai.framework.config.listener.utils;

import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import org.springframework.context.support.beans
import org.springframework.core.annotation.AliasFor
import org.springframework.core.annotation.AnnotationUtils
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.config.EventClassUtil.getIfEvent
import per.chowhound.bot.mirai.framework.config.MatchType
import per.chowhound.bot.mirai.framework.config.listener.MiraiListenerConfiguration
import per.chowhound.bot.mirai.framework.config.listener.utils.EventClassUtil.isMessageEvent
import per.chowhound.bot.mirai.framework.config.listener.utils.PatternUtil.resolve
import java.util.Arrays
import java.util.HashMap
import java.util.Stack
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaMethod

/**
 * @author : Chowhound
 * @since : 2023/9/1 - 22:11
 */
object PatternUtil {
    private const val SEPARATOR_START = "{{"
    private const val SEPARATOR_END = "}}"

//    private const val DEFAULT_GROUP_NAME = "DEFAULT"

    // `name,\\s+`
    fun Any.resolve(): List<ListenerPatternInfo> {
        var list = ArrayList<ListenerPatternInfo>()
        this::class.declaredFunctions.forEach { function->
            val listener = AnnotationUtils.getAnnotation(function.javaMethod!!,
                Listener::class.java) ?: return@forEach
            // 解析事件监听
            // 获取扩展函数的被拓展的类型
            // (function.parameters[1].type.classifier as KClass<*>).isSubclassOf(Event::class)
            val eventClass = function.parameters.stream()
                .map { it.type.classifier as? KClass<*> }
                .filter { it != null && it.isSubclassOf(Event::class) }
                .limit(1)
                .toList()[0]
            ?: return@forEach
            val event = eventClass.getIfEvent() ?: throw RuntimeException("不是事件类型")
            val info = ListenerPatternInfo(function = function, eventClass = event, bean = this)

            if (event.isMessageEvent()){
                var patternMap = HashMap<String, String>()
                var regPattern = recur(listener.pattern, patternMap)
            } else{
                info.isMessageEvent = false
            }

        }
        return emptyList()
    }

    /**
     * @param str 后半部分的需要替换掉[SEPARATOR_START]包裹的内容的字符串
     * @param groupMap 被替换掉的[SEPARATOR_START]包裹的内容
     *
     * @return 后半部分的替换掉[SEPARATOR_START]包裹的内容之后的字符串
     *
     * @author : Chowhound
     * @since : 2023/09/02 - 14:43
     */
    fun recur(str: String, groupMap: MutableMap<String, String>): String{
        val startIndex = str.lastIndexOf(SEPARATOR_START)

        // /name={{name,a{2,14}}}or{{name2,ab{2,14}}}}111
        if (startIndex == -1){ // 没有找到 SEPARATOR_START
            return str
        }
        // /name={{name,a{2,14}}}or
        val prefix = str.substring(0, startIndex)


        val afterStartSign = str.substring(startIndex + SEPARATOR_START.length)
        val endIndex = afterStartSign.indexOf(SEPARATOR_END)

        val charQueue = ConcurrentLinkedQueue(SEPARATOR_END.toCharArray().asList())
        var i = endIndex + SEPARATOR_END.length
        while (i < afterStartSign.length){
            charQueue.remove()
            charQueue.add(afterStartSign[i])
            if (charQueue.joinToString("") != SEPARATOR_END){
                break
            }
            i++
        }
        val suffix = afterStartSign.substring(i)
        val pattern = afterStartSign.substring(0, i - SEPARATOR_END.length)
        val patternPair = pattern.split(',', limit = 2)

        when(patternPair.size){
            1 -> groupMap[patternPair[0]] = ".*?"
            2 -> groupMap[patternPair[0]] = patternPair[1].replaceGroupNoCapture()
            else -> {
                throw RuntimeException("{{name,pattern}}语法错误")
            }
        }

        return recur(prefix, groupMap) + "(?<${patternPair[0]}>${groupMap[patternPair[0]]})" + suffix
    }
    // TODO 成对的括号才替换
    fun String.replaceGroupNoCapture() = this.replace("(", "(?:")



    data class ListenerPatternInfo(
        var raw: String? = null,
        var regPattern: String? = null,
//        var regPatternWithGroup: String? = null,
        var patternMap: Map<String, String>? = null,
        var function: KFunction<*>,
        var eventClass: KClass<out Event>,
        var isMessageEvent: Boolean?  = true,
        var bean: Any
        )
}

fun main(args: Array<String>) {
    PatternUtil.recur("/name={{name,a{2,14}}}or{{name2,ab{2,14}}}111", HashMap())
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
 * @author Chowhound
 * @date   2023/6/6 - 14:33
 */
@Suppress("unused")
object EventClassUtil {

    fun KClass<*>.isGroupMessageEvent(): Boolean = this == GroupMessageEvent::class

    fun KClass<*>.isFriendMessageEvent(): Boolean = this == FriendMessageEvent::class

    fun KClass<*>.isMessageEvent(): Boolean = this.isSubclassOf(MessageEvent::class)

    fun KClass<*>.isEvent(): Boolean = this.isSubclassOf(Event::class)

    @Suppress("UNCHECKED_CAST")
    fun KClass<*>.getIfEvent(): KClass<out Event>? {
        return if (this.isEvent()) { this as KClass<out Event> } else { null }
    }

    @Suppress("UNCHECKED_CAST")
    fun KClass<*>.getIfMessageEvent(): KClass<out MessageEvent>? {
        return if (this.isMessageEvent()) { this as KClass<out MessageEvent> }else{ null }
    }
}
