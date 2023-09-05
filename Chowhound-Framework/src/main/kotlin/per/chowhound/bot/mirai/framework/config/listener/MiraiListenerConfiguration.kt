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
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.ListeningStatus
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Configuration
import per.chowhound.bot.mirai.framework.config.listener.filter.MatchType
import per.chowhound.bot.mirai.framework.config.listener.utils.EventClassUtil.getIfMessageEvent
import per.chowhound.bot.mirai.framework.config.listener.utils.ListenerInfo
import per.chowhound.bot.mirai.framework.config.listener.utils.PatternUtil.resolve
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


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

    private val listenerInfoList = ArrayList<ListenerInfo>()
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

        listenerInfoList.forEach{ info ->

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
                                    ?: throw RuntimeException("参数${param.name}没有指定名称")

                                val value = matchResult!!.groups[name]?.value
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

                val listener = bot.eventChannel.subscribe(info.eventClass.getIfMessageEvent()!!) {

                    if (info.listenerAnn.matchType.match(it.message.content, info.regPattern!!)) {
                        info.function.callSuspend(*getParams())
                    }

                    return@subscribe ListeningStatus.LISTENING
                }
                listener.start()
                listener.cancel()
            }else{
                bot.eventChannel.subscribeAlways(eventClass = info.eventClass){
                    info.function.callSuspend(info.bean, this)
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



