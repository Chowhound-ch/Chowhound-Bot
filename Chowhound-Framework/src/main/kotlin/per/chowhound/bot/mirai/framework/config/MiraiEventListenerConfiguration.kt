package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.core.util.StrUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.events.MessageEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo


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

                                method.invoke(SpringUtil.getBean(clazz), this)
                            }
                        }
                        logInfo("注册监听器：${clazz.name}.${method.name}")
                    }
                }
            }
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