package per.chowhound.bot.mirai.framework.components.academic.util

import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.MessageChainBuilder
import org.springframework.stereotype.Component
import per.chowhound.bot.mirai.framework.components.academic.entity.ClassMap
import per.chowhound.bot.mirai.framework.components.academic.entity.Schedule
import per.chowhound.bot.mirai.framework.components.academic.service.ClassMapService
import java.sql.Date
import java.time.temporal.ChronoField
import java.util.stream.Collectors

/**
 * @author zsck
 * @date   2022/11/10 - 9:11
 */
@Component
class AcademicUtil(
    val classMapService: ClassMapService
) {
    fun getLessonInfoMsg(classMap: ClassMap, classDetail: Map<String, Any> ): String{
        val builder = StringBuilder()
        builder.append("课程:${classMap.className}")

        if (classDetail.isNotEmpty()) {
            builder.append("\n开课周次: ${classDetail["start"]} - ${classDetail["end"]}")
                .append("\n授课教师: ${classDetail["person_name"]}")
            return builder.toString()
        }
        return builder.append("暂无信息").toString()
    }

    fun getCourseDetailMsg(scheduleList: List<Schedule>): List<Message>{
        var builder = MessageChainBuilder()
        scheduleList.stream().collect(Collectors.groupingBy(Schedule::date))
        var today: Date? = null
        val messagesList = ArrayList<Message>()

        val mutableClassMap = classMapService.list().associateBy { it.lessonId }

        for (schedule in scheduleList) {
            val th = schedule.date

            if (today == null || th != today){
                if (today != null){
                    messagesList.add(builder.build())
                    builder = MessageChainBuilder()
                }

                val i: Int = schedule.date!!.toLocalDate().get(ChronoField.DAY_OF_WEEK)
                builder.append(DateUtil.format(th, "MM-dd") + " 周" + WeekUtil.WEEKDAY[i - 1] + ":")
                today = th
            }
            if (th == today) {
                val start = String.format("%02d", schedule.startTime!! / 100) + ":" + String.format("%02d", schedule.startTime!! % 100)

                val end = String.format("%02d", schedule.endTime!! / 100) + ":" + String.format("%02d", schedule.endTime!! % 100)
                builder.append("\n * $start - $end ${schedule.room} ${mutableClassMap[schedule.lessonId]!!.className} ${schedule.personName}")
            }
        }

        return messagesList.apply { this.add(builder.build()) }
    }
}

object WeekUtil {
    val WEEKDAY = arrayOf("一", "二", "三", "四", "五", "六", "日")
}
