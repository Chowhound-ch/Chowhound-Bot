package per.chowhound.bot.mirai.config

import cn.hutool.core.util.ClassUtil
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import per.chowhound.bot.mirai.common.logInfo


@Configuration
class MiraiEventListenerConfiguration {
    @Autowired
    private lateinit var bot: Bot

    init {
        ClassUtil.scanPackage("per.chowhound.bot.mirai").forEach { clazz ->
            clazz.declaredMethods.forEach { method ->
                method.getAnnotation(Listener::class.java)?.let {
                    // 扫描所有的 Listener 注解，注册监听器
                    method.parameterTypes.forEach { parameterType ->
                        // 如果参数是 Event 的子类，就注册监听器
                        if (Event::class.java.isAssignableFrom(parameterType)) {
                            logInfo("注册监听器: ${parameterType.simpleName}")
                        }
                    }
                }
            }
        }
    }

}

annotation class Listener{

}