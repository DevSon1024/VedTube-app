package com.devson.vedtube.data.provider.youtube

import com.devson.vedtube.domain.model.AppError
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.PrivateContentException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps raw low-level exceptions (HTTP, NewPipe Extractor, Socket) into domain [AppError] types.
 */
object YoutubeErrorMapper {

    fun map(throwable: Throwable, resourceId: String? = null): AppError {
        return when (throwable) {
            is AppError -> throwable

            is GeographicRestrictionException -> AppError.GeoRestricted(
                message = "This video is not available in your country/region",
                cause = throwable
            )

            is PrivateContentException -> AppError.ContentUnavailable(
                message = "This video is private",
                cause = throwable
            )

            is PaidContentException -> AppError.ContentUnavailable(
                message = "This video is paid/premium content and cannot be played",
                cause = throwable
            )

            is AgeRestrictedContentException -> AppError.ContentUnavailable(
                message = "This video is age-restricted and requires sign-in",
                cause = throwable
            )

            is ContentNotAvailableException -> {
                val msg = throwable.message ?: ""
                if (msg.contains("not found", ignoreCase = true) || msg.contains("404")) {
                    AppError.NotFound(resourceId = resourceId, message = "Video not found", cause = throwable)
                } else {
                    AppError.ContentUnavailable(
                        message = throwable.message ?: "This content is currently unavailable",
                        cause = throwable
                    )
                }
            }

            is ReCaptchaException -> AppError.RateLimited(
                message = "reCAPTCHA / Rate limit triggered by YouTube",
                cause = throwable
            )

            is ParsingException -> AppError.Parsing(
                message = throwable.message ?: "Failed to parse YouTube metadata",
                cause = throwable
            )

            is SocketTimeoutException -> AppError.Network(
                message = "Connection timed out. Please check your internet connection.",
                cause = throwable
            )

            is UnknownHostException, is ConnectException -> AppError.Network(
                message = "Unable to connect to YouTube servers. Please verify your network.",
                cause = throwable
            )

            is IOException -> AppError.Network(
                message = throwable.message ?: "A network communication error occurred",
                cause = throwable
            )

            is ExtractionException -> {
                val msg = throwable.message ?: ""
                when {
                    msg.contains("geo", ignoreCase = true) || msg.contains("region", ignoreCase = true) ->
                        AppError.GeoRestricted(cause = throwable)
                    msg.contains("private", ignoreCase = true) ->
                        AppError.ContentUnavailable(message = "This video is private", cause = throwable)
                    msg.contains("not found", ignoreCase = true) || msg.contains("404") ->
                        AppError.NotFound(resourceId = resourceId, cause = throwable)
                    msg.contains("rate", ignoreCase = true) || msg.contains("captcha", ignoreCase = true) ->
                        AppError.RateLimited(cause = throwable)
                    else -> AppError.Parsing(message = msg.ifBlank { "Extraction failed" }, cause = throwable)
                }
            }

            else -> {
                val msg = throwable.message ?: ""
                when {
                    msg.contains("404") || msg.contains("not found", ignoreCase = true) ->
                        AppError.NotFound(resourceId = resourceId, cause = throwable)
                    msg.contains("429") || msg.contains("rate limit", ignoreCase = true) ->
                        AppError.RateLimited(cause = throwable)
                    else -> AppError.Unknown(
                        message = msg.ifBlank { "An unexpected error occurred during extraction" },
                        cause = throwable
                    )
                }
            }
        }
    }
}
