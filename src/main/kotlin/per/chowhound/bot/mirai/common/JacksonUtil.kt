package per.chowhound.bot.mirai.common

import cn.hutool.extra.spring.SpringUtil
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.type.CollectionType
import org.springframework.context.annotation.Lazy
import java.util.*
import java.util.function.Function


@Lazy
@Suppress("unused", "MemberVisibilityCanBePrivate")
class JacksonUtil {
    companion object{
        val objectMapper: ObjectMapper = SpringUtil.getBean(ObjectMapper::class.java)

        fun toJsonString(obj: Any): String {
            if (obj is String) {
                return obj
            }
            return objectMapper.writeValueAsString(obj)
        }


        fun readTree(content: Any): JsonNode {
            return objectMapper.readTree(toJsonString(content))
        }

        fun <T> readValue(json: String, clazz: Class<T>): T {
            return objectMapper.readValue(json, clazz)
        }

        fun <T> readValue(content: Any, valueType: JavaType, jsonPath: JsonPath): T {
            return readValue(content, valueType, JsonOperateFunction.fromPath(jsonPath))
        }

        fun <T> readValue(content: Any, valueType: JavaType, function: JsonOperateFunction? = null): T {
            val json = function?.let { toJsonString(it.apply(readTree(content))) }
                ?: toJsonString(content)
            return objectMapper.readValue(json, valueType)
        }

        fun <T> readListValue(content: Any, valueType: Class<T>, function: JsonOperateFunction? = null): List<T> {
            return readValue(content, getListOf(valueType), function)
        }
        fun <T> readListValue(content: Any, valueType: Class<T>, path: JsonPath): List<T> {
            return readValue(content, getListOf(valueType), path)
        }

        private fun getListOf(elementClasses: Class<*>?): CollectionType {
            return objectMapper.typeFactory.constructCollectionType(MutableList::class.java, elementClasses)
        }
    }

}
@Suppress("unused")

class JsonPath private constructor(c: Collection<String>) :
    ArrayList<String>(c) {
    companion object {
        fun of(vararg paths: String): JsonPath {
            return JsonPath(listOf(*paths))
        }
    }
}
@Suppress("unused")
fun interface JsonOperateFunction : Function<JsonNode, JsonNode> {
    companion object {
        fun fromPath(path: JsonPath): JsonOperateFunction {
            return JsonOperateFunction { node: JsonNode ->
                var nodeVar = node
                for (s in path) {
                    nodeVar = nodeVar[s]
                }
                nodeVar
            }
        }

        fun emptyOperate(): JsonOperateFunction {
            return JsonOperateFunction { node: JsonNode -> node }
        }
    }
}
