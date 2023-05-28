package per.chowhound.bot.mirai.framework.config

import cn.hutool.core.util.ClassUtil
import cn.hutool.extra.spring.SpringUtil
import jakarta.annotation.PostConstruct
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.data.content
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AliasFor
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum

//@Configuration
//class MiraiEventListenerConfigurationV2 {
//
//
//    @PostConstruct
//    fun init() {
//        ClassUtil.scanPackage("per.chowhound.bot.mirai").forEach { clazz ->
//            if (clazz == MiraiEventListenerConfiguration::class.java) return@forEach
//            val bean = try {
//                SpringUtil.getBean(clazz) // 监听方法所在的类的实例
//            } catch (e: NoSuchBeanDefinitionException){
//                return@forEach
//            }
//            // 遍历类中的方法，找到带有@Listener注解的方法
//            for (method in clazz.declaredMethods) {
//                val listenerFun = try {
//                    MiraiEventListenerConfiguration.ListenerFunction.create(bean, method)
//                } catch (e: Exception) { continue }
//
//                GlobalEventChannel.filter { event ->
////                listenerFun.eventClass.isAssignableFrom(event::class.java)
//                    if (event is MessageEvent && listenerFun.customPattern != null){
//                        return@filter listenerFun.customPattern!!.getRegWithGroup().toRegex().find(event.message.content) != null
//                    }else if (event is MessageEvent){
//                        return@filter listenerFun.listener.pattern.toRegex().find(event.message.content) != null
//                    }else{
//                        return@filter listenerFun.eventClass.isAssignableFrom(event::class.java)
//                    }
//                }.subscribeAlways(listenerFun.eventClass, priority = listenerFun.listener.priority, handler = listenerFun.getCallFunction())
//
////                bot.eventChannel.subscribeAlways(listenerFun.eventClass, priority = listenerFun.listener.priority, handler = listenerFun.getCallFunction())
//                logInfo("注册监听器：{}.{}({})",listenerFun.bean.javaClass, listenerFun.method.name, listenerFun.listener.desc)
//            }
//        }
//    }
//}

//@Suppress("unused")
//@Target(AnnotationTarget.FUNCTION)
//@Retention(AnnotationRetention.RUNTIME)
//annotation class Listener(
//    @get:AliasFor("pattern")
//    val value: String = "",
//
//    val priority: EventPriority = EventPriority.NORMAL,
//    @get:AliasFor("value")
//    val pattern: String = "", // 正则表达式，用于匹配消息内容
//
//    val desc: String = "无描述",
//
//    val isBoot: Boolean = false,// 监听是否需要开机，为 false 时关机不监听
//    val permit: PermitEnum = PermitEnum.MEMBER,// 监听方法的权限
//)
//
//@Target(AnnotationTarget.VALUE_PARAMETER)
//@Retention(AnnotationRetention.RUNTIME)
//annotation class FilterValue(val value: String)