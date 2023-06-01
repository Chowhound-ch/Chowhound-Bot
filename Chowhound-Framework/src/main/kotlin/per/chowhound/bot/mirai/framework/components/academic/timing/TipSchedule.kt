package per.chowhound.bot.mirai.framework.components.academic.timing

import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.PlainText
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.components.academic.service.ScheduleService
import per.chowhound.bot.mirai.framework.components.academic.util.AcademicUtil
import per.chowhound.bot.mirai.framework.components.academic.entity.Schedule
import java.sql.Date
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * @author zsck
 * @date   2022/11/15 - 18:23
 */
@EnableScheduling
@Component
class TipSchedule  (
    val scheduleService: ScheduleService,
    val academicUtil: AcademicUtil,
    val bot: Bot
){

    @Value("\${chowhound.owner}")
    lateinit var botHost: String

    lateinit var scheduledExecutorService: ScheduledExecutorService

    @PostConstruct
    fun init(){
        scheduledExecutorService = Executors.newScheduledThreadPool(5)
    }

    @Scheduled(cron = "0 40 7 * * 1-5")
    @Async
    fun morning() {
        val firstDate = scheduleService.getFirstDate()
        val date = DateUtil.date().toSqlDate()

        runBlocking {
            val messageList = buildList{
                val gap = DateUtil.between(firstDate, date, DateUnit.DAY, true)

                if (date.before(firstDate)) {
                    this.add(PlainText("当前正处于假期，距离开学还有${gap}天"))
                } else {

                    val scheduleList: List<Schedule> = scheduleService.getLessonsByDate(date)
                    if (scheduleList.isEmpty()) {
                        return@runBlocking
                    }
                    if (scheduleList[0].startTime != 800) {
                        if (LocalDateTime.now().hour <= 8) {
                            scheduledExecutorService.schedule({
                                logInfo("不是早八,延迟提醒")
                                morning()
                            }, 2, TimeUnit.HOURS)
                            return@runBlocking
                        }
                    }
                    this.add(PlainText("今日为:${DateUtil.format(date, "MM-dd")} , 第:${(gap / 7 + 1)}周"))
                    academicUtil.getCourseDetailMsg(scheduleList).forEach {
                        this.add(it)
                    }
                }
            }

            // 将课表发给botHost
            bot.getFriend(botHost.toLong())?.apply {
                messageList.forEach { this.sendMessage(it) }
            }
        }

    }


    @Scheduled(cron = "0 0 22 * * 1,2,3,4,7")
    @Async
    fun even() {
        val firstDate: Date = scheduleService.getFirstDate()
        val date = DateUtil.tomorrow().toJdkDate()
        if (!date.before(firstDate)) { //开学之后
            val gap = DateUtil.between(firstDate, date, DateUnit.DAY, true)

            val scheduleList = scheduleService.getLessonsByDate(DateUtil.date(date).toSqlDate())
            if (scheduleList.isEmpty()) {
                return // 第二天没课则不提醒
            }

            runBlocking {
                val messageList = buildList {
                    this.add(PlainText("明日为:${DateUtil.format(date, "MM-dd")}, 第:${(gap / 7 + 1)}周"))
                    academicUtil.getCourseDetailMsg(scheduleList).forEach {
                        this.add(it)
                    }
                }
                bot.getFriend(botHost.toLong())?.apply {
                    messageList.forEach { this.sendMessage( it ) }
                }
            }

        }
    }
}