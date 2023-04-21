package per.chowhound.bot.mirai.framework.config.utils

import per.chowhound.bot.mirai.framework.config.MiraiEventListenerConfiguration
import per.chowhound.bot.mirai.framework.config.exception.PatternErrorException

/**
 * @Author: Chowhound
 * @Date: 2023/4/21 - 15:48
 * @Description:
 */
object SyntaxUtil {
    // 解析{{name,pattern}}语法
    fun spiltPattern(pattern: String): MiraiEventListenerConfiguration.CustomPattern {
        val customPattern = MiraiEventListenerConfiguration.CustomPattern()


        var tempPattern = pattern // 用于替换正则表达式中的{{name,pattern}}语法
        var prefixStr: List<String>
        do {
            prefixStr = tempPattern.split("{{")

            customPattern.regParts.add(prefixStr[0])
            if (prefixStr.size == 1){
                break
            }
            val suffixStr = prefixStr[1].split("}}")
            if (suffixStr.size == 1){
                throw PatternErrorException("表达式语法错误，缺少}}")
            }

            suffixStr[0].let {// it :  name,pattern
                val split = it.split(",")
                if (split.size == 1){
                    throw PatternErrorException("表达式语法错误，期望的语法:{{name,pattern}}")
                }
                customPattern.regParts.add(split[1])
                customPattern.fieldMap[split[0]] = split[1]
            }

            tempPattern = suffixStr[1]
        } while  (true)


        return customPattern
    }
}