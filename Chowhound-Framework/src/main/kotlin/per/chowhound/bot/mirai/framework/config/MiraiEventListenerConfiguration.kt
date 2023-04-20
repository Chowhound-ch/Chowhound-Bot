package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.core.util.StrUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.components.state.exception.PatternErrorException
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.jvm.internal.impl.load.java.typeEnhancement.AbstractSignatureParts


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
                                // TODO: 若正则表达式中含有{{name,pattern}}语法，则将{{name,pattern}}替换为pattern
                                val customPattern = spiltPattern(listener.pattern)
                                val desReg = customPattern.desReg
                                if (desReg.toRegex().matches(event.message.content)) {
                                    val bean = SpringUtil.getBean(clazz)
                                    method.invoke(bean, event)
                                }
                            }
                        }
                        logInfo("注册监听器：${clazz.name}.${method.name}")
                    }
                }
            }
        }
    }

    fun spiltPattern(pattern: String): CustomPattern{
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
            if (suffixStr.size > 1){
                throw PatternErrorException("表达式语法错误，缺少}}")
            }

            suffixStr[0].let {// it :  name,pattern
                val split = it.split(",")
                if (split.size != 2){
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
     * @Description: pattern: /.*{{name,pattern}} 对应的CustomPattern:
     * {fieldName:"name", replaceReg:"pattern", desReg:"//.*pattern"}
     */
    data class CustomPattern(
        val fieldMap: MutableMap<String, String> = mutableMapOf(),// key:fieldName, value:pattern
        val regParts: MutableList<String> = ArrayList()// 正则表达式的各个部分
    ){
        val desReg: String // 用于匹配消息内容的正则表达式
            get() {
                return regParts.joinToString(separator = "")
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