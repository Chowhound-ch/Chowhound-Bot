package per.chowhound.bot.mirai.framework.components.state.entity

import per.chowhound.bot.mirai.framework.components.state.enums.GroupStateEnum

/**
 * @Author: Chowhound
 * @Date: 2023/4/19 - 17:52
 * @Description:
 */
data class GroupState(
    val id: String? = null,
    val group: Long? = null,
    val state: GroupStateEnum? = null
)
