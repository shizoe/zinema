package com.zinema.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Login request/response models (recovered from the decompiled client).
 *
 * authType: 1 = email, 0 = phone. type: 1 = password, 2 = SMS code.
 * ⚠️ Confirm `data.token` is the Bearer JWT field against a captured response.
 */
@Serializable
data class LoginRequestBody(
    val mail: String = "",
    val phone: String = "",
    val cc: String = "",
    val password: String = "",
    val type: Int = 1,
    val authType: Int = 1,
    @SerialName("package_name") val packageName: String = "com.zinema.app",
    val verificationCode: String = "",
)

@Serializable
data class UserInfoData(
    val uid: Long = 0L,
    val token: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val email: String = "",
    val phone: String = "",
)

@Serializable
data class CheckEmailBody(val mail: String)

@Serializable
data class AccountExistsData(val exists: Boolean = false)
