package com.felipearpa.tyche.poolhome.drawer

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@EntryPoint
@InstallIn(ActivityComponent::class)
interface DrawerViewModelFactoryProvider {
    fun drawerViewModelFactory(): DrawerViewModelFactory
}