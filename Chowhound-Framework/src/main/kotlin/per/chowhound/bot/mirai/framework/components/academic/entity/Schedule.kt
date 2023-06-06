package per.chowhound.bot.mirai.framework.components.academic.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.data.mongodb.core.mapping.Document
import java.sql.Date

/**
 * @author zsck
 * @date   2022/11/8 - 14:41
 */
@Document
@JsonIgnoreProperties("room")
data class Schedule(
    val id: String? = null,

    @JsonProperty("lessonId")
    var lessonId: Long? = null,
    @JsonProperty("scheduleGroupId")
    var scheduleGroupId: Int? = null,
    var periods: Int? = null,
    var date: Date? = null,
    var room: String? = null,
    var weekday: Int? = null,
    @JsonProperty("startTime")
    var startTime: Int? = null,
    @JsonProperty("endTime")
    var endTime: Int? = null,
    @JsonProperty("personName")
    var personName: String? = null,
    @JsonProperty("weekIndex")
    var weekIndex: Int? = null
) {
    companion object {
        fun instance(): Schedule = Schedule().apply { this.date = Date(System.currentTimeMillis()) }
    }
//    constructor(): this(null, null, null, null, Date(System.currentTimeMillis()), "xx学堂xx", 0, 800, 950, "未知", 0)
}