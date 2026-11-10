package com.company.shtopor.vzvod.data.remote

import com.company.shtopor.vzvod.data.secure.SecureSessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.CertificatePinner
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class ApiClientFactory(
    private val sessionStore: SecureSessionStore,
    private val onUnauthorizedToken: (() -> Unit)? = null
) {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Volatile
    private var cachedBaseUrl: String? = null

    @Volatile
    private var cachedApi: BotApi? = null

    fun moshi(): Moshi = moshi

    fun api(): BotApi {
        val base = sessionStore.baseUrl.trimEnd('/') + "/"
        val current = cachedApi
        if (current != null && cachedBaseUrl == base) return current
        synchronized(this) {
            if (cachedApi != null && cachedBaseUrl == base) return cachedApi!!
            val client = buildClient()
            val retrofit = Retrofit.Builder()
                .baseUrl(base)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
            cachedApi = retrofit.create(BotApi::class.java)
            cachedBaseUrl = base
            return cachedApi!!
        }
    }

    fun invalidate() {
        synchronized(this) {
            cachedApi = null
            cachedBaseUrl = null
        }
    }

    private fun buildClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val authInterceptor = Interceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Accept", "application/json")
            val key = sessionStore.apiKey
            if (key.isNotBlank()) {
                builder.header("X-Api-Key", key)
            }
            chain.proceed(builder.build())
        }

        // 401 на запросах с X-Mobile-Token → сессия недействительна (смена пароля / новый вход).
        val unauthorizedInterceptor = Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            val hasMobileToken = !request.header("X-Mobile-Token").isNullOrBlank()
            if (hasMobileToken &&
                com.company.shtopor.vzvod.domain.SessionAuthPolicy.mustLogoutOnHttpCode(response.code)
            ) {
                onUnauthorizedToken?.invoke()
            }
            response
        }

        val builder = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthorizedInterceptor)
            .addInterceptor(logging)

        val pins = sessionStore.certificatePins
            .split(',')
            .map { it.trim() }
            .filter { it.startsWith("sha256/") }
        if (pins.isNotEmpty()) {
            val host = runCatching {
                java.net.URI(sessionStore.baseUrl).host
            }.getOrNull()
            if (!host.isNullOrBlank()) {
                val pinnerBuilder = CertificatePinner.Builder()
                pins.forEach { pinnerBuilder.add(host, it) }
                builder.certificatePinner(pinnerBuilder.build())
            }
        }
        return builder.build()
    }
}

sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class Err(val code: Int, val message: String, val offline: Boolean = false) : ApiResult<Nothing>()
}

suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Ok(block())
    } catch (e: retrofit2.HttpException) {
        val body = e.response()?.errorBody()?.string().orEmpty()
        ApiResult.Err(e.code(), body.ifBlank { e.message() })
    } catch (e: java.io.IOException) {
        ApiResult.Err(0, e.message ?: "Нет сети", offline = true)
    } catch (e: Exception) {
        ApiResult.Err(0, e.message ?: "Неизвестная ошибка")
    }
}

fun ApiResult.Err.userMessage(): String {
    val detail = extractServerDetail(message)
    return when {
        offline -> {
            if (detail != null) {
                "Не удалось связаться с сервером: $detail. Проверьте адрес сервера и что Core запущен."
            } else {
                "Нет связи с сервером. Проверьте интернет и адрес сервера в настройках."
            }
        }
        code == 400 -> detail?.let { humanizeServerError(it) } ?: "Проверьте введённые данные."
        code == 401 -> detail?.let { humanizeServerError(it) }
            ?: "Нужен повторный вход: неверный пароль или сессия устарела."
        code == 403 -> detail?.let { humanizeServerError(it) }
            ?: "Доступ запрещён. Возможно, вы не в статусе «В строю»."
        code == 404 -> "Данные не найдены."
        code == 409 -> detail?.let { humanizeServerError(it) }
            ?: "Конфликт: смена уже завершена или запись занята."
        code in 500..599 -> detail?.let { "Ошибка сервера: ${humanizeServerError(it)}" }
            ?: "Сервер временно недоступен (код $code)."
        else -> detail ?: message.ifBlank { "Ошибка (код $code)" }
    }
}

private fun extractServerDetail(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return null
    // Spring часто отдаёт {"detail":"..."} или {"message":"..."}
    val detail = Regex("\"(?:detail|message|title)\"\\s*:\\s*\"([^\"]+)\"").find(trimmed)?.groupValues?.getOrNull(1)
    return (detail ?: trimmed).take(280)
}

private fun humanizeServerError(detail: String): String = when {
    detail.contains("partnerServiceInfoId", ignoreCase = true) ->
        "Не выбран напарник."
    detail.contains("open shift conflict", ignoreCase = true) ->
        "У вас или у напарника уже есть открытая смена."
    detail.contains("invalid routeId", ignoreCase = true) ->
        "Неверный номер маршрута."
    detail.contains("invalid typeOfShiftId", ignoreCase = true) ->
        "Неверный тип смены."
    detail.contains("date and startTime", ignoreCase = true) ->
        "Нужны дата и время начала."
    detail.contains("user already in shift", ignoreCase = true) ->
        "Этот сотрудник уже в смене."
    else -> detail
}
