package com.offlinebrain.ecs

class ComponentMapper<C : Component> internal constructor(
    private val entityManager: ECSManager
) {
    private val components = mutableMapOf<Entity, C>()
    private val addComponentQueue = mutableMapOf<Entity, C>()
    private val removeComponentQueue = LinkedHashSet<Entity>()

    operator fun get(entity: Entity): C? = components[entity]
    operator fun set(entity: Entity, component: C) {
        if (entityManager.processing) {
            addComponentQueue[entity] = component
        } else {
            components[entity] = component
        }
    }

    fun remove(entity: Entity) {
        if (entityManager.processing) {
            removeComponentQueue.add(entity)
        } else {
            components.remove(entity)
        }
    }

    fun getOr(entity: Entity, c: C): C = components.getOrElse(entity) { c }
    fun destroy(entity: Entity) {
        components.remove(entity)
    }

    fun destroyAll(entities: Iterable<Entity>) {
        entities.forEach { components.remove(it) }
    }

    operator fun contains(entity: Entity) = components.containsKey(entity)

    fun flush(notify: Boolean = false) {
        removeComponentQueue.forEach {
            components.remove(it)
            if (notify) entityManager.notify(it)
        }
        addComponentQueue.forEach {
            components[it.key] = it.value
            if (notify) entityManager.notify(it.key)
        }

        addComponentQueue.clear()
        removeComponentQueue.clear()
    }
}