package per.chowhound.bot.mirai.framework.components.academic.listener

import cn.hutool.core.date.DateUnit
import cn.hutool.core.date.DateUtil
import net.mamoe.mirai.event.events.FriendMessageEvent
import org.springframework.stereotype.Component
import per.chowhound.bot.mirai.framework.common.MessageSender.send
import per.chowhound.bot.mirai.framework.components.academic.Academic
import per.chowhound.bot.mirai.framework.components.academic.service.ClassMapService
import per.chowhound.bot.mirai.framework.components.academic.service.ScheduleService
import per.chowhound.bot.mirai.framework.components.academic.util.AcademicUtil
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.components.state.service.GroupStateService
import per.chowhound.bot.mirai.framework.config.Listener
import java.sql.Date
import java.time.LocalDate

/**
 * @author zsck
 * @date   2022/11/10 - 8:30
 */
@Suppress("unused")
@Component
class AcademicListener(
    val scheduleService: ScheduleService,
    val classMapService: ClassMapService,
    val academicUtil: AcademicUtil,
    val groupStateService: GroupStateService,
    val academic: Academic
){

    @Listener("{{index,\\d{1,2}}}")
    suspend fun FriendMessageEvent.viewWeek(index: Long ){


        scheduleService.getLessonsByWeek(index).let { schedule ->
            academicUtil.getCourseDetailMsg(schedule).forEach { send(it) }
        }
    }

    @Listener("(w|W){{param,(\\+|-|=)?}}")
    suspend fun FriendMessageEvent.week(param: String){
        val firstDate = scheduleService.getFirstDate()
        val date = Date.valueOf(DateUtil.today())
        val standard = getBalanceByParam(param)

        val gap: Long = DateUtil.between(firstDate, date, DateUnit.WEEK, false) + standard.value

        if (gap >= 0) {
            send("${standard.week} 为第 $gap 周")
        } else {
            send("${standard.week} 尚未开学,距离开学还有 ${DateUtil.between(
                date, firstDate, DateUnit.DAY, true)} 天")
            return
        }

        val scheduleList = scheduleService.getLessonsByWeek(gap)
        if (scheduleList.isEmpty()){
            send("${standard.week} 无课程")
            return
        }

        academicUtil.getCourseDetailMsg(scheduleList).forEach{
            send(it)
        }

    }

    @Listener("(d|D){{param,(\\+|-|=)?}}")
    suspend fun FriendMessageEvent.day(param: String){
        val firstDate = scheduleService.getFirstDate()
        val standard = getBalanceByParam(param)
        val date = standard.getDateIfDay()

        if (date.before(firstDate)) {
            send("该日正处于假期，距离开学:${DateUtil.between(date, firstDate, DateUnit.DAY, true)}天")
        } else {
            val gap = DateUtil.between(firstDate, date, DateUnit.DAY, true)
            val scheduleList = scheduleService.getLessonsByDate(date)
            send("${standard.day} 日期: $date ,属于第 ${gap / 7 + 1} 周")
            if (scheduleList.isEmpty()) {
                send("当日无课程")
                return
            }
            academicUtil.getCourseDetailMsg(scheduleList).forEach {
                send(it)
            }
        }
    }
    @Listener("(f|F)\\s*{{name}}")
    suspend fun FriendMessageEvent.find(name: String){
        val classMapList = classMapService.likeClassName(name)

        if (classMapList.isEmpty()){
            send("未查询到符合条件的信息")
        }else{
            for (classMap in classMapList) {
                val classDetail = scheduleService.getClassDetail(classMap.lessonId!!)

                send(academicUtil.getLessonInfoMsg(classMap, classDetail))
            }
        }
    }


    @Listener("刷新课表", permit = PermitEnum.OWNER)
    suspend fun FriendMessageEvent.refreshSchedule() = send( academic.refresh() )


    fun getBalanceByParam(param: String): Standard {
        return when (param) {
            "+" -> {
                Standard.NEXT
            }
            "-" -> {
                Standard.LAST
            }
            else -> Standard.THIS
        }
    }

    enum class Standard(val value: Int, val week: String, val day: String) {
        //今天、明天、昨天 或 本周、下周、上周
        THIS(1, "本周", "今天"), NEXT(2, "下周", "明天"), LAST(0, "上周", "昨天");

        fun getDateIfDay(): Date{

            return Date.valueOf(LocalDate.now().let {
                if (value > 1) it.plusDays(1)
                else if (value < 1) it.minusDays(1)
                else it
            })
        }
    }
}