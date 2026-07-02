package com.zinema.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Login request/response models.
 *
 * authType: 1 = email, 0 = phone. type: 1 = password, 2 = SMS code.
 * `check-mail-account` returns {exists, hasPassword} (confirmed): if hasPassword is
 * true, use password login via `user-api/login`; otherwise the client runs the
 * email→SMS flow (get-sms-code → check-sms-code).
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

/**
 * Login response. ⚠️ NOT confirmed against a capture — no successful `user-api/login`
 * was in the traffic (the captured session logged in via SMS). We only read [token].
 * Non-token field names mirror the confirmed `user-api/profile/v2` userInfo object.
 */
@Serializable
data class UserInfoData(
    val token: String = "",
    val userId: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val mail: String = "",
    val phone: String = "",
)

@Serializable
data class CheckEmailBody(val mail: String)

/** check-mail-account response data (confirmed): {exists, hasPassword}. */
@Serializable
data class AccountExistsData(
    val exists: Boolean = false,
    val hasPassword: Boolean = false,
)
