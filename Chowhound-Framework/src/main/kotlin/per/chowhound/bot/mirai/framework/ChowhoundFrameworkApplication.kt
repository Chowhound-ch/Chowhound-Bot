package per.chowhound.bot.mirai.framework

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChowhoundFrameworkApplication

fun main(args: Array<String>) {
    runApplication<ChowhoundFrameworkApplication>(*args)
}
