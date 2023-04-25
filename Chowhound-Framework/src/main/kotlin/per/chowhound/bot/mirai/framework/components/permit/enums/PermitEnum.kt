package per.chowhound.bot.mirai.framework.components.permit.enums

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 10:51
 * @Description:
 */
@Suppress("unused", "memberVisibilityCanBePrivate")
enum class PermitEnum(val level: Int) {
    /**
     * 不做要求
     */
    MEMBER(0),
    /**
     * bot主人
     */
    OWNER(2),
    /**
     * bot管理员
     */
    ADMIN(1);

    companion object {
        fun getPermit(level: Int): PermitEnum {
            return when (level) {
                0 -> MEMBER
                1 -> ADMIN
                2 -> OWNER
                else -> MEMBER
            }
        }

        fun getPermit(permit: String): PermitEnum {
            return when (permit.uppercase()) {
                "NONE" -> MEMBER
                "ADMIN" -> ADMIN
                "OWNER" -> OWNER
                else -> MEMBER
            }
        }
    }

    // 重载比较运算符
    operator fun compareTo(level: Int): Int {
        return this.level.compareTo(level)
    }

    fun isHigherThan(permitEnum: PermitEnum): Boolean {
        return this.level > permitEnum.level
    }


    fun isHigherOrEqual(permitEnum: PermitEnum): Boolean {
        return this.level >= permitEnum.level
    }


    fun isLowerThan(permitEnum: PermitEnum): Boolean {
        return this.level < permitEnum.level
    }

    fun isLowerOrEqual(permitEnum: PermitEnum): Boolean {
        return this.level <= permitEnum.level
    }


    fun isEqual(permitEnum: PermitEnum): Boolean {
        return this.level == permitEnum.level
    }


}