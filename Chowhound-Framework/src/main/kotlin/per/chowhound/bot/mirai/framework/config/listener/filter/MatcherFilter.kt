package per.chowhound.bot.mirai.framework.config.listener.filter

/**
 * @author : Chowhound
 * @since : 2023/9/5 - 14:02
 */


/**
 *
 * @author ForteScarlet
 */
enum class MatchType(private val matcher: StringMatcher, val isReg: Boolean): StringMatcher by matcher{
    /**
     * 全等匹配
     */
    TEXT_EQUALS(StringMatchers.EQUALS, false),

    /**
     * 忽略大小写的全等匹配
     */
    TEXT_EQUALS_IGNORE_CASE(StringMatchers.EQUALS_IGNORE_CASE, false),


    /**
     * 开头匹配
     */
    TEXT_STARTS_WITH(StringMatchers.STARTS_WITH, false),

    /**
     * 尾部匹配.
     */
    TEXT_ENDS_WITH(StringMatchers.ENDS_WITH, false),

    /**
     * 包含匹配.
     */
    TEXT_CONTAINS(StringMatchers.CONTAINS, false),

    /**
     * 正则完全匹配. `regex.matches(...)`
     */
    REGEX_MATCHES(StringMatchers.REGEX_MATCHES, true),


    /**
     * 正则包含匹配. `regex.find(...)`
     */
    REGEX_CONTAINS(StringMatchers.REGEX_CONTAINS, true),

    ;
}