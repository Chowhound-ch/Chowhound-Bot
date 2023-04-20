package per.chowhound.bot.mirai.framework.components.state

import jakarta.annotation.Resource
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.springframework.stereotype.Controller
import per.chowhound.bot.mirai.framework.components.state.service.GroupStateService
import per.chowhound.bot.mirai.framework.config.Listener

/**
 * @Author: Chowhound
 * @Date: 2023/4/19 - 16:59
 * @Description:
 */

@Controller
class GroupStateListener {
    @Resource
    lateinit var groupStateService: GroupStateService

    @Listener(pattern = "^/update")
    fun GroupMessageEvent.updateGroupState() {
        runBlocking { sender.group.sendMessage("请输入新的群状态") }
    }
}