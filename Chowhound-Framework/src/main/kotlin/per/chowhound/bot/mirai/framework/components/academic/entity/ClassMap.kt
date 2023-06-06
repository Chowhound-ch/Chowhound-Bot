package per.chowhound.bot.mirai.framework.components.academic.entity

import org.springframework.data.mongodb.core.mapping.Document


/**
 * @author zsck
 * @date   2022/11/8 - 14:39
 */
@Document
data class ClassMap(
    val id: String? = null,
    val lessonId: Long? = null,
    val className: String? = null
)