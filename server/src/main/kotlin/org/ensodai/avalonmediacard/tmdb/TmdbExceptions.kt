package org.ensodai.avalonmediacard.tmdb

open class TmdbException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class TmdbAuthException(message: String, cause: Throwable? = null) : TmdbException(message, cause)

class TmdbNetworkException(message: String, cause: Throwable? = null) : TmdbException(message, cause)

class TmdbTimeoutException(message: String, cause: Throwable? = null) : TmdbException(message, cause)
