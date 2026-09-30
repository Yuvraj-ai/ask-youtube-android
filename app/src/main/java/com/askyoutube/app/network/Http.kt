package com.askyoutube.app.network

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * The single OkHttp instance.
 *
 * The User-Agent is load-bearing, not cosmetic: youtube.com serves a stripped
 * page without a browser UA, and the caption endpoint at api/timedtext returns
 * an empty body for requests that do not look like they came from a browser.
 *
 * A cookie jar is kept because the caption baseUrl is signature- and
 * expiry-bound to the visitor session that fetched the watch page, so the two
 * requests have to share cookies.
 */
object Http {

    const val DESKTOP_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }
}
