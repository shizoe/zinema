package com.zinema.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Login models — confirmed against the decompiled client (`nx/a` API interface,
 * `LoginSmsCodeRequest`, `com.transsnet.loginapi.bean.UserInfo`,
 * `LoginCheckPhoneExistResult`).
 *
 * Login/registration all POST a [LoginRequestBody] and return `ApiResponse<UserInfoData>`.
 * Flow (from check-mail-account → {exists, hasPassword}):
 *   • exists && hasPassword → password login  (user-api/login, type=1/authType=1)
 *   • exists && !hasPassword → SMS login       (get-sms-code → check-sms-code)
 *   • !exists                → register        (get-sms-code → register)
 * Password is sent as-is over HTTPS (no client-side RSA — verified in smali).
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
    val inviteCode: String = "",
)

/** Login/register response `data` — the confirmed `UserInfo` bean. [token] is the JWT. */
@Serializable
data class UserInfoData(
    val token: String = "",
    val userId: String = "",
    val username: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val email: String = "",
    val phone: String = "",
    val cc: String = "",
    val shortId: String = "",
    val userType: Int = 0,
)

@Serializable
data class CheckEmailBody(val mail: String)

/** get-sms-code response `data` (server returns null/minimal; we only check `code`). */
@Serializable
data class SmsCodeResult(val interval: Int = 0)

/** check-mail-account response — confirmed `LoginCheckPhoneExistResult`. */
@Serializable
data class AccountExistsData(
    val exists: Boolean = false,
    val hasPassword: Boolean = false,
    val reset: Boolean = false,
)
