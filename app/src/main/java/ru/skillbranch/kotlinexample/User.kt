package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
        private val firstName: String,
        private val lastName: String?,
        email: String? = null,
        rawPhone: String? = null,
        meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
                .joinToString(" ")
                .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
                .map { it.first().toUpperCase() }
                .joinToString(" ")

    private var phone: String? = null
        set(value) {
            field = value.clearPhone()
        }

    private var _login: String? = null
    var login: String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    // For email
    constructor(
            firstName: String,
            lastName: String?,
            email: String,
            password: String?,
            passData: Pair<String, String>? = null
    ) : this(firstName, lastName, email = email, meta = if (passData == null) mapOf("auth" to "password") else csvMap) {
        println("Secondary email constructor")
        if (passData == null) {
            passwordHash = encrypt(password!!)
        } else {
            salt = passData.first
            passwordHash = passData.second
        }
    }

    // For phone
    constructor(
            firstName: String,
            lastName: String?,
            rawPhone: String,
            passData: Pair<String, String>? = null
    ) : this(firstName, lastName, rawPhone = rawPhone, meta = if (passData == null) mapOf("auth" to "sms") else csvMap) {
        println("Secondary phone constructor")
        if (passData == null) {
            val code = generateAccessCode()
            passwordHash = encrypt(code)
            println("Phone passwordHash is $passwordHash")
            accessCode = code
            sendAccessCodeToUser(rawPhone, code)
        } else {
            salt = passData.first
            passwordHash = passData.second
        }
    }
    
    init {
        println("First init block, primary constructor was called")

        check(firstName.isNotBlank()) { "FirstName must not be blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) { "Email or phone must not be null or blank" }

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: ${phone}
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash.also {
        println("Checking passwordHash is $passwordHash")
    }

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) {
            passwordHash = encrypt(newPass)
            if (!accessCode.isNullOrEmpty()) accessCode = newPass
            println("Password $oldPass has been changed on new password $newPass")
        } else throw IllegalArgumentException("The entered password does not match the current password")
    }

    private fun encrypt(password: String): String {
        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        }
        println("Salt while encrypt: $salt")
        return salt.plus(password).md5()
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray()) // 16 byte
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"

        return StringBuilder().apply {
            repeat(6) {
                (possible.indices).random().also { index ->
                    append(possible[index])
                }
            }
        }.toString()
    }

    fun sendAccessCodeToUser(phone: String, code: String) {
        println("..... sending access code: $code on $phone")
    }

    companion object Factory {
        val csvMap = mapOf("src" to "csv")

        fun makeUser(
                fullName: String,
                email: String? = null,
                password: String? = null,
                phone: String? = null,
                passData: Pair<String, String>? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone, passData)
                !email.isNullOrBlank() -> User(
                    firstName,
                    lastName,
                    email,
                    password,
                    passData
                )
                else -> throw IllegalArgumentException("Email or phone must not be null or blank")
            }
        }

        private fun String.fullNameToPair(): Pair<String, String?> =
                this.split(" ")
                        .filter { it.isNotBlank() }
                        .run {
                            when (size) {
                                1 -> first() to null
                                2 -> first() to last()
                                else -> throw IllegalArgumentException(
                                        "FullName must contain only first name and last name, current split " +
                                                "result: ${this@fullNameToPair}"
                                )
                            }
                        }
    }

}
