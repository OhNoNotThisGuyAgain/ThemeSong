package com.spotzones.domain.util

/**
 * Explicit success/failure result used across layer boundaries instead of throwing.
 *
 * Forces call sites to handle the failure path, which is how the app meets its "degrade gracefully,
 * never crash" requirement. [DomainError] enumerates the failure modes the UI must distinguish.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = (this as? Success)?.value

    fun <R> map(transform: (T) -> R): Outcome<R> = when (this) {
        is Success -> Success(transform(value))
        is Failure -> this
    }

    companion object {
        inline fun <T> catching(block: () -> T): Outcome<T> = try {
            Success(block())
        } catch (t: Throwable) {
            Failure(DomainError.Unexpected(t.message ?: t.javaClass.simpleName, t))
        }
    }
}

inline fun <T> Outcome<T>.onSuccess(block: (T) -> Unit): Outcome<T> {
    if (this is Outcome.Success) block(value)
    return this
}

inline fun <T> Outcome<T>.onFailure(block: (DomainError) -> Unit): Outcome<T> {
    if (this is Outcome.Failure) block(error)
    return this
}

/** Closed set of failures the app knows how to recover from or report. */
sealed interface DomainError {
    val message: String

    data object SpotifyNotInstalled : DomainError {
        override val message = "Spotify isn't installed on this device."
    }
    data object SpotifyNotAuthorized : DomainError {
        override val message = "PlayZones isn't connected to Spotify yet."
    }
    data class SpotifyRemote(override val message: String) : DomainError
    data object NoNetwork : DomainError {
        override val message = "No internet connection."
    }
    data object LocationUnavailable : DomainError {
        override val message = "Couldn't determine your location."
    }
    data object PermissionDenied : DomainError {
        override val message = "A required permission was denied."
    }
    data class NotFound(override val message: String) : DomainError
    data class Validation(override val message: String) : DomainError
    data class Unexpected(override val message: String, val cause: Throwable? = null) : DomainError
}
