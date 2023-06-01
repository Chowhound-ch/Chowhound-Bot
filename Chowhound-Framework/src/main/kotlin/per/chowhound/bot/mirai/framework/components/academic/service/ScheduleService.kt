package per.chowhound.bot.mirai.framework.components.academic.service

import per.chowhound.bot.mirai.framework.components.academic.entity.Schedule
import java.sql.Date

/**
 * @author zsck
 * @date   2022/11/8 - 14:51
 */
interface ScheduleService{
    fun removeAll(): Long

    fun getLessonsByWeek(week: Long): List<Schedule>

    fun getLessonsByDate(date: Date): List<Schedule>

    fun getFirstDate(): Date

    fun getClassDetail(lessonId: Long): Map<String, Any>
    fun saveBatch(scheduleList: ArrayList<Schedule>): Boolean
}