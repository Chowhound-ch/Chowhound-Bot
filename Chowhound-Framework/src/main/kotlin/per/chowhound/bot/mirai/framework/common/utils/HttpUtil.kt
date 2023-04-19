package per.chowhound.bot.mirai.framework.common.utils

import cn.hutool.extra.spring.SpringUtil
import com.fasterxml.jackson.databind.JsonNode
import org.apache.http.Header
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpRequestBase
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import per.chowhound.bot.mirai.framework.common.utils.LoggerUtils.logInfo
import java.io.IOException
import java.net.URI
import java.net.URLEncoder

/**
 * @author zsck
 * @date   2023/1/26 - 20:04
 */

/**
 * 公共的http请求工具类
 */
@Suppress("unused")
object HttpUtil: HttpBase(){
    override fun isDefault(): Boolean {
        return false
    }
}
@Suppress("unused")
open class HttpBase{

    protected open val httpClient: CloseableHttpClient = HttpClients.createDefault()


    @Throws(IOException::class)
    fun doGetStr(url: String, header: Array<Header>? = getHeader(), params: Map<String, Any>? = null, isDefault: Boolean = isDefault()): String {
        val httpGet = HttpGet(getUri(url, params))
        httpGet.setHeaders(header)
        return doHttpRequestStr(httpGet, isDefault)
    }

    @Throws(IOException::class)
    fun doGetBytes(url: String, header: Array<Header>? = getHeader(), params: Map<String, Any>? = null, isDefault: Boolean = isDefault()): ByteArray {
        val httpGet = HttpGet( getUri(url, params) )
        httpGet.setHeaders(header)
        return doHttpRequestBytes(httpGet, isDefault)
    }

    @Throws(IOException::class)
    fun doPostStr(url: String, entity: HttpEntity? = null, header: Array<Header>? = getHeader(), isDefault: Boolean = isDefault()): String {
        val httpPost = HttpPost(url)
        httpPost.setHeaders(header)
        entity?.let { httpPost.entity = it }
        return doHttpRequestStr(httpPost, isDefault)
    }

    @Throws(IOException::class)
    fun doGetJson(url: String, header: Array<Header>? = getHeader(), params: Map<String, Any>? = null, isDefault: Boolean = isDefault()): JsonNode {
        return JacksonUtil.readTree(doGetStr(url, header, params, isDefault))
    }
    @Throws(IOException::class)
    fun doPostJson(url: String, entity: HttpEntity? = null, header: Array<Header>? = getHeader(), isDefault: Boolean = isDefault()): JsonNode {
        return JacksonUtil.readTree(doPostStr(url, entity, header, isDefault))
    }

    private fun doHttpRequestStr(httpRequestBase: HttpRequestBase, isDefault: Boolean = isDefault()): String{
        val httpClient : CloseableHttpClient = if (isDefault) this.httpClient else HttpClients.createDefault()
        try {
            httpClient.execute(httpRequestBase).use { exec -> return EntityUtils.toString(exec.entity) }
        }finally {//若不使用默认httpClient则自动关闭
            if ( !isDefault ){
                httpClient.close()
            }
        }
    }
    private fun doHttpRequestBytes(httpRequestBase: HttpRequestBase, isDefault: Boolean = isDefault()): ByteArray{
        val httpClient : CloseableHttpClient = if (isDefault) this.httpClient else HttpClients.createDefault()
        try {
            httpClient.execute(httpRequestBase).use { exec -> return EntityUtils.toByteArray(exec.entity) }
        }finally {//若不使用默认httpClient则自动关闭
            if ( !isDefault ){
                httpClient.close()
            }
        }
    }

    private fun getUri(url: String, params: Map<String, Any>? = null): URI {
        val uriBuilder = URIBuilder(url)

        params?.let {
            it.forEach { (key, value) -> uriBuilder.addParameter(key, value.toString()) }
        }

        return uriBuilder.build()
    }



    /**
     * 待子类重写
     */
    open fun getHeader(): Array<Header>?{
        return null
    }

    open fun isDefault(): Boolean{
        return true
    }
}

@Suppress("unused", "MemberVisibilityCanBePrivate")
object HttpProxy: HttpBase(){



    override lateinit var httpClient: CloseableHttpClient

    val port: String? = SpringUtil.getProperty("zsck.proxy.port")

    val proxyAddress: String? = SpringUtil.getProperty("zsck.proxy.addr")

    init {

        if (port == null || proxyAddress == null) {
            logInfo("HttpProxy init failed, port or proxyAddress is null")
            httpClient = HttpClients.createDefault()
        }else{
            logInfo("HttpProxy init proxyAddress: {}, port: {}", proxyAddress, port)


            // 使用代理
            val httpHost = HttpHost(proxyAddress, port.toInt())

            httpClient = HttpClients.custom().setProxy(httpHost).build()

            logInfo("HttpProxy init success")
        }

    }

}
@Suppress("unused")
object UrlUtil {
    fun String.urlEncode(charset: String = "UTF-8"): String {
        return URLEncoder.encode(this, charset)
    }

    fun String.toHttpGet(): HttpGet {
        return HttpGet(this)
    }

    fun String.toHttpPost(): HttpPost {
        return HttpPost(this)
    }

}

