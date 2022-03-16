package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
            fullName: String,
            email: String,
            password: String
    ): User {
        if (map.contains(email.lowercase())) throw IllegalArgumentException("A user with this email already exists")
        return User.makeUser(fullName, email = email, password = password)
            .also { user -> map[user.login] = user }
    }

    fun loginUser(login: String, password: String): String? {
        val userByEmail = map[login.trim()]
        val userByPhone = map[login.clearPhone()]
        val user = userByEmail ?: userByPhone ?: return null
        return if (user.checkPassword(password)) user.userInfo else null
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }

    fun requestAccessCode(login: String) {
        val user = map[login.clearPhone()] ?: return
        val newAccessCode = user.generateAccessCode()
        user.accessCode?.let { oldAccessCode -> user.changePassword(oldAccessCode, newAccessCode) }
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User {
        if (map.contains(rawPhone.clearPhone())) throw IllegalArgumentException("A user with this phone already exists")
        if (!rawPhone.rawPhoneValid()) throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        return User.makeUser(fullName = fullName, phone = rawPhone)
            .also { user -> map[user.login] = user}
    }

    fun importUsers(list: List<String>) = list.map { convertToUser(it) }

    private fun convertToUser(stringUser: String): User {
        val fields: List<String> = stringUser.split(';')
        val userFullName = fields[0]
        val userEmail = fields[1].orNull()
        val userPasswordData = fields[2].split(':').let { it[0] to it[1] }
        val userPhone = fields[3].orNull()
        return User.makeUser(
            userFullName,
            email = userEmail,
            password = null,
            phone = userPhone,
            passData = userPasswordData
        ).also { user -> map[user.login] = user }
    }

    private fun String?.orNull() = if (this.isNullOrEmpty()) null else this
}

fun String?.clearPhone() = this?.replace("""[^+\d]""".toRegex(), "")

fun String?.rawPhoneValid(): Boolean {
    if (this == null) return false
    return this.matches("""[^A-Za-z]*""".toRegex()) && this.clearPhone()?.matches("""\+\d{11}""".toRegex()).orFalse()
}

fun Boolean?.orFalse() = this ?: false