package per.chowhound.bot.mirai.framework.config.listener.filter



/**
 * 一个匹配器，提供一个 [T] 作为被匹配的目标，[R] 为匹配原则/规则，
 * 并得到一个匹配结果。
 */
fun interface Matcher<T, R> {

    /**
     * 通过匹配规则，对目标进行匹配检测。
     */
    fun match(target: T, rule: R): Boolean
}

/**
 * 字符串之间的匹配器。
 */
 fun interface StringMatcher : Matcher<String, String>


/**
 * 常见的字符串匹配器。
 */
enum class StringMatchers(private val matcher: StringMatcher) :
    StringMatcher by matcher {

    /**
     * 全等匹配
     */
    EQUALS({ t, r -> t == r}),

    /**
     * 忽略大小写的全等匹配
     */
    EQUALS_IGNORE_CASE({ t, r -> t.equals(r, true) }),


    /**
     * 首部匹配
     */
    STARTS_WITH({ t, r -> t.startsWith(r) }),

    /**
     * 尾部匹配.
     *
     */
    ENDS_WITH({ t, r -> t.endsWith(r) }),

    /**
     * 包含匹配.
     *
     */
    CONTAINS({ target, rule -> rule in target }),

    /**
     * 完整正则匹配
     */
    REGEX_MATCHES({ t, r -> r.toRegex().matches(t) }),

    /**
     * 包含正则匹配
     */
    REGEX_CONTAINS({ t, r -> r.toRegex().containsMatchIn(t) }),

}
