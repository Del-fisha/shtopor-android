package com.company.shtopor.vzvod.domain

/**
 * Политика сессии мобильного клиента:
 * - каждый новый запуск процесса → снова логин/пароль к серверу;
 * - HTTP 401 (неверный/протухший токен, смена пароля и перевыпуск) → выход.
 */
object SessionAuthPolicy {
    fun mustReauthOnProcessStart(): Boolean = true

    fun mustLogoutOnHttpCode(code: Int): Boolean = code == 401
}
