package com.edgegesture.evilgodxu.data.translate

import com.edgegesture.evilgodxu.log.CrashLogManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

// 翻译服务商，接口协议与示例项目 ApkMesh 保持一致
enum class TranslationProvider {
    MICROSOFT, GOOGLE, FREE_MODEL
}

class TranslationException(message: String, cause: Throwable? = null) : Exception(message, cause)

// 翻译失败时由接口返回 401 等授权异常触发 token 刷新重试
private class UnauthorizedException : Exception()

/**
 * 翻译客户端，批量请求并拼接结果。
 * 默认使用微软 Edge 免费接口，与示例项目 ApkMesh 的默认配置一致。
 */
class TranslationService private constructor(private val client: OkHttpClient) {

    companion object {
        private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
        private val formMediaType = "application/x-www-form-urlencoded".toMediaType()
        private const val FREE_MODEL_TIMEOUT_SECONDS = 120L

        @Volatile
        private var instance: TranslationService? = null

        fun get(): TranslationService = instance ?: synchronized(this) {
            instance ?: TranslationService(
                OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build()
            ).also { instance = it }
        }
    }

    @Volatile
    private var freeModelJwt: String? = null
    private var freeModelTokenExpiresAt: Long = 0

    // 上一次翻译成功的服务商，下次优先尝试，避免每次都经历不可达服务商的超时
    @Volatile
    private var preferredProvider: TranslationProvider? = null

    // 依次尝试各翻译服务商，网络不可达时自动切换；成功后记住该服务商
    suspend fun translateWithFallback(
        texts: List<String>,
        targetLanguage: String = "system",
        deviceId: String = "",
    ): List<String> {
        var lastError: Throwable? = null
        for (provider in providerOrder()) {
            try {
                val result = translate(texts, provider, targetLanguage, deviceId)
                preferredProvider = provider
                return result
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                CrashLogManager.logException(
                    "TranslationService",
                    "翻译服务商 ${provider.name} 失败，尝试其他服务",
                    e
                )
            }
        }
        throw lastError ?: TranslationException("所有翻译服务均失败")
    }

    private fun providerOrder(): List<TranslationProvider> {
        val preferred = preferredProvider ?: return TranslationProvider.entries.toList()
        return listOf(preferred) + TranslationProvider.entries.filter { it != preferred }
    }

    suspend fun translate(
        texts: List<String>,
        provider: TranslationProvider = TranslationProvider.MICROSOFT,
        targetLanguage: String = "system",
        deviceId: String = "",
    ): List<String> = withContext(Dispatchers.IO) {
        val values = texts.map { it.trim() }
        val translated = MutableList(values.size) { "" }
        val pending = mutableListOf<Pair<Int, String>>()
        val maxTextLength = maxTextLength(provider)
        for ((index, value) in values.withIndex()) {
            if (value.isEmpty()) continue
            var offset = 0
            while (offset < value.length) {
                val end = (offset + maxTextLength).coerceAtMost(value.length)
                pending.add(index to value.substring(offset, end))
                offset = end
            }
        }
        if (pending.isEmpty()) return@withContext translated

        val target = translationLanguageCode(targetLanguage, provider)
        for (batch in batches(pending, provider)) {
            val batchTexts = batch.map { it.second }
            val result = when (provider) {
                TranslationProvider.MICROSOFT -> translateMicrosoft(batchTexts, target)
                TranslationProvider.GOOGLE -> translateGoogle(batchTexts, target)
                TranslationProvider.FREE_MODEL -> translateFreeModel(batchTexts, target, deviceId)
            }
            if (result.size != batch.size) {
                throw TranslationException("翻译接口返回数量与请求不一致")
            }
            for (i in batch.indices) {
                translated[batch[i].first] += result[i]
            }
        }
        translated.map { it.trim() }
    }

    private fun maxTextLength(provider: TranslationProvider) = when (provider) {
        TranslationProvider.MICROSOFT -> 1800
        TranslationProvider.GOOGLE -> 8000
        TranslationProvider.FREE_MODEL -> 12000
    }

    // 按接口的条目数与字符数上限分批，避免单次请求超限
    private fun batches(
        values: List<Pair<Int, String>>,
        provider: TranslationProvider,
    ): List<List<Pair<Int, String>>> {
        val (maxItems, maxChars) = when (provider) {
            TranslationProvider.MICROSOFT -> 50 to 1800
            TranslationProvider.GOOGLE -> 8 to 8000
            TranslationProvider.FREE_MODEL -> 20 to 12000
        }
        val result = mutableListOf<List<Pair<Int, String>>>()
        var current = mutableListOf<Pair<Int, String>>()
        var characters = 0
        for (value in values) {
            val tooManyItems = current.size >= maxItems
            val tooManyCharacters = current.isNotEmpty() && characters + value.second.length > maxChars
            if (tooManyItems || tooManyCharacters) {
                result.add(current)
                current = mutableListOf()
                characters = 0
            }
            current.add(value)
            characters += value.second.length
        }
        if (current.isNotEmpty()) result.add(current)
        return result
    }

    private fun translateMicrosoft(texts: List<String>, target: String): List<String> {
        val url = "https://edge.microsoft.com/translate/translatetext"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("to", target)
            .addQueryParameter("isEnterpriseClient", "false")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .post(JSONArray(texts).toString().toRequestBody(jsonMediaType))
            .build()
        val body = client.newCall(request).execute().use { response ->
            checkSuccess(response, "Microsoft")
            response.body.string()
        }
        val array = try {
            JSONArray(body)
        } catch (e: Exception) {
            throw TranslationException("Microsoft 翻译返回格式无效", e)
        }
        if (array.length() != texts.size) {
            throw TranslationException("Microsoft 翻译返回数量与请求不一致")
        }
        val result = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val translations = array.optJSONObject(i)?.optJSONArray("translations")
            result.add(translations?.optJSONObject(0)?.optString("text", "") ?: "")
        }
        return result
    }

    // Google 的浏览器接口每请求只翻译一条文本
    private fun translateGoogle(texts: List<String>, target: String): List<String> {
        return texts.map { translateGoogleText(it, target) }
    }

    private fun translateGoogleText(text: String, target: String): String {
        val url = "https://translate.googleapis.com/translate_a/t"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("client", "gtx")
            .addQueryParameter("dt", "t")
            .addQueryParameter("sl", "auto")
            .addQueryParameter("tl", target)
            .build()
        val request = Request.Builder()
            .url(url)
            .post(
                ("q=" + URLEncoder.encode(text, "UTF-8")).toRequestBody(formMediaType)
            )
            .build()
        val body = client.newCall(request).execute().use { response ->
            checkSuccess(response, "Google")
            response.body.string()
        }
        val array = try {
            JSONArray(body)
        } catch (e: Exception) {
            throw TranslationException("Google 翻译返回格式无效", e)
        }
        return decodeGoogleText(array)
    }

    private fun translateFreeModel(texts: List<String>, target: String, deviceId: String): List<String> {
        var token = freeModelToken(deviceId, target, forceRefresh = false)
        try {
            return translateFreeModelWithToken(texts, target, token)
        } catch (e: UnauthorizedException) {
            token = freeModelToken(deviceId, target, forceRefresh = true)
            return translateFreeModelWithToken(texts, target, token)
        }
    }

    private fun translateFreeModelWithToken(texts: List<String>, target: String, token: String): List<String> {
        val url = "https://aigw1.immersivetranslate.com/v1/translation/tasks".toHttpUrl()
        val payload = JSONObject().apply {
            put("task_type", "web_page")
            put("stream", false)
            put("payload", JSONObject().apply {
                put("to", target)
                put("content_type", "plain_text")
                put("segments", JSONArray().apply {
                    for (i in texts.indices) {
                        put(JSONObject().put("id", "seg-$i").put("text", texts[i]))
                    }
                })
            })
            put("ui_language", target)
        }
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()
        val call = client.newCall(request)
        call.timeout().timeout(FREE_MODEL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val body = call.execute().use { response ->
            if (response.code == 401) throw UnauthorizedException()
            checkSuccess(response, "免费翻译服务")
            response.body.string()
        }
        val decoded = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw TranslationException("免费翻译服务返回格式无效", e)
        }
        val segments = decoded.optJSONObject("result")?.optJSONArray("segments")
            ?: throw TranslationException("免费翻译服务返回格式无效")
        val byId = mutableMapOf<String, String>()
        for (i in 0 until segments.length()) {
            val item = segments.optJSONObject(i) ?: continue
            val id = item.optString("id")
            if (id.isNotEmpty()) {
                byId[id] = item.optString("translated_text").ifEmpty { item.optString("text") }
            }
        }
        return List(texts.size) { i -> byId["seg-$i"] ?: "" }
    }

    private fun freeModelToken(deviceId: String, language: String, forceRefresh: Boolean): String {
        val now = System.currentTimeMillis()
        val cached = freeModelJwt
        if (!forceRefresh && cached != null && freeModelTokenExpiresAt > now + 2 * 60 * 1000) {
            return cached
        }
        val url = "https://api2.immersivetranslate.com/free-model/get-token"
            .toHttpUrl()
            .newBuilder()
            .addQueryParameter("deviceId", deviceId)
            .addQueryParameter("l", "0")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Accept-Language", language)
            .build()
        val body = client.newCall(request).execute().use { response ->
            checkSuccess(response, "免费翻译服务 Token")
            response.body.string()
        }
        val token = try {
            JSONObject(body).optString("data", "")
        } catch (e: Exception) {
            throw TranslationException("免费翻译服务返回格式无效", e)
        }
        if (token.isEmpty()) {
            throw TranslationException("免费翻译服务没有返回 Token")
        }
        freeModelJwt = token
        freeModelTokenExpiresAt = jwtExpiry(token) ?: now + 60 * 60 * 1000
        return token
    }

    // 解析 JWT 的 exp 字段（UTC 秒）作为 token 过期时间
    private fun jwtExpiry(token: String): Long? {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) {
                null
            } else {
                val payload = JSONObject(
                    String(
                        android.util.Base64.decode(
                            parts[1],
                            android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING
                        ),
                        Charsets.UTF_8
                    )
                )
                payload.optLong("exp", 0L).takeIf { it > 0 }?.times(1000)
            }
        } catch (e: Exception) {
            null
        }
    }

    // Google 返回深层嵌套数组，取第一个叶子节点作为译文
    private fun decodeGoogleText(value: Any?): String {
        var current = value
        while (current is JSONArray && current.length() > 0) {
            current = current.opt(0)
        }
        return if (current is String) decodeEntities(current) else ""
    }

    private fun decodeEntities(value: String): String = value
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace(Regex("&#(\\d+);")) { match ->
            match.groupValues[1].toIntOrNull()
                ?.let { String(Character.toChars(it)) }
                ?: match.value
        }

    private fun checkSuccess(response: okhttp3.Response, provider: String) {
        if (!response.isSuccessful) {
            throw TranslationException("$provider HTTP ${response.code}")
        }
    }
}

// 目标语言代码转换：示例项目同一接口对不同服务商要求的语言代码存在差异
fun translationLanguageCode(language: String, provider: TranslationProvider): String {
    val value = if (language == "system") systemLanguageCode() else language
    return when (provider) {
        TranslationProvider.MICROSOFT -> when (value) {
            "zh-CN" -> "zh-Hans"
            "zh-TW" -> "zh-Hant"
            "pt-BR" -> "pt"
            "pt" -> "pt-PT"
            "no" -> "nb"
            else -> value
        }
        TranslationProvider.GOOGLE -> when (value) {
            "ja" -> "jp"
            "ko" -> "kr"
            "pt-BR" -> "pt"
            else -> value
        }
        TranslationProvider.FREE_MODEL -> value
    }
}

// 跟随系统语言：中文区分简繁，其余直接使用语言代码
fun systemLanguageCode(): String {
    val locale = Locale.getDefault()
    val language = locale.language.lowercase()
    if (language == "zh") {
        return if (locale.country.uppercase() in setOf("TW", "HK", "MO")) "zh-TW" else "zh-CN"
    }
    return language
}
