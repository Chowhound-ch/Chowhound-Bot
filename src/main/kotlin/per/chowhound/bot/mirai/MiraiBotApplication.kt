package per.chowhound.bot.mirai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

//@ComponentScan("per.chowhound.bot")
@SpringBootApplication
class MiraiBotApplication

fun main(args: Array<String>) {
//    FixProtocolVersion.update()
    runApplication<MiraiBotApplication>(*args)
}
