# Is your app calling lots of API endpoints parallelly or server sending huge JSON response and your log cat is getting messy..? using following snippet you can only that is nessary 

![image](https://user-images.githubusercontent.com/46936065/226174421-532e86cd-9a9b-4f72-949d-3e77f82edb2a.png)

## Useage 

```java
    @GET("/api/my_profile_details")
    @LogThisRequest(logCatTag = "MyProfileDetails")
    suspend fun getMyProfileDetails(): ProfileDetails
```


## Copy this code into your project

```java

import android.util.Log
import okhttp3.*
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.GzipSource
import retrofit2.Invocation
import java.io.EOFException
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets.UTF_8
import java.util.*
import java.util.concurrent.TimeUnit

annotation class LogThisRequest(
    val logCatTag: String = "OkHttp",
)

/*Caching for reflection*/
private val registration: MutableMap<Int, LogThisRequest> = mutableMapOf()

private fun findAnnotation(
  request: Request,
): LogThisRequest? {
    val key = request.url.hashCode()
    return registration[key] ?: request.tag(Invocation::class.java)
        ?.method()
        ?.annotations
        ?.filterIsInstance<LogThisRequest>()
        ?.firstOrNull()
        ?.also {
            registration[key] = it
        }
}

/**
 * An OkHttp interceptor which logs request and response information. Can be applied as an
 * [application interceptor][OkHttpClient.interceptors] or as a [OkHttpClient.networkInterceptors].
 *
 * The format of the logs created by this class should not be considered stable and may
 * change slightly between releases. If you need a stable logging format, use your own interceptor.
 */
class HttpLoggingInterceptor2 @JvmOverloads constructor(
  private val logger: Logger = Logger.DEFAULT,
) : Interceptor {

    @Volatile
    private var headersToRedact = emptySet<String>()

    @set:JvmName("level")
    @Volatile
    var level = Level.NONE

    enum class Level {
        /** No logs. */
        NONE,

        /**
         * Logs request and response lines.
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1 (3-byte body)
         *
         * <-- 200 OK (22ms, 6-byte body)
         * ```
         */
        BASIC,

        /**
         * Logs request and response lines and their respective headers.
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         * <-- END HTTP
         * ```
         */
        HEADERS,

        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         *
         * Example:
         * ```
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END POST
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * ```
         */
        BODY
    }

    interface Logger {
        fun log(tag: String, message: String)

        companion object {
            /** A [Logger] defaults output appropriate for the current platform. */
            @JvmField
            val DEFAULT: Logger = object : Logger {
                override fun log(tag: String, message: String) {
                    Log.d(tag, message)
//          Platform.get().log(INFO, message, null)
                }
            }
        }
    }

    fun redactHeader(name: String) {
        val newHeadersToRedact = TreeSet(String.CASE_INSENSITIVE_ORDER)
        newHeadersToRedact += headersToRedact
        newHeadersToRedact += name
        headersToRedact = newHeadersToRedact
    }

    @Deprecated(
        message = "Moved to var. Replace setLevel(...) with level(...) to fix Java",
        replaceWith = ReplaceWith(expression = "apply { this.level = level }"),
        level = DeprecationLevel.WARNING)
    fun setLevel(level: Level) = apply {
        this.level = level
    }

    @JvmName("-deprecated_level")
    @Deprecated(
        message = "moved to var",
        replaceWith = ReplaceWith(expression = "level"),
        level = DeprecationLevel.ERROR)
    fun getLevel(): Level = level

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val annotation: LogThisRequest =
            findAnnotation(chain.request()) ?: return chain.proceed(request = chain.request())

        val tag = annotation.logCatTag
        val level = this.level

        val request = chain.request()
        if (level == Level.NONE) {
            return chain.proceed(request)
        }

        val logBody = level == Level.BODY
        val logHeaders = logBody || level == Level.HEADERS

        val requestBody = request.body

        val connection = chain.connection()
        var requestStartMessage =
            ("--> ${request.method} ${request.url}${if (connection != null) " " + connection.protocol() else ""}")
        if (!logHeaders && requestBody != null) {
            requestStartMessage += " (${requestBody.contentLength()}-byte body)"
        }
        logger.log(tag, requestStartMessage)

        if (logHeaders) {
            val headers = request.headers

            if (requestBody != null) {
                // Request body headers are only present when installed as a network interceptor. When not
                // already present, force them to be included (if available) so their values are known.
                requestBody.contentType()?.let {
                    if (headers["Content-Type"] == null) {
                        logger.log(tag, "Content-Type: $it")
                    }
                }
                if (requestBody.contentLength() != -1L) {
                    if (headers["Content-Length"] == null) {
                        logger.log(tag, "Content-Length: ${requestBody.contentLength()}")
                    }
                }
            }

            for (i in 0 until headers.size) {
                logHeader(tag, headers, i)
            }

            if (!logBody || requestBody == null) {
                logger.log(tag, "--> END ${request.method}")
            } else if (bodyHasUnknownEncoding(request.headers)) {
                logger.log(tag, "--> END ${request.method} (encoded body omitted)")
            } else if (requestBody.isDuplex()) {
                logger.log(tag, "--> END ${request.method} (duplex request body omitted)")
            } else {
                val buffer = Buffer()
                requestBody.writeTo(buffer)

                val contentType = requestBody.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                logger.log(tag, "")
                if (buffer.isProbablyUtf8()) {
                    logger.log(tag, buffer.readString(charset))
                    logger.log(tag,
                        "--> END ${request.method} (${requestBody.contentLength()}-byte body)")
                } else {
                    logger.log(
                        tag,
                        "--> END ${request.method} (binary ${requestBody.contentLength()}-byte body omitted)")
                }
            }
        }

        val startNs = System.nanoTime()
        val response: Response
        try {
            response = chain.proceed(request)
        } catch (e: Exception) {
            logger.log(tag, "<-- HTTP FAILED: $e")
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)

        val responseBody = response.body!!
        val contentLength = responseBody.contentLength()
        val bodySize = if (contentLength != -1L) "$contentLength-byte" else "unknown-length"
        logger.log(tag,
            "<-- ${response.code}${if (response.message.isEmpty()) "" else ' ' + response.message} ${response.request.url} (${tookMs}ms${if (!logHeaders) ", $bodySize body" else ""})")

        if (logHeaders) {
            val headers = response.headers
            for (i in 0 until headers.size) {
                logHeader(tag, headers, i)
            }

            if (!logBody || !response.promisesBody()) {
                logger.log(tag, "<-- END HTTP")
            } else if (bodyHasUnknownEncoding(response.headers)) {
                logger.log(tag, "<-- END HTTP (encoded body omitted)")
            } else {
                val source = responseBody.source()
                source.request(Long.MAX_VALUE) // Buffer the entire body.
                var buffer = source.buffer

                var gzippedLength: Long? = null
                if ("gzip".equals(headers["Content-Encoding"], ignoreCase = true)) {
                    gzippedLength = buffer.size
                    GzipSource(buffer.clone()).use { gzippedResponseBody ->
                        buffer = Buffer()
                        buffer.writeAll(gzippedResponseBody)
                    }
                }

                val contentType = responseBody.contentType()
                val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                if (!buffer.isProbablyUtf8()) {
                    logger.log(tag, "")
                    logger.log(tag, "<-- END HTTP (binary ${buffer.size}-byte body omitted)")
                    return response
                }

                if (contentLength != 0L) {
                    logger.log(tag, "")
                    logger.log(tag, buffer.clone().readString(charset))
                }

                if (gzippedLength != null) {
                    logger.log(tag,
                        "<-- END HTTP (${buffer.size}-byte, $gzippedLength-gzipped-byte body)")
                } else {
                    logger.log(tag, "<-- END HTTP (${buffer.size}-byte body)")
                }
            }
        }

        return response
    }

    private fun logHeader(tag: String, headers: Headers, i: Int) {
        val value = if (headers.name(i) in headersToRedact) "██" else headers.value(i)
        logger.log(tag, headers.name(i) + ": " + value)
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }
}

fun Buffer.isProbablyUtf8(): Boolean {
    try {
        val prefix = Buffer()
        val byteCount = size.coerceAtMost(64)
        copyTo(prefix, 0, byteCount)
        for (i in 0 until 16) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return false
            }
        }
        return true
    } catch (_: EOFException) {
        return false // Truncated UTF-8 sequence.
    }
}

```

## Add this intercepter at end while creating OkHttpClient  

```java
    @Provides
    @Singleton
    fun providesOkHttpClient(
        authTokenInterceptor: AuthTokenInterceptor,
        bodyLogger: HttpLoggingInterceptor2
    ):OkHttpClient{
        return OkHttpClient
            .Builder()
            .addInterceptor(authTokenInterceptor)
            .addInterceptor(bodyLogger)
            .build()
    }
```
