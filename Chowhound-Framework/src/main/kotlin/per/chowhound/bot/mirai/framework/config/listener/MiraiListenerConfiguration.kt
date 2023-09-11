package per.chowhound.bot.mirai.framework.config.listener

import cn.hutool.core.util.ClassUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import org.springframework.core.annotation.AnnotationUtils
import per.chowhound.bot.mirai.framework.config.listener.EventClassUtil.getIfEvent
import per.chowhound.bot.mirai.framework.config.listener.EventClassUtil.getIfMessageEvent
import per.chowhound.bot.mirai.framework.config.listener.EventClassUtil.isMessageEvent
import per.chowhound.bot.mirai.framework.config.listener.PatternUtil.resolve
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaMethod
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


internal val log = LoggerFactory.getLogger(MiraiListenerManager::class.java)

/**
 * @author Chowhound
 * @date   2023/6/11 - 19:26
 * @description  持续会话
 * 返回值为空则表示超时
 */
suspend inline fun  <reified E : Event> E.waitMessage(
    time: Duration = 15.seconds,
    noinline filter: (E) -> Boolean = { true }
): E? {
    val deferred = CoroutineScope(Dispatchers.IO).async {
        GlobalEventChannel.asFlow()
            .filterIsInstance<E>()
            .filter(filter)
            .first()
    }

    return withTimeoutOrNull(time) { deferred.await() }
}
suspend inline fun  <reified E : Event> waitMessage(
    time: Duration = 15.seconds,
    noinline filter: (E) -> Boolean = { true }
): E? {
    val deferred = CoroutineScope(Dispatchers.IO).async {
        GlobalEventChannel.asFlow()
            .filterIsInstance<E>()
            .filter(filter)
            .first()
    }

    return withTimeoutOrNull(time) { deferred.await() }
}

// region 注册监听器

/**
 * @author : Chowhound
 * @since : 2023/09/05 - 23:25
 */
@Configuration
class MiraiListenerManager(
    val bot: Bot): ApplicationRunner{

    @Autowired
    private lateinit var context: ApplicationContext

    val listenerMap: MutableMap<KClass<*>, ListenerInfoGroup> = mutableMapOf()


//    @PostConstruct
    private fun init(){
        //扫描
        val scanPath = getScanPath()
        log.info("开始扫描如下package中事件监听器: {}", scanPath)

        ClassUtil.scanPackage(scanPath).forEach { clazz ->
            if (clazz == MiraiListenerManager::class.java) return@forEach

            val bean = try {
                context.getBean(clazz)
            } catch (e: Exception) {
                if (e !is NoSuchBeanDefinitionException){
                    log.error(e.message)
                    e.printStackTrace()
                }
                return@forEach
            }

            val listenerInfos = clazz.resolve(bean)

            if (listenerInfos.isNotEmpty()){
                listenerMap[clazz.kotlin] = ListenerInfoGroup(listenerInfos.associateBy { it.function })
            }
        }

    }
    fun getScanPath(): String{
        val collection = context.getBeansWithAnnotation(SpringBootApplication::class.java).values
        if (collection.isEmpty()){
            throw RuntimeException("没有找到SpringBoot启动类")
        } else if (collection.size > 1){
            throw RuntimeException("找到多个SpringBoot启动类")
        }

        val clazz = collection.first().javaClass

        return try{ clazz.getAnnotation(ListenerScan::class.java).value }
        catch (e : NullPointerException){null}
            ?: clazz.`package`.name
    }

    fun register(info: ListenerInfo): Boolean{
        if (info.isMessageEvent!!) {
            // 正则匹配时获取方法参数
            fun MessageEvent.getParams(): Array<Any?> {
                val params = mutableListOf<Any?>()
                val matchResult = when(info.listenerAnn.matchType){
                    MatchType.REGEX_MATCHES -> info.regPattern!!.toRegex().matchEntire(this.message.content)
                    MatchType.REGEX_CONTAINS -> info.regPattern!!.toRegex().find(this.message.content)
                    else -> null
                }


                info.function.parameters.forEach { param ->
                    when (param.kind){
                        KParameter.Kind.INSTANCE -> params.add(info.bean)
                        KParameter.Kind.EXTENSION_RECEIVER -> params.add(this)
                        else ->{
                            // 只有Reg匹配才可进入
                            val name = param.annotations.find { it == FilterValue::class }
                                ?.run { (this as? FilterValue)?.value }
                                ?: param.name
                                ?: throw RuntimeException("参数没有指定名称")

                            val value = matchResult!!.groups[name]?.value
                            params.add(value?.castToType(param.type.classifier))
                        }
                    }
                }
                return params.toTypedArray()
            }

            val listener = bot.eventChannel.subscribe(info.eventClass.getIfMessageEvent()!!) {

                if (info.listenerAnn.matchType.match(it.message.content, info.regPattern!!)) {
                    val res = info.function.callSuspend(*getParams())
                    if (res is ListeningStatus){
                        return@subscribe res
                    }
                }

                return@subscribe ListeningStatus.LISTENING
            }
            info.listener = listener
        }else{
            info.listener = bot.eventChannel.subscribeAlways(eventClass = info.eventClass){
                info.function.callSuspend(info.bean, this)
            }
        }
        return true
    }

    fun String.castToType(type: KClassifier?): Any?{
        return when(type){
            String::class -> this
            Int::class -> this.toInt()
            Long::class -> this.toLong()
            Double::class -> this.toDouble()
            Float::class -> this.toFloat()
            Boolean::class -> this.toBoolean()
            else -> null
        }
    }
    // TODO 尝试KFunction直接获取KClass
    fun stopListen(function: KFunction<*>): Boolean{
        val kClass = function.javaMethod?.declaringClass?.kotlin ?: return false
        val group = listenerMap[kClass]?: return false
        val info = group[function] ?: run {
            log.debug("未找到监听器: {}", function.name)
            return false
        }

        info.listener?.let {
            log.info("注销监听器: {}({})", function.name, info.listenerAnn.desc)
            it.complete()
            group.remove(function)
            return true
        }

        return false
    }

    class ListenerInfoGroup(private val listenerMap: Map<KFunction<*>, ListenerInfo>)
        : MutableMap<KFunction<*>, ListenerInfo> by HashMap(listenerMap)

    override fun run(args: ApplicationArguments?) {
        init()

        this.listenerMap.forEach { (_, group) ->
            group.forEach { (_, info) ->
                this.register(info)
            }
        }

        log.info("监听器注册完成")

    }
}

// endregion

// region 监听器解析

/**
 * @author : Chowhound
 * @since : 2023/9/1 - 22:11
 */
object PatternUtil {
    private const val SEPARATOR_START = "{{"
    private const val SEPARATOR_END = "}}"

//    private const val DEFAULT_GROUP_NAME = "DEFAULT"

    // `name,\\s+`
    fun Class<*>.resolve(bean: Any): List<ListenerInfo> {
        val list = ArrayList<ListenerInfo>()
        this.kotlin.declaredFunctions.forEach { function->
            val listener = AnnotationUtils.getAnnotation(function.javaMethod!!,
                Listener::class.java) ?: return@forEach
            // 解析事件监听
            // 获取扩展函数的被拓展的类型
            // (function.parameters[1].type.classifier as KClass<*>).isSubclassOf(Event::class)
            val eventClass = function.parameters.stream()
                .map { it.type.classifier as? KClass<*> }
                .filter { it != null && it.isSubclassOf(Event::class) }
                .limit(1).toList()[0]
                ?: return@forEach
            val event = eventClass.getIfEvent() ?: throw RuntimeException("不是事件类型")
            val info = ListenerInfo(function = function, eventClass = event,
                bean = bean, listenerAnn = listener)

            if (event.isMessageEvent()){
                val patternMap = HashMap<String, String>()
                val regPattern = recur(listener.pattern, patternMap)
                info.patternMap = patternMap
                info.raw = listener.pattern
                info.regPattern = regPattern
            } else{
                info.isMessageEvent = false
            }

            list.add(info)
        }
        return list
    }

    /**
     * <p> 以prefix{{syntax}}suffix为例，其中syntax可为name,pattern或者name
     *
     * @param str 后半部分的需要替换掉[SEPARATOR_START]包裹的内容的字符串
     * @param groupMap 被替换掉的[SEPARATOR_START]包裹的内容
     *
     * @return 后半部分的替换掉[SEPARATOR_START]包裹的内容之后的字符串
     *
     * @author : Chowhound
     * @since : 2023/09/02 - 14:43
     */
    private fun recur(str: String, groupMap: MutableMap<String, String>): String{
        val startIndex = str.lastIndexOf(SEPARATOR_START)
        if (startIndex == -1){ // 没有找到 SEPARATOR_START
            return str
        }
        // syntax}}suffix
        val afterStartSign = str.substring(startIndex + SEPARATOR_START.length)
        // 防止SEPARATOR_END为}}时出现afterStartSign为name,abc{1,2}}}aa}}的情况(正常应匹配aa前的}})
        val charQueue = ConcurrentLinkedQueue(SEPARATOR_END.toCharArray().asList())
        var endIndex = afterStartSign.indexOf(SEPARATOR_END) + SEPARATOR_END.length
        while (endIndex < afterStartSign.length){
            charQueue.remove()
            charQueue.add(afterStartSign[endIndex])
            if (charQueue.joinToString("") != SEPARATOR_END){ break }
            endIndex++
        }
        // ["name", "pattern"]或者pattern未指定默认为["name", ".*?"]
        val patternPair = afterStartSign.substring(0, endIndex - SEPARATOR_END.length)
            .split(',', limit = 2)

        when(patternPair.size){
            1 -> groupMap[patternPair[0]] = ".*?"
            2 -> groupMap[patternPair[0]] = patternPair[1].replaceGroupNoCapture()
            else -> throw RuntimeException("{{name,pattern}}语法错误")
        }

        return recur(str.substring(0, startIndex), groupMap) +
                "(?<${patternPair[0]}>${groupMap[patternPair[0]]})" +
                afterStartSign.substring(endIndex)
    }
    // TODO 成对的括号才替换
    private fun String.replaceGroupNoCapture() = this.replace("(", "(?:")



}
data class ListenerInfo(
    var raw: String? = null,
    var regPattern: String? = null,
//        var regPatternWithGroup: String? = null,
    var patternMap: Map<String, String>? = null,
    var function: KFunction<*>,
    var eventClass: KClass<out Event>,
    var listenerAnn: Listener,
    var listener: net.mamoe.mirai.event.Listener<*>? = null,
    var isMessageEvent: Boolean?  = true,
    var bean: Any
)

/**
 * @author Chowhound
 * @date   2023/6/6 - 14:33
 */
@Suppress("unused")
object EventClassUtil {

    fun KClass<*>.isGroupMessageEvent(): Boolean = this == GroupMessageEvent::class

    fun KClass<*>.isFriendMessageEvent(): Boolean = this == FriendMessageEvent::class

    fun KClass<*>.isMessageEvent(): Boolean = this.isSubclassOf(MessageEvent::class)

    private fun KClass<*>.isEvent(): Boolean = this.isSubclassOf(Event::class)

    @Suppress("UNCHECKED_CAST")
    fun KClass<*>.getIfEvent(): KClass<out Event>? {
        return if (this.isEvent()) { this as KClass<out Event> } else { null }
    }

    @Suppress("UNCHECKED_CAST")
    fun KClass<*>.getIfMessageEvent(): KClass<out MessageEvent>? {
        return if (this.isMessageEvent()) { this as KClass<out MessageEvent> }else{ null }
    }
}


// endregion

// region 过滤器定义

/**
 *
 * @author ForteScarlet
 */
@Suppress("unused")
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

// endregion

// region 注解定义
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
// endregion



