package per.chowhound.bot.mirai.framework.config

/**
 * @Author: Chowhound
 * @Date: 2023/4/25 - 14:25
 * @Description:
 */
class StorageTypeConfiguration {
}

enum class StorageType {
    /**
     * 使用mysql存储
     */
    MYSQL,

    /**
     * 使用mongodb存储
     */
    MONGO,

    /**
     * 使用存储在本地的json文件存储
     */
    LOCAL
}