package io.voodoo.apps.privacy.config

abstract class BaseCmpHelper<T> where T : Enum<T>, T : BaseCmpEnum {
    private val map: MutableMap<String, Boolean> = mutableMapOf()

    fun containsKey(key: String): Boolean {
        return map.containsKey(key)
    }

    fun get(enum: T): Boolean {
        return map[enum.getKey()] ?: false
    }

    fun get(key: String): Boolean {
        return map[key] ?: false
    }

    fun put(key: String, value: Boolean) {
        map[key] = value
    }

    fun getMap(): Map<String, Boolean> {
        return map.filterKeys { it != INITIALIZED }
    }

    fun setInitialized() {
        map[INITIALIZED] = true
    }

    fun isInitialized(): Boolean {
        return map[INITIALIZED] ?: false
    }

    inline fun <reified T : Enum<T>> getEnumFromKey(key: String): T? {
        return enumValues<T>().find { (it as BaseCmpEnum).getKey() == key } // Cast to BaseCmpEnum
    }

    companion object {
        private const val INITIALIZED = "INITIALIZED"
    }
}

interface BaseCmpEnum {
    fun getKey(): String
}
