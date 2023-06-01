package per.chowhound.bot.mirai.framework.components.academic.service

import per.chowhound.bot.mirai.framework.components.academic.entity.ClassMap

/**
 * @author zsck
 * @date   2022/11/8 - 15:44
 */
interface ClassMapService{
    fun removeAll(): Long

    fun likeClassName(name: String): List<ClassMap>

    fun list(): List<ClassMap>
    fun saveBatch(classMapList: ArrayList<ClassMap>): Boolean
}