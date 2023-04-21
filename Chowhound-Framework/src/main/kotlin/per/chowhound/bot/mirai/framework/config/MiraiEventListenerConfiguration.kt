package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.core.util.StrUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.components.state.exception.PatternErrorException
import kotlin.coroutines.*
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.jvm.kotlinFunction


@Suppress("unused")
@Configuration
class MiraiEventListenerConfiguration {
    @Autowired
    private lateinit var bot: Bot

    @Suppress("UNCHECKED_CAST")

    @PostConstruct
    fun init() {
        ClassUtil.scanPackage("per.chowhound.bot.mirai").forEach { clazz ->
            clazz.declaredMethods.forEach { method ->
                method.getAnnotation(Listener::class.java)?.let { listener ->
                    // 扫描所有的 Listener 注解，注册监听器
                    method.parameterTypes.find { Event::class.java.isAssignableFrom(it) }?.let {
                        bot.eventChannel.subscribeAlways(it as Class<Event>, priority = listener.priority) { event ->
                            if (event is MessageEvent && StrUtil.isNotBlank(listener.pattern)) {// 如果有pattern正则表达式，匹配事件消息内容
                                val msg = event.message.content
                                // TODO: 若正则表达式中含有{{name,pattern}}语法，则将{{name,pattern}}替换为pattern
                                val customPattern = spiltPattern(listener.pattern)
                                val argsMap = getArgsMap(msg, customPattern)
                                if (argsMap != null) {
                                    val bean = SpringUtil.getBean(clazz)
                                    // 创建一个协程并在协程中调用监听方法
                                    CoroutineScope(EmptyCoroutineContext).launch {
                                        // 将customPattern.fieldMap.values的每一个特value都当作method的参数
                                        method.kotlinFunction?.let { kFun ->
                                            val args = mutableListOf<Any?>()
                                            for ((index, kParameter) in kFun.parameters.withIndex()) {
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
                                                            ?.let { filterValue ->
                                                                args.add(argsMap[filterValue.value])
                                                            } ?: run {
                                                            args.add(argsMap[kParameter.name])
                                                        }
                                                    }
                                                }
                                            }

                                            kFun.callSuspend(*args.toTypedArray())
                                        }

                                    }

                                }
                            }
                        }
                        logInfo("注册监听器：${clazz.name}.${method.name}")
                    }
                }
            }
        }
    }

    private fun getArgsMap(msg: String, customPattern: CustomPattern): MutableMap<String, String>? {
        val argsMap = mutableMapOf<String, String>()
        if (customPattern.regParts.size == 1) {// 没有{{name,pattern}}语法
            return argsMap
        }
        customPattern.getRegWithGroup().toRegex().find(msg)?.let {
            if (it.value != msg){// 如果正则表达式匹配不到整个消息，则返回null
                return null
            }
            for (i in 1 until it.groupValues.size) {
                argsMap[customPattern.fieldMap.keys.elementAt(i - 1)] = it.groupValues[i]
            }
        }

        return argsMap
    }

    private fun spiltPattern(pattern: String): CustomPattern{
        val customPattern = CustomPattern()


        var tempPattern = pattern // 用于替换正则表达式中的{{name,pattern}}语法
        var prefixStr: List<String>
        do {
            prefixStr = tempPattern.split("{{")

            customPattern.regParts.add(prefixStr[0])
            if (prefixStr.size == 1){
                break
            }
            val suffixStr = prefixStr[1].split("}}")
            if (suffixStr.size == 1){
                throw PatternErrorException("表达式语法错误，缺少}}")
            }

            suffixStr[0].let {// it :  name,pattern
                val split = it.split(",")
                if (split.size == 1){
                    throw PatternErrorException("表达式语法错误，期望的语法:{{name,pattern}}")
                }
                customPattern.regParts.add(split[1])
                customPattern.fieldMap[split[0]] = split[1]
            }

            tempPattern = suffixStr[1]
        } while  (true)


        return customPattern
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