package com.askyoutube.app.domain

/**
 * Every way a request can fail, each with a message worth showing a person.
 *
 * The source app had no error handling at all (defect D7): a bad key, an
 * unreachable video and an exhausted quota all surfaced as a Python traceback.
 * Modelling them as a sealed type keeps the UI from having to parse strings.
 */
sealed class AppError {

    /** Shown to the user. Deliberately never contains the API key. */
    abstract val message: String

    object NoApiKey : AppError() {
        override val message = "Add your Gemini API key in Settings first."
    }

    object InvalidApiKey : AppError() {
        override val message = "Your Gemini API key was rejected. Check it in Settings."
    }

    object QuotaExceeded : AppError() {
        override val message = "You have used up your Gemini quota for now. Try again later."
    }

    object ModelUnavailable : AppError() {
        override val message = "That Gemini model is unavailable. Change it in Settings."
    }

    object Overloaded : AppError() {
        override val message = "Gemini is busy right now. Try again."
    }

    object InvalidUrl : AppError() {
        override val message = "That does not look like a YouTube link."
    }

    object NoCaptions : AppError() {
        override val message = "This video has no captions, so there is no transcript to read."
    }

    /**
     * YouTube answers HTTP 200 with a zero-length body when it will not serve a
     * caption track — observed from datacenter IPs, where the timedtext
     * endpoint returns text/html with content-length 0 rather than an error.
     * Treating that as an empty transcript is what produces a useful message
     * instead of a confusing "the model had nothing to read".
     */
    object EmptyTranscript : AppError() {
        override val message =
            "YouTube returned an empty transcript. This sometimes happens on some networks."
    }

    object NoEmbeddingKey : AppError() {
        override val message = "Add a separate embedding key in Settings, or use the same key as the chat provider."
    }

    object NoBaseUrl : AppError() {
        override val message = "Set the server address in Settings before using a custom model."
    }

    object Network : AppError() {
        override val message = "No connection. Check your network and try again."
    }

    data class Unexpected(val detail: String) : AppError() {
        override val message = "Something went wrong: $detail"
    }

    companion object {
        /**
         * Maps a Gemini HTTP status onto a typed failure.
         *
         * 400 and 403 are both "your key is not usable", 404 means the model id
         * is gone, 429 is quota or rate limiting, 5xx is a Google-side problem.
         */
        fun fromHttpStatus(code: Int): AppError = when (code) {
            400, 401, 403 -> InvalidApiKey
            404 -> ModelUnavailable
            429 -> QuotaExceeded
            in 500..599 -> Overloaded
            else -> Unexpected("HTTP $code")
        }
    }
}

/** Thrown internally so the repository layer can use exceptions; mapped to
 *  [AppError] at the boundary. Never escapes the ViewModel. */
class AppException(val error: AppError) : Exception(error.message)
