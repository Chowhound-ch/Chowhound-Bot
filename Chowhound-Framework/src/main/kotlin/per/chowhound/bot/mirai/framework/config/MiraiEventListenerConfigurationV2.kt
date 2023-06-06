package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.*
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotationUtils
import per.chowhound.bot.mirai.framework.common.utils.EventClassUtil.getIfEvent
import per.chowhound.bot.mirai.framework.common.utils.EventClassUtil.isEvent
import per.chowhound.bot.mirai.framework.common.utils.EventClassUtil.isMessageEvent
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logError
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredMemberExtensionFunctions
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.kotlinFunction

/**
 * @author Chowhound
 * @date   2023/6/6 - 10:50
 */
@Configuration
class MiraiEventListenerConfigurationV2(
    val bot: Bot
) {

    val listenerFunctions = ListenerFunctions()


    @PostConstruct
    fun init() {
        ClassUtil.scanPackage("per.chowhound.bot.mirai").forEach { clazz ->

            logInfo("扫描到类：{}", clazz)
            if (clazz == MiraiEventListenerConfigurationV2::class.java || clazz == MiraiEventListenerConfiguration::class.java.superclass){
                return@forEach
            }
            // TODO: 扫描报错
            clazz.declaredMethods.map { it.kotlinFunction!! }.forEach { function ->
                    function.annotations.forEach { annotation ->
                        if (annotation is Listener) {
                            // 获取扩展函数的被拓展的类型
                            val classifier = function.extensionReceiverParameter!!.type.classifier!! as KClass<*>
                            val event = classifier.getIfEvent() ?: throw RuntimeException("不是事件类型")

                            if (classifier.isEvent()){
                                listenerFunctions[SpringUtil.getBean(clazz)]
                                    ?.add(function to event)
                                    ?: run {
                                        listenerFunctions[SpringUtil.getBean(clazz)] =
                                            mutableListOf(function to event)
                                    }

                            }


//
//                        bot.eventChannel.subscribeMessages {  }

//                        if (GroupMessageEvent::class == classifier) {
//                            bot.eventChannel.subscribe(eventClass = classifier as KClass<GroupMessageEvent>) {
//
//                            }
//                        }

                            /*         when(classifier){
                                         GroupMessageEvent::class -> {
                                             bot.eventChannel.subscribeGroupMessages {
                                                 function.call(this)
                                             }
                                         }
                                         FriendMessageEvent::class -> {
                                             bot.eventChannel.subscribeFriendMessages{
                                                 function.call(this)
                                             }
                                         }

                                         MessageEvent::class -> {
                                             bot.eventChannel.subscribeMessages {
                                                 startsWith("/点歌") {

                                                 }

                                                 matching("/点歌".toRegex()) {
                                                     it.groups

                                                 }
                                                 sentBy(bot.id) {

                                                 }
                                                 atBot {
                                                     reply("你好")
                                                 }

                                             }
                                         }

                                     }
             */
                            // 当classifier为KClass<GroupMessageEvent>时

                        }
                    }
                }


        }


        // 开始注册监听器
        bot.eventChannel.subscribeMessages {
            listenerFunctions.forEach{ entry ->
                entry.value.forEach { pair ->
                    if (pair.second.isMessageEvent()){
                        val listener = AnnotationUtils.getAnnotation(pair.first.javaMethod!!, Listener::class.java)
                            ?: throw RuntimeException("不是监听器")

                        when (listener.matchType) {
                            MatchType.TEXT_EQUALS -> case(listener.value) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.TEXT_EQUALS_IGNORE_CASE -> case(listener.value, ignoreCase = true) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.TEXT_STARTS_WITH -> startsWith(listener.value) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.TEXT_ENDS_WITH -> endsWith(listener.value) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.TEXT_CONTAINS -> contains(listener.value) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.REGEX_MATCHES -> matching(listener.value.toRegex()) {
                                pair.first.call(entry.key, this)
                            }
                            MatchType.REGEX_CONTAINS -> finding(listener.value.toRegex()) {
                                pair.first.call(entry.key, this)
                            }
                        }

                    }else{// 非MessageEvent
                        bot.eventChannel.subscribeAlways(eventClass = pair.second){
                            pair.first.call(entry.key, this)
                        }
                    }

                }
            }
        }

    }


}

class ListenerFunctions: MutableMap<Any, MutableList<Pair<KFunction<*>, KClass<out Event>>>> by mutableMapOf() {



}
