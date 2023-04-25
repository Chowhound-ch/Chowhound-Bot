package per.chowhound.bot.mirai.framework.components.permit.service

import per.chowhound.bot.mirai.framework.components.permit.entity.Permit
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum

interface PermitService{
    fun getPermit(qqNumber: Long): Permit

    fun setPermit(qqNumber: Long, permit: PermitEnum): Permit

    fun setUsedHostPermit(qqNumber: Long, permit: PermitEnum): MutableList<Permit>
}