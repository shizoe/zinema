package com.zinema.app.core.domain.util

import kotlinx.coroutines.flow.Flow

/** Observes network connectivity (blueprint T-060). */
interface ConnectivityObserver {
    /** Emits true when the device has an internet-capable network. */
    val isOnline: Flow<Boolean>
}
