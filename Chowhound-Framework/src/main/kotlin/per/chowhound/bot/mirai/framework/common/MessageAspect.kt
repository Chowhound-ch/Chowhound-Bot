package per.chowhound.bot.mirai.framework.common

import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.GroupTempMessageEvent
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.aop.support.AopUtils
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.AnnotationUtils
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.components.permit.service.PermitService
import per.chowhound.bot.mirai.framework.components.state.enums.GroupStateEnum
import per.chowhound.bot.mirai.framework.components.state.service.GroupStateService
import per.chowhound.bot.mirai.framework.config.Filter
import per.chowhound.bot.mirai.framework.config.listener.Listener

/**
 * @Author: Chowhound
 * @Date: 2023/4/17 - 20:43
 * @Description: TODO 对监听器的切面
 */
@Aspect
@Suppress("unused")
@Configuration
class MessageAspect(val bot: Bot,
                    val groupStateService: GroupStateService,
                    val permitService: PermitService) {

    // 消息监听，拦截所有带有@Listener注解的方法
    @Around("@annotation(per.chowhound.bot.mirai.framework.config.listener.Listener) && @annotation(annotation))")
    fun ProceedingJoinPoint.doAroundAdvice(annotation: Listener): Any? {
        // 参数无Event则直接执行
        val event = args.find { it is Event } ?: return proceed()
        val start = System.currentTimeMillis()


        fun proceedSuccess(group: Long? = null): Any? {
            logInfo(
                "执行了监听器{}({})(群: {}), 拦截器耗时: {}",
                signature.name,
                annotation.desc,
                group ?: "好友消息",
                System.currentTimeMillis() - start
            )

            return proceed()
        }

        fun proceedFailed(tip: String? = null, group: String) {
            logInfo("执行监听器{}({})(群: {}) 失败 : {}", signature.name, annotation.desc, group, tip ?: "无")

            runBlocking {
                tip?.let {
                    bot.getGroup(group.toLong())?.sendMessage(it)
                }
            }

        }

        // 获取被代理方法的KFunction对象
        val filter = (this.signature as MethodSignature).method.getAnnotation(Filter::class.java) ?: return proceedSuccess()


        // 判断权限
        fun hasNoPermission(number: Long): Boolean{
            return permitService.getPermit(number).permit!!.isHigherOrEqual(filter.permit)
        }

        // 判断群开机状态
        fun checkGroupState(number: Long, group: Group): Boolean{
            if (filter.isBoot) {
                return true
            }
            val desGroup = groupStateService.getGroupState(group.id)

            return desGroup.state?.state == GroupStateEnum.START.state
        }

        // 判断是否为群消息
        if (event is GroupMessageEvent) {
            return if (hasNoPermission(event.sender.id) && checkGroupState(event.sender.id, event.group)) {
                proceedSuccess(event.group.id)
            } else {
                proceedFailed("权限不足或已经关机", event.group.id.toString())
            }
        }

        if (event is GroupTempMessageEvent) {
            return if (hasNoPermission(event.sender.id) && checkGroupState(event.sender.id, event.group)) {
                proceedSuccess(event.group.id)
            } else {
                proceedFailed("权限不足或已经关机", event.group.id.toString())
            }
        }



        return proceedSuccess()
    }
}