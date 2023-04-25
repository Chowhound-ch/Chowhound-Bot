package per.chowhound.bot.mirai.framework.components.permit

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import per.chowhound.bot.mirai.framework.components.permit.enums.PermitEnum
import per.chowhound.bot.mirai.framework.components.permit.service.PermitService

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 14:06
 * @Description: 检查owner权限
 */
@Component
class OwnerPermitInspector(val permitService: PermitService,@Value("\${chowhound.owner}") val owner: Long) {
    @PostConstruct
    fun init() {
        permitService.setUsedHostPermit(owner, PermitEnum.OWNER)
        permitService.setPermit(owner, PermitEnum.OWNER)
        logInfo("owner permit init success")
    }
}