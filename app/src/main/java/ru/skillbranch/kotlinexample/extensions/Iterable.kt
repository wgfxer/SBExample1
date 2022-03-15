package ru.skillbranch.kotlinexample.extensions

/**
 * @author Valeriy Minnulin
 */


fun <T> List<T>.dropLastUntil(predicate: (T) -> Boolean): List<T> {
    if (this.isEmpty()) return this
    var currentIndex = this.lastIndex
    while (!predicate(this[currentIndex])) {
        currentIndex--
    }
    if (currentIndex < 0) return emptyList()
    return this.subList(0, currentIndex)

}