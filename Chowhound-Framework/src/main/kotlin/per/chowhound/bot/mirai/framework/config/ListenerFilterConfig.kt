package per.chowhound.bot.mirai.framework.config

import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum

/**
 * @author : Chowhound
 * @since : 2023/9/5 - 19:52
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Filter(
    val isBoot: Boolean = false,// 监听是否需要开机，为 false 时关机不监听
    val permit: PermitEnum = PermitEnum.MEMBER,// 监听方法的权限
)