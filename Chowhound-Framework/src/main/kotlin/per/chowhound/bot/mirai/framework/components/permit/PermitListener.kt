package per.chowhound.bot.mirai.framework.components.permit

import net.mamoe.mirai.event.events.GroupMessageEvent
import org.springframework.stereotype.Controller
import per.chowhound.bot.mirai.framework.common.MessageSender.send
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.components.permit.service.PermitService
import per.chowhound.bot.mirai.framework.config.Listener

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 14:33
 * @Description:
 */
@Controller
class PermitListener(val permitService: PermitService) {

    @Listener("/permit\\s*{{qqNumber}}\\s*{{desPermit}}", permit = PermitEnum.OWNER, isBoot = true, desc = "修改权限")
    suspend fun GroupMessageEvent.updatePermit(qqNumber: String, desPermit: String) {
        val permit = PermitEnum.getPermit(desPermit)
        val newPermit = permitService.setPermit(qqNumber.toLong(), permit)

        if (newPermit.permit == permit) {
            send("权限未改变, 当前权限为 ${newPermit.permit!!.name.lowercase()}")
        }else {
            send("权限改变, 当前权限为 ${newPermit.permit!!.name.lowercase()}")
        }
    }

}