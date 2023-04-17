package per.chowhound.bot.mirai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MiraiBotApplication

fun main(args: Array<String>) {
    runApplication<MiraiBotApplication>(*args)
}
