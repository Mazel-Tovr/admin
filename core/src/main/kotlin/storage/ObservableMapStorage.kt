package com.epam.drill.admin.storage

import kotlinx.atomicfu.*
import kotlinx.collections.immutable.*

class ObservableMapStorage<K, V>(map: Map<K, V> = emptyMap()) {

    val onUpdate: MutableList<suspend (Map<K, V>) -> Unit> = mutableListOf()
    val onAdd: MutableList<suspend (K, V) -> Unit> = mutableListOf()
    val onRemove: MutableList<suspend (K) -> Unit> = mutableListOf()

    private val _targetMap = atomic(map.toPersistentMap())

    val targetMap: PersistentMap<K, V> get() = _targetMap.value

    val keys: Set<K>
        get() = targetMap.keys

    val values: Collection<V>
        get() = targetMap.values

    fun init(map: Map<K, V>) {
        _targetMap.value = map.toPersistentHashMap()
    }

    operator fun get(key: K): V? = targetMap[key]

    suspend fun put(key: K, value: V): V? {
        val replaced: V? = _targetMap.getAndUpdate {
            it.put(key, value)
        }[key]
        handleAdd(key, value)
        handleUpdate(targetMap)
        return replaced
    }

    suspend fun remove(key: K): V? {
        val removed = _targetMap.getAndUpdate { it.remove(key) }[key]
        handleRemove(key)
        return removed
    }

    private suspend fun handleAdd(key: K, value: V) {
        onAdd.forEach { it(key, value) }
    }

    private suspend fun handleUpdate(map: Map<K, V>) {
        onUpdate.forEach { it(map) }
    }

    suspend fun handleRemove(key: K) {
        onRemove.forEach { it(key) }
    }

    suspend fun update() {
        handleUpdate(targetMap)

    }

    suspend fun singleUpdate(key: K) {
        targetMap[key]?.let { value ->
            handleAdd(key, value)
        }
    }
}
