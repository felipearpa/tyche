package com.felipearpa.tyche.config

import com.felipearpa.tyche.BuildConfig
import com.felipearpa.tyche.core.IosBundleIdProvider

class LocalIosBundleIdProvider : IosBundleIdProvider {
    override fun invoke(): String = BuildConfig.IOS_BUNDLE_ID
}
