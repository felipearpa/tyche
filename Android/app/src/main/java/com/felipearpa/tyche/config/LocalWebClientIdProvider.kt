package com.felipearpa.tyche.config

import com.felipearpa.tyche.BuildConfig
import com.felipearpa.tyche.account.bygoogle.WebClientIdProvider

class LocalWebClientIdProvider : WebClientIdProvider {
    override fun invoke(): String = BuildConfig.GOOGLE_WEB_CLIENT_ID
}
