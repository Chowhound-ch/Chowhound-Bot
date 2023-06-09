package per.chowhound.bot.mirai.framework.components.state.service

import per.chowhound.bot.mirai.framework.components.state.entity.GroupState
import per.chowhound.bot.mirai.framework.components.state.enums.GroupStateEnum

/**
 * @Author: Chowhound
 * @Date: 2023/4/19 - 19:14
 * @Description:
 */
interface GroupStateService{
    fun getGroupState(group: Long): GroupState

    fun setGroupState(groupState: GroupState): GroupState

    fun getGroupStateByState(state: GroupStateEnum): List<GroupState>
    fun list(): List<GroupState>
}