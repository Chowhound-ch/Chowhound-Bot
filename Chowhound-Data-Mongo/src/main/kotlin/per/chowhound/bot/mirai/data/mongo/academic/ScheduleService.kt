package per.chowhound.bot.mirai.data.mongo.academic

import org.springframework.data.domain.Example
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Service
import per.chowhound.bot.mirai.framework.components.academic.service.ScheduleService
import per.chowhound.bot.mirai.framework.components.academic.entity.Schedule
import java.sql.Date

/**
 * @author zsck
 * @date   2022/11/8 - 14:51
 */
interface ScheduleRepository: MongoRepository<Schedule, Long>{
    @Aggregation("{\$mach: {lesson_id: ?0}}", "{\$group: {_id: '\$lesson_id', minDate: {\$min: '\$date'}, maxDate: {\$max: '\$date'}}}")
    fun getClassDetail(lessonId: Long): Map<String, Any>
}
@Service
class ScheduleServiceImpl(val repository: ScheduleRepository): ScheduleService {
    override fun removeAll() : Long {
        return repository.count().also { repository.deleteAll() }
    }

    override fun getLessonsByWeek(week: Long): List<Schedule> {

//        return list(KtQueryWrapper(Schedule::class.java)
//            .eq(Schedule::weekIndex, week)
//            .orderByAsc(Schedule::date)
//            .orderByAsc(Schedule::startTime))

        // 根据日期和开始时间排序
        return repository.findAll(
            Example.of(Schedule().apply { this.weekIndex = week.toInt() }),
            Sort.sort(Schedule::class.java).by(Schedule::date).ascending().and(Sort.sort(Schedule::class.java).by(
                Schedule::startTime).ascending())
        )
    }

    override fun getLessonsByDate(date: Date): List<Schedule> {
//        return list(KtQueryWrapper(Schedule::class.java)
//            .eq(Schedule::date, date)
//            .orderByAsc(Schedule::startTime))

        return repository.findAll(Example.of(Schedule().apply { this.date = date }))
            .stream().sorted{ o1, o2 -> o1.startTime!!.compareTo(o2.startTime!!) }.toList()
    }

    override fun getFirstDate(): Date {
//        return getMap(QueryWrapper<Schedule>().select("MIN(date) as date"))["date"] as Date

        // 从mongodb查询Schedule，按date属性排序，取第一个

// 创建一个Query对象
        val query = Query()
// 添加排序条件，按date升序
        query.with(Sort.by(Sort.Direction.ASC, "date"))
// 添加限制条件，只取第一个
        query.limit(1)
// 使用MongoTemplate执行查询
        val schedule = repository.findAll(Sort.sort(Schedule::class.java).by(Schedule::date).ascending()).first()
// 或者使用MongoRepository执行查询




        return repository.findAll(Sort.sort(Schedule::class.java).by(Schedule::date).ascending()).first().date!!
    }

    override fun getClassDetail(lessonId: Long): Map<String, Any> {
//        return baseMapper.getClassDetail(lessonId)

        return repository.getClassDetail(lessonId)
    }

    override fun saveBatch(scheduleList: ArrayList<Schedule>): Boolean {
        return repository.saveAll(scheduleList).size == scheduleList.size
    }

}