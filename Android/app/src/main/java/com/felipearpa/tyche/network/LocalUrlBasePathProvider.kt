package com.felipearpa.tyche.network

import com.felipearpa.tyche.BuildConfig
import com.felipearpa.tyche.core.network.UrlBasePathProvider

class LocalUrlBasePathProvider : UrlBasePathProvider {
    override val basePath: String = BuildConfig.URL_BASE_BATH
}