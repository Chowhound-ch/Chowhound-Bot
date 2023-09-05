package per.chowhound.bot.mirai.framework.config.listener;

import cn.hutool.core.util.ClassUtil
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.MessageEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.config.listener.utils.ListenerPatternInfo
import per.chowhound.bot.mirai.framework.config.listener.utils.PatternUtil.resolve
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * @author Chowhound
 * @date   2023/6/8 - 16:45
 * @description key: 拥有监听方法的bean, value: list集合，其中的元素pair.first为监听方法，pair.second为监听的事件类型
 */

internal val log = LoggerFactory.getLogger(MiraiListenerConfiguration::class.java)

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

/**
 * @author : Chowhound
 * @since : 2023/9/1 - 21:59
 */
@Configuration
class MiraiListenerConfiguration(
    val bot: Bot
) {
    @Autowired
    private lateinit var context: ApplicationContext

    private val listenerInfoList = ArrayList<ListenerPatternInfo>()
    @PostConstruct
    private fun init(){
        //扫描
        val scanPath = getScanPath()
        log.info("开始扫描如下package中事件监听器: {}", scanPath)

        ClassUtil.scanPackage(scanPath).forEach { clazz ->
            if (clazz == MiraiListenerConfiguration::class.java) return@forEach

            val bean = try {
                context.getBean(clazz)
            } catch (e: NoSuchBeanDefinitionException) {
                return@forEach
            }

            val listenerInfos = clazz.resolve(bean)
            listenerInfoList.addAll(listenerInfos)
        }
        // 注册
        bot.eventChannel.subscribeMessages {
            listenerInfoList.forEach { info ->
                if (info.isMessageEvent!!){
                    // 正则匹配时获取方法参数
                    fun MessageEvent.getParams(matchResult: MatchResult): Array<Any?> {
                        val params = mutableListOf<Any?>()
                        info.function.parameters.forEach { param ->
                            when (param.kind){
                                KParameter.Kind.INSTANCE -> params.add(info.bean)
                                KParameter.Kind.EXTENSION_RECEIVER -> params.add(this)
                                else ->{
                                    val name = param.annotations.find { it == FilterValue::class }
                                        ?.run { (this as? FilterValue)?.value }
                                        ?: param.name
                                        ?: throw RuntimeException("参数${param.name}没有指定名称")

                                    val value = matchResult.groups[name]?.value
                                    when(param.type.classifier){
                                        String::class -> params.add(value)
                                        Int::class -> params.add(value?.toInt())
                                        Long::class -> params.add(value?.toLong())
                                        Double::class -> params.add(value?.toDouble())
                                        Float::class -> params.add(value?.toFloat())
                                        Boolean::class -> params.add(value?.toBoolean())
                                        else -> throw RuntimeException("参数${param.name}类型不支持")
                                    }
                                }
                            }
                        }
                        return params.toTypedArray()
                    }
                    // TODO 区别监听的事件的类型
                    when (info.listener.matchType) {
                        MatchType.TEXT_EQUALS -> case(info.listener.value) {
                            info.function.callSuspend(info.bean, this)
                        }
                        MatchType.TEXT_EQUALS_IGNORE_CASE -> case(info.listener.value, ignoreCase = true) {
                            info.function.callSuspend(info.bean, this)
                        }
                        MatchType.TEXT_STARTS_WITH -> startsWith(info.listener.value) {
                            info.function.callSuspend(info.bean, this)
                        }
                        MatchType.TEXT_ENDS_WITH -> endsWith(info.listener.value) {
                            info.function.callSuspend(info.bean, this)
                        }
                        MatchType.TEXT_CONTAINS -> contains(info.listener.value) {
                            info.function.callSuspend(info.bean, this)
                        }
                        MatchType.REGEX_MATCHES -> matching(info.regPattern!!.toRegex()) {
                            info.function.callSuspend(*getParams(it))
                        }
                        MatchType.REGEX_CONTAINS -> finding(info.regPattern!!.toRegex()) {
                            info.function.callSuspend(*getParams(it))
                        }
                    }
                } else{
                    bot.eventChannel.subscribeAlways(eventClass = info.eventClass){
                        info.function.callSuspend(info.bean, this)
                    }
                }
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
}

/**
 *
 * @author ForteScarlet
 */
enum class MatchType{
    /**
     * 全等匹配
     */
    TEXT_EQUALS,

    /**
     * 忽略大小写的全等匹配
     */
    TEXT_EQUALS_IGNORE_CASE,


    /**
     * 开头匹配
     */
    TEXT_STARTS_WITH,

    /**
     * 尾部匹配.
     */
    TEXT_ENDS_WITH,

    /**
     * 包含匹配.
     */
    TEXT_CONTAINS,

    /**
     * 正则完全匹配. `regex.matches(...)`
     */
    REGEX_MATCHES,


    /**
     * 正则包含匹配. `regex.find(...)`
     */
    REGEX_CONTAINS,

    ;



}

@Suppress("unused")
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Listener(
    @get:AliasFor("pattern")
    val value: String = "",

    val matchType: MatchType = MatchType.REGEX_MATCHES,

    val priority: EventPriority = EventPriority.NORMAL,
    @get:AliasFor("value")
    val pattern: String = "", // 正则表达式，用于匹配消息内容

    val desc: String = "无描述",

    val isBoot: Boolean = false,// 监听是否需要开机，为 false 时关机不监听
    val permit: PermitEnum = PermitEnum.MEMBER,// 监听方法的权限
)
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class FilterValue(val value: String)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ListenerScan(val value: String)