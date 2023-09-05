package per.chowhound.bot.mirai.framework.components.permit

import net.mamoe.mirai.event.events.GroupMessageEvent
import org.springframework.stereotype.Controller
import per.chowhound.bot.mirai.framework.common.MessageSender.send
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.components.permit.service.PermitService
import per.chowhound.bot.mirai.framework.config.listener.Listener

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 14:33
 * @Description:
 */
@Suppress("unused")
@Controller
class PermitListener(val permitService: PermitService) {


    @Listener("/permit\\s*{{qqNumber,@?}}\\s*{{desPermit}}", permit = PermitEnum.OWNER, isBoot = true, desc = "修改权限(直接输入qq号和权限名)")
    suspend fun GroupMessageEvent.updatePermit(qqNumber: Long, desPermit: String) {
        if (!group.contains(qqNumber)) {
            send("不能修改非群成员的权限")
            return
        }

        when(qqNumber){
            group.botAsMember.id -> { send("不能修改机器人的权限");return }
            group.owner.id -> { send("不能修改owner的权限");return }
            sender.id -> { send("不能修改自己的权限");return }
        }


        val permit = PermitEnum.getPermit(desPermit) ?: run {
            send("权限名错误,可选值为${PermitEnum.values().joinToString(",") { it.name.lowercase() }}")
            return
        }
        val newPermit = permitService.setPermit(qqNumber, permit)

        if (newPermit.permit == permit) {
            send("权限未改变, 当前权限为 ${newPermit.permit!!.name.lowercase()}")
        }else {
            send("权限改变, 当前权限为 ${newPermit.permit!!.name.lowercase()}")
        }
    }

}