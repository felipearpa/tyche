package com.felipearpa.tyche.config

import com.felipearpa.tyche.BuildConfig
import com.felipearpa.tyche.session.SignInLinkUrlTemplateProvider

class LocalSignInLinkUrlTemplateProvider : SignInLinkUrlTemplateProvider {
    override fun invoke(): String = BuildConfig.SIGN_IN_LINK_URL_TEMPLATE
}
