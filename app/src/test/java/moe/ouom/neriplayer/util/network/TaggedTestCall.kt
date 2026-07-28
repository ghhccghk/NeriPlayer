package moe.ouom.neriplayer.util.network

import okhttp3.Call
import kotlin.reflect.KClass

abstract class TaggedTestCall : Call {

    // 简单的 tag 存储，key 统一用 KClass
    private val tags = mutableMapOf<KClass<*>, Any>()

    fun <T : Any> setTag(type: KClass<T>, value: T) {
        tags[type] = value
    }

    override fun <T : Any> tag(type: KClass<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return tags[type] as? T
    }

    override fun <T> tag(type: Class<out T>): T? {
        @Suppress("UNCHECKED_CAST")
        return tags[type.kotlin] as? T
    }

    override fun <T : Any> tag(type: KClass<T>, computeIfAbsent: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return tags.getOrPut(type) { computeIfAbsent() } as T
    }

    override fun <T : Any> tag(type: Class<T>, computeIfAbsent: () -> T): T {
        @Suppress("UNCHECKED_CAST")
        return tags.getOrPut(type.kotlin) { computeIfAbsent() } as T
    }
}