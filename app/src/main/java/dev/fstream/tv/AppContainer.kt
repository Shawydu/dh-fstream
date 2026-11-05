package dev.fstream.tv

import dev.fstream.tv.provider.ProviderAdapter
import dev.fstream.tv.provider.strmfree.StrmFreeAdapter
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer {
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    val providers: List<ProviderAdapter> = listOf(StrmFreeAdapter(httpClient))
}
