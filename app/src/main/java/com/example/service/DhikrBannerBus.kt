package com.example.service

import com.example.data.model.ActiveDhikrBanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object DhikrBannerBus {
    private val _activeBanner = MutableStateFlow<ActiveDhikrBanner?>(null)
    val activeBanner: StateFlow<ActiveDhikrBanner?> = _activeBanner.asStateFlow()

    fun post(banner: ActiveDhikrBanner) {
        _activeBanner.value = banner
    }

    fun dismiss() {
        _activeBanner.value = null
    }
}
