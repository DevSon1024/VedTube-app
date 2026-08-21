package com.devson.vedtube.domain.model

/**
 * Domain-level error hierarchy preventing leakage of low-level extractor or network exceptions.
 */
sealed class AppError(
    message: String? = null,
    cause: Throwable? = null
) : Exception(message, cause) {

    data class Network(
        override val message: String = "Network connection failed",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class NotFound(
        val resourceId: String? = null,
        override val message: String = if (resourceId != null) "Resource not found: $resourceId" else "Requested resource was not found",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class GeoRestricted(
        override val message: String = "Content is not available in your region",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class RateLimited(
        override val message: String = "Too many requests. Please try again later.",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class ContentUnavailable(
        override val message: String = "This content is unavailable or private",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Parsing(
        override val message: String = "Failed to parse content data",
        override val cause: Throwable? = null
    ) : AppError(message, cause)

    data class Unknown(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError(message, cause)
}
