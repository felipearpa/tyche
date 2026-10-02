package com.felipearpa.tyche.pool

import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.request.ImageResult
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Replaces Coil's singleton image loader for the duration of a test with one that records the
 * data of every request it receives and answers each with an error, so nothing reaches the
 * network. `AccountAvatar` loads through the singleton, so [requestedData] lists every avatar
 * URL a composition asked for.
 */
@OptIn(DelicateCoilApi::class)
class RecordingImageLoaderRule : TestWatcher() {
    private val recorded = CopyOnWriteArrayList<Any>()

    val requestedData: List<Any> get() = recorded.toList()

    override fun starting(description: Description) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val recordingInterceptor = Interceptor { chain ->
            recorded += chain.request.data
            ErrorResult(
                image = null,
                request = chain.request,
                throwable = IllegalStateException("Image requests are recorded, not served"),
            ) as ImageResult
        }
        SingletonImageLoader.setUnsafe(
            ImageLoader.Builder(context)
                .components { add(recordingInterceptor) }
                .build(),
        )
    }

    override fun finished(description: Description) {
        SingletonImageLoader.reset()
    }
}
