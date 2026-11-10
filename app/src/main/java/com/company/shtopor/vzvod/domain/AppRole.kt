package com.company.shtopor.vzvod.domain

enum class AppRole {
    POLICEMAN,
    COMMANDER;

    val isCommander: Boolean get() = this == COMMANDER
}

object RoleResolver {
    fun fromServer(role: String?, fullAccess: Boolean): AppRole {
        if (fullAccess) return AppRole.COMMANDER
        return when (role?.uppercase()) {
            "FULL_ACCESS" -> AppRole.COMMANDER
            else -> AppRole.POLICEMAN
        }
    }
}
