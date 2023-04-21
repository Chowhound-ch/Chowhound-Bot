package per.chowhound.bot.mirai.framework.config.exception

/**
 * @Author: Chowhound
 * @Date: 2023/4/20 - 15:23
 * @Description:
 */
open class BotException(message: String) : RuntimeException(message)

class PatternErrorException(message: String) : BotException(message)

class ListenerWithNoEventException(message: String) : BotException(message)


class ListenerNoAnnotationException(message: String) : BotException(message)