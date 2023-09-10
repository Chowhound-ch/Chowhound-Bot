package per.chowhound.bot.mirai.framework.components

import net.mamoe.mirai.event.events.MessageEvent
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import per.chowhound.bot.mirai.framework.common.MessageSender.reply
import per.chowhound.bot.mirai.framework.config.listener.Listener
import per.chowhound.bot.mirai.framework.config.listener.MiraiListenerManager
import kotlin.reflect.full.declaredMemberExtensionFunctions

/**
 * @author : Chowhound
 * @since : 2023/9/5 - 18:57
 */

@Component
class TestListener{
    @Autowired
    lateinit var manager: MiraiListenerManager

    private var context: ApplicationContext? = null
//    @Autowired
//    fun setMiraiListenerManager(miraiListenerManager: MiraiListenerManager) {
//        this.manager = miraiListenerManager
//    }

    @Listener
    suspend fun MessageEvent.test() {
        this.reply("daw")
        manager.stopListen(TestListener::class.declaredMemberExtensionFunctions.first())
    }

}