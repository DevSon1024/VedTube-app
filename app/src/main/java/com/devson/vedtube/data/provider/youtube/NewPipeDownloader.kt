package com.devson.vedtube.data.provider.youtube

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Custom [Downloader] implementation utilizing the application's configured [OkHttpClient]
 * with robust browser header spoofing and rate-limiting mitigation.
 */
@Singleton
class NewPipeDownloader @Inject constructor(
    private val okHttpClient: OkHttpClient
) : Downloader() {

    private val defaultUserAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .url(url)

        var hasUserAgent = false
        headers.forEach { (key, values) ->
            if (key.equals("User-Agent", ignoreCase = true)) {
                hasUserAgent = true
            }
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        if (!hasUserAgent) {
            requestBuilder.header("User-Agent", defaultUserAgent)
        }

        if (url.contains("youtube.com") || url.contains("googlevideo.com")) {
            requestBuilder.header("Origin", "https://www.youtube.com")
            requestBuilder.header("Referer", "https://www.youtube.com/")
            requestBuilder.header("Sec-Fetch-Mode", "navigate")
            requestBuilder.header("Sec-Fetch-Site", "cross-site")
        }

        if (httpMethod.equals("POST", ignoreCase = true) ||
            httpMethod.equals("PUT", ignoreCase = true) ||
            httpMethod.equals("PATCH", ignoreCase = true)
        ) {
            val contentType = headers["Content-Type"]?.firstOrNull() ?: "application/json; charset=UTF-8"
            val body = (dataToSend ?: ByteArray(0)).toRequestBody(contentType.toMediaTypeOrNull())
            requestBuilder.method(httpMethod, body)
        } else if (httpMethod.equals("HEAD", ignoreCase = true)) {
            requestBuilder.head()
        } else {
            requestBuilder.get()
        }

        val okHttpResponse = okHttpClient.newCall(requestBuilder.build()).execute()
        val responseCode = okHttpResponse.code

        if (responseCode == 429) {
            okHttpResponse.close()
            throw ReCaptchaException("reCAPTCHA or Rate Limit triggered (HTTP 429)", url)
        }

        val responseBody = okHttpResponse.body?.string() ?: ""
        val responseMessage = okHttpResponse.message
        val responseHeaders = mutableMapOf<String, List<String>>()
        for (name in okHttpResponse.headers.names()) {
            responseHeaders[name] = okHttpResponse.headers.values(name)
        }
        val latestUrl = okHttpResponse.request.url.toString()

        return Response(
            responseCode,
            responseMessage,
            responseHeaders,
            responseBody,
            latestUrl
        )
    }
}
