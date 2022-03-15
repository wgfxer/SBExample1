package ru.skillbranch.kotlinexample

/**
 * @author Valeriy Minnulin
 */
fun String?.clearPhone() = this?.replace("""[^+\d]""".toRegex(), "")

fun String?.rawPhoneValid(): Boolean {
    if (this == null) return false
    return this.matches("""[^A-Za-z]*""".toRegex()) && this.clearPhone()?.matches("""\+\d{11}""".toRegex()).orFalse()
}

fun Boolean?.orFalse() = this ?: false