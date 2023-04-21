package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import per.chowhound.bot.mirai.framework.config.exception.ListenerNoAnnotationException
import per.chowhound.bot.mirai.framework.config.exception.ListenerWithNoEventException
import per.chowhound.bot.mirai.framework.config.utils.SyntaxUtil
import java.lang.reflect.Method
import kotlin.coroutines.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction


@Suppress("unused")
@Configuration
class MiraiEventListenerConfiguration {
    @Autowired
    private lateinit var bot: Bot

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
                val listenerFun = try {
                    ListenerFunction.create(bean, method)
                } catch (e: Exception) { continue }


                bot.eventChannel.subscribeAlways(listenerFun.eventClass, priority = listenerFun.listener.priority, handler = listenerFun.getCallFunction())
            }
        }
    }






    /**
     * @Author: Chowhound
     * @Date: 2023/4/21 - 15:04
     * @Description:
     */
    @Suppress("MemberVisibilityCanBePrivate")
    class ListenerFunction private constructor(
        val bean: Any,// 监听方法所在的类的实例
        val method: KFunction<*>, // 监听方法
        var eventClass: Class<out Event>, // 监听事件的类型
        val listener: Listener, // 监听方法的Listener注解

        var customPattern: CustomPattern? = null, // 监听方法的pattern正则表达式, MessageEvent类型的事件才有
        var isMessageEvent: Boolean = false, // 是否是MessageEvent类型的事件
    ){

        companion object{
            @Suppress("UNCHECKED_CAST")
            fun create(bean: Any, method: Method): ListenerFunction {
                val listener = method.getAnnotation(Listener::class.java) ?: throw ListenerNoAnnotationException("监听方法必须有Listener注解")
                val eventClass = method.parameterTypes.find { Event::class.java.isAssignableFrom(it) } ?: throw ListenerWithNoEventException("监听方法的参数中必须有Event类型的参数")

                method.isAccessible = true

                val listenerFunction = ListenerFunction(
                    bean = bean,
                    method = method.kotlinFunction!!,
                    eventClass = eventClass as Class<out Event>,
                    listener = listener
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
                if (event is MessageEvent && customPattern != null){// 如果有pattern正则表达式，匹配事件消息内容
                    getArgsMap(event.message.content)?.let { argsMap ->
                        // 创建一个协程并在协程中调用监听方法
                        CoroutineScope(EmptyCoroutineContext).launch {
                            // 将customPattern.fieldMap.values的每一个特value都当作method的参数
                            method.callSuspend(*getFunArgs(argsMap, event))
                        }
                    }

                }else{
                    CoroutineScope(EmptyCoroutineContext).launch{
                        method.callSuspend(bean, event)
                    }
                }

            }

        /**
         * 获得反射调用该方法的参数
         */

        fun getFunArgs(argsMap: MutableMap<String, String>, event: Event): Array<Any?>{
            val args = mutableListOf<Any?>()
            for ( kParameter in method.parameters) {
                when (kParameter.kind) {
                    KParameter.Kind.INSTANCE -> {
                        args.add(bean)
                    }
                    KParameter.Kind.EXTENSION_RECEIVER -> {
                        args.add(event)
                    }
                    else -> {
                        // 如果参数有FilterValue注解，则filterValue.value作为key从argsMap中取出对应的值，
                        // 否则kParameter.name作为key从argsMap中取出对应的值
                        (kParameter.annotations.find { annotation -> annotation is FilterValue }
                                as? FilterValue)
                            ?.let { filterValue -> args.add(argsMap[filterValue.value]) }
                            ?: run { args.add(argsMap[kParameter.name]) }
                    }
                }
            }

            return args.toTypedArray()
        }


        /**
         * 获得消息中的参数{{name,pattern}}的值
         */
        fun getArgsMap(msg: String): MutableMap<String, String>? {
            if (customPattern == null){
                return null
            }
            val argsMap = mutableMapOf<String, String>()
            if (customPattern!!.regParts.size == 1) {// 没有{{name,pattern}}语法
                return argsMap
            }
            customPattern!!.getRegWithGroup().toRegex().find(msg)?.let {
                if (it.value != msg){// 如果正则表达式匹配不到整个消息，则返回null
                    return null
                }
                for (i in 1 until it.groupValues.size) {
                    argsMap[customPattern!!.fieldMap.keys.elementAt(i - 1)] = it.groupValues[i]
                }
            }

            return argsMap
        }

    }




    /**
     * @Author: Chowhound
     * @Date: 2023/4/20 - 12:47
     * @Description: pattern: /.+{{name,pattern}} 对应的CustomPattern:
     * regParts: ["/.+", "pattern"], fieldMap: ["name" to "pattern"]
     */
    data class CustomPattern(
        val fieldMap: LinkedHashMap<String, String> = linkedMapOf(),// key:fieldName, value:pattern
        val regParts: MutableList<String> = ArrayList(),// 正则表达式的各个部分
    ){
        val desReg: String // 用于匹配消息内容的正则表达式 /.+pattern
            get() {
                return regParts.joinToString(separator = "")
            }

        // 用于匹配消息内容的正则表达式，带有分组 /.+(?<name>pattern)
        fun getRegWithGroup(): String {
            val regWithGroup = StringBuilder(regParts[0])

            val iterator = fieldMap.iterator()
            while (iterator.hasNext()){
                val next = iterator.next()
                regWithGroup.append("(?<${next.key}>${next.value})")
            }

            regWithGroup.append(regParts.last())

            return regWithGroup.toString()
        }
    }
}
@Suppress("unused")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Listener(
    val priority: EventPriority = EventPriority.NORMAL,
    val pattern: String = "", // 正则表达式，用于匹配消息内容

    val desc: String = "无描述",

    val isBoot: Boolean = true // 监听是否需要开机，为 false 时关机不监听
)

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FilterValue(val value: String)