package per.chowhound.bot.mirai.framework.common

import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.event.events.MessageEvent
import net.mamoe.mirai.message.MessageReceipt
import net.mamoe.mirai.message.data.*

/**
 * @Author: Chowhound
 * @Date: 2023/4/24 - 22:20
 * @Description:
 */
@Suppress("unused")
object MessageSender {

//    suspend fun GroupMessageEvent.send(message: String): MessageReceipt<Group> = this.subject.sendMessage(message)
//
//    suspend fun GroupMessageEvent.send(message: Message): MessageReceipt<Group> = this.subject.sendMessage(message)

    suspend fun MessageEvent.send(message: Message): MessageReceipt<Contact> = this.subject.sendMessage(message)

    suspend fun MessageEvent.send(message: String): MessageReceipt<Contact> = this.subject.sendMessage(message)

    suspend fun MessageEvent.reply(message: Message): MessageReceipt<Contact> = this.send(messageChainOf(QuoteReply(this.source)) + message)

    suspend fun MessageEvent.reply(message: String): MessageReceipt<Contact> = this.reply(PlainText(message))

}