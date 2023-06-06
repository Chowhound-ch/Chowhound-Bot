package per.chowhound.bot.mirai.framework.common.utils

import net.mamoe.mirai.event.Event
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageEvent
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

/**
 * @author Chowhound
 * @date   2023/6/6 - 14:33
 */
object EventClassUtil {
    fun KClass<*>.isGroupMessageEvent(): Boolean {
        return this == GroupMessageEvent::class
    }

    fun KClass<*>.isFriendMessageEvent(): Boolean {
        return this == FriendMessageEvent::class
    }

    fun KClass<*>.isMessageEvent(): Boolean {
        return this.isSubclassOf(MessageEvent::class)
    }

    fun KClass<*>.isEvent(): Boolean {
        return this.isSubclassOf(Event::class)
    }

    @Suppress("UNCHECKED_CAST")
    fun KClass<*>.getIfEvent(): KClass<out Event>? {
        return if (Event::class.java.isAssignableFrom(this.java)) {
            this as KClass<out Event>
        } else { null }
    }

    @Suppress("UNCHECKED_CAST")
    fun KClass<*>.getIfMessageEvent(): KClass<out MessageEvent>? {
        return if (this.isMessageEvent()) {
            this as KClass<out MessageEvent>
        }else{ null }
    }

}