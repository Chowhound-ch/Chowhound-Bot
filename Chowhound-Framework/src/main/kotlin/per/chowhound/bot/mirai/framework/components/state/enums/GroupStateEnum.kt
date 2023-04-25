package per.chowhound.bot.mirai.framework.components.state.enums

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 12:38
 * @Description:
 */
@Suppress("unused")
enum class GroupStateEnum(val state: Int) {
    STOP(0),
    START(1);
    companion object {
        fun getState(state: Int): GroupStateEnum {
            return when (state) {
                0 -> STOP
                1 -> START
                else -> STOP
            }
        }

        fun getState(state: String): GroupStateEnum {
            return when (state.uppercase()) {
                "STOP" -> STOP
                "START" -> START
                else -> STOP
            }
        }
    }
}