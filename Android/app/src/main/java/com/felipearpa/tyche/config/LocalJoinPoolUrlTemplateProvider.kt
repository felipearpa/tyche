package com.felipearpa.tyche.config

import com.felipearpa.tyche.BuildConfig
import com.felipearpa.tyche.core.JoinPoolUrlTemplateProvider

class LocalJoinPoolUrlTemplateProvider : JoinPoolUrlTemplateProvider {
    override fun invoke(): String = BuildConfig.JOIN_POOL_URL_TEMPLATE
}
