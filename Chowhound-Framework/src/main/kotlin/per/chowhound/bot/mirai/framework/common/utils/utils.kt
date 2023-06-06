package per.chowhound.bot.mirai.framework.common.utils

import cn.hutool.core.util.ClassUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

/**
 * @Author: Chowhound
 * @Date: 2023/4/19 - 17:05
 * @Description: 一般工具类
 */
@Suppress("unused")
object Utils{
    val CLASS_LOADER: ClassLoader = ClassUtil.getClassLoader()

}
/**
 * @author Chowhound
 * @date   2023/6/6 - 15:50
 *
 * AnnotationUtils.getAnnotation()的扩展函数，增加对kotlin反射的支持
 */




/**
 * @Author: Chowhound
 * @Date: 2023/4/19 - 17:05
 * @Description: 日志工具类
 */
@Suppress("unused")
object LoggerUtils {
    fun  Any.logInfo(msg : String, vararg params: Any) {
        Log.log(this.javaClass, Log.LogLevel.INFO, msg, params)
    }
    fun  Any.logTrace(msg : String, vararg params: Any) {
        Log.log(this.javaClass, Log.LogLevel.TRACE, msg, params)
    }
    fun  Any.logError(msg : String, vararg params: Any) {
        Log.log(this.javaClass, Log.LogLevel.ERROR, msg, params)
    }
    fun  Any.logDebug(msg : String, vararg params: Any) {
        Log.log(this.javaClass, Log.LogLevel.DEBUG, msg, params)
    }
    fun  Any.logWarn(msg : String, vararg params: Any) {
        Log.log(this.javaClass, Log.LogLevel.WARN, msg, params)
    }

    object Log{
        private val logs : MutableMap<Class<Any>, Logger> = HashMap()

        fun log(clazz: Class<Any>, level: LogLevel, msg: String, params: Array<out Any>){

            val logger = logs.computeIfAbsent(clazz) {
                LoggerFactory.getLogger(it)
            }
            when (level){
                LogLevel.INFO -> logger.info(msg, *params)
                LogLevel.TRACE -> logger.trace(msg, *params)
                LogLevel.DEBUG -> logger.debug(msg, *params)
                LogLevel.WARN -> logger.warn(msg, *params)
                LogLevel.ERROR -> logger.error(msg, *params)
            }
        }

        enum class LogLevel{
            TRACE,
            DEBUG,
            INFO,
            WARN,
            ERROR
        }
    }

}

/**
 * @Author: Chowhound
 * @Date: 2023/4/19 - 17:05
 * @Description: 文件路径工具类
 */
object FileUrlUtils{
    fun String.toPath(): Path = Paths.get(this)
}