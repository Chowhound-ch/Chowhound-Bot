package per.chowhound.bot.mirai.framework.config.listener

import net.mamoe.mirai.event.EventPriority
import org.springframework.core.annotation.AliasFor
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.config.listener.filter.MatchType

/**
 * @author : Chowhound
 * @since : 2023/9/5 - 13:56
 */
@Suppress("unused")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Listener(
    @get:AliasFor("pattern")
    val value: String = ".*",

    val matchType: MatchType = MatchType.REGEX_MATCHES,

    val priority: EventPriority = EventPriority.NORMAL,
    @get:AliasFor("value")
    val pattern: String = ".*", // 正则表达式，用于匹配消息内容

    val desc: String = "无描述",
)
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FilterValue(val value: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ListenerScan(val value: String)