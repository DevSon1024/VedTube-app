package com.devson.vedtube.core.player

import androidx.media3.common.PlaybackException
import com.devson.vedtube.domain.model.AppError
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps low-level Media3 [PlaybackException] and underlying network/decoder errors
 * into domain [AppError] instances to avoid leaking implementation details to UI.
 */
@Singleton
class PlayerErrorMapper @Inject constructor() {

    fun map(throwable: Throwable): AppError {
        if (throwable is AppError) {
            return throwable
        }

        if (throwable is PlaybackException) {
            return when (throwable.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
                PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> {
                    AppError.Network(
                        message = "Network connection failed during playback: ${throwable.message}",
                        cause = throwable
                    )
                }

                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> {
                    AppError.NotFound(
                        resourceId = null,
                        message = "Media resource unavailable: ${throwable.message}",
                        cause = throwable
                    )
                }

                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> {
                    AppError.Parsing(
                        message = "Media format parsing failed: ${throwable.message}",
                        cause = throwable
                    )
                }

                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED -> {
                    AppError.Playback(
                        message = "Device video decoder failed: ${throwable.message}",
                        cause = throwable
                    )
                }

                else -> {
                    AppError.Playback(
                        message = throwable.message ?: "Playback error occurred (Code ${throwable.errorCode})",
                        cause = throwable
                    )
                }
            }
        }

        return when (throwable) {
            is UnknownHostException,
            is ConnectException,
            is SocketTimeoutException,
            is IOException -> AppError.Network(
                message = throwable.message ?: "Network error during playback",
                cause = throwable
            )
            else -> AppError.Unknown(
                message = throwable.message ?: "Unexpected player error",
                cause = throwable
            )
        }
    }
}
