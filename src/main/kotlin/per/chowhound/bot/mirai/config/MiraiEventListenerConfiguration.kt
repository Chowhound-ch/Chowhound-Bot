package per.chowhound.bot.mirai.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import per.chowhound.bot.mirai.common.utils.Logger.logInfo


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
                method.getAnnotation(Listener::class.java)?.let {listener ->
                    // 扫描所有的 Listener 注解，注册监听器
                    method.parameterTypes.find { Event::class.java.isAssignableFrom(it) }?.let {
                        bot.eventChannel.subscribeAlways(it as Class<Event>, priority = listener.priority) {
                            method.invoke(SpringUtil.getBean(clazz), this)
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

    val desc: String = "无描述",
)