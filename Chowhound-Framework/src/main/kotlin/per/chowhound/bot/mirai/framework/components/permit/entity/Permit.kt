package per.chowhound.bot.mirai.framework.components.permit.entity

import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 13:42
 * @Description:
 */
data class Permit(
    val id: String? = null,
    val qqNumber: Long? = null,
    var permit: PermitEnum? = null
)