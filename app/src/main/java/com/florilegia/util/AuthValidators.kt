package com.florilegia.util

data class PasswordValidationState(
    val hasMinLength: Boolean = false,
    val hasLetter: Boolean = false,
    val hasDigit: Boolean = false,
    val hasSpecialChar: Boolean = false,
) {
    val isValid: Boolean
        get() = hasMinLength && hasLetter && hasDigit && hasSpecialChar

    fun firstErrorOrNull(): String? = when {
        !hasMinLength -> "Password must be at least 8 characters long."
        !hasLetter -> "Password must contain at least one letter."
        !hasDigit -> "Password must contain at least one number."
        !hasSpecialChar -> "Password must contain at least one special character (e.g. !@#\$%^&*)."
        else -> null
    }
}

object AuthValidators {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean =
        EMAIL_REGEX.matches(email.trim())

    fun evaluatePassword(password: String): PasswordValidationState =
        PasswordValidationState(
            hasMinLength = password.length >= 8,
            hasLetter = password.any { it.isLetter() },
            hasDigit = password.any { it.isDigit() },
            hasSpecialChar = password.any { !it.isLetterOrDigit() && !it.isWhitespace() },
        )

    fun validatePassword(password: String): String? =
        evaluatePassword(password).firstErrorOrNull()
}