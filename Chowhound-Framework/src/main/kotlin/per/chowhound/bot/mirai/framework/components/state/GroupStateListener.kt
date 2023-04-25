package per.chowhound.bot.mirai.framework.components.state

import jakarta.annotation.Resource
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import org.springframework.stereotype.Controller
import per.chowhound.bot.mirai.framework.common.MessageSender.send
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.components.state.enums.GroupStateEnum
import per.chowhound.bot.mirai.framework.components.state.service.GroupStateService
import per.chowhound.bot.mirai.framework.config.Listener

/**
 * @Author: Chowhound
 * @Date: 2023/4/19 - 16:59
 * @Description:
 */

@Suppress("unused")
@Controller
class GroupStateListener {
    @Resource
    lateinit var groupStateService: GroupStateService

    @Listener("^/status\\s*{{desState}}", permitEnum = PermitEnum.ADMIN, isBoot = true, desc = "修改群状态")
    suspend fun GroupMessageEvent.updateGroupState(desState: String) {

        val groupState = groupStateService.getGroupState(group.id)
        val newState = GroupStateEnum.getState(desState).let {
            groupStateService.setGroupState(groupState.copy(state = it))
        }

        if (newState.state!!.state == groupState.state!!.state) {
            send("状态未改变, 当前状态为 ${groupState.state.name.lowercase()}")
        }else {
            send("状态改变, 当前状态为 ${newState.state.name.lowercase()}")
        }
    }
}