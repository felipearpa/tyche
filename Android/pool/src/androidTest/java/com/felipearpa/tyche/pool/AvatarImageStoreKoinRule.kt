package com.felipearpa.tyche.pool

import androidx.test.platform.app.InstrumentationRegistry
import com.felipearpa.tyche.account.AvatarUrl
import com.felipearpa.tyche.account.di.avatarImageStoreModule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Starts Koin with the production [avatarImageStoreModule] and an [AvatarUrl] for the
 * duration of a test: every row that renders a gambler resolves the shared `AvatarImageStore`
 * and `AvatarUrl` through Koin, so rendering one without them fails with "KoinApplication has
 * not been started". Coil's own image requests are not intercepted.
 *
 * Declare it with a lower `order` than the Compose rule so Koin outlives the composition.
 */
class AvatarImageStoreKoinRule : TestWatcher() {
    override fun starting(description: Description) {
        startKoin {
            androidContext(InstrumentationRegistry.getInstrumentation().targetContext)
            modules(avatarImageStoreModule, avatarUrlModule)
        }
    }

    override fun finished(description: Description) {
        stopKoin()
    }
}

private val avatarUrlModule = module {
    single { AvatarUrl(base = "https://avatars.example.com") }
}
