package per.chowhound.bot.mirai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.EnableAspectJAutoProxy
import per.chowhound.bot.mirai.framework.config.ListenerScan

@ListenerScan("per.chowhound")
@EnableAspectJAutoProxy
@SpringBootApplication
class ChowhoundBotApplication

fun main(args: Array<String>) {
    runApplication<ChowhoundBotApplication>(*args)
}
