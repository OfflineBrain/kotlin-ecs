package com.offlinebrain.ecs

import kotlin.reflect.KClass

interface Query {
    val entities: Set<Entity>
    val include: Set<KClass<out Component>>
        get() = emptySet()
    val exclude: Set<KClass<out Component>>
        get() = emptySet()

    operator fun contains(entity: Entity?): Boolean = entity != null && entities.contains(entity)

    fun offer(entity: Entity, removeIfApplicable: Boolean = false): Boolean
    fun forget(entity: Entity)

    fun ECSManager.init()
}

open class BaseQuery(
    include: Set<KClass<out Component>> = emptySet(),
    exclude: Set<KClass<out Component>> = emptySet()
) : Query {
    protected val _entities = mutableSetOf<Entity>()
    override val entities: Set<Entity> = _entities
    override val include: Set<KClass<out Component>> = include.toSet()
    override val exclude: Set<KClass<out Component>> = exclude.toSet()

    protected val includeMappers = mutableSetOf<ComponentMapper<out Component>>()
    protected val excludeMappers = mutableSetOf<ComponentMapper<out Component>>()

    override fun offer(entity: Entity, removeIfApplicable: Boolean): Boolean {
        if (requiredMatch(entity)) {
            return _entities.add(entity)
        } else if (removeIfApplicable) {
            return _entities.remove(entity)
        }
        return false
    }

    override fun forget(entity: Entity) {
        _entities.remove(entity)
    }

    override fun ECSManager.init() {
        includeMappers.addAll(include.mapNotNull { this.mapper(it) })
        excludeMappers.addAll(exclude.mapNotNull { this.mapper(it) })
    }

    protected fun requiredMatch(entity: Entity) =
        includeMappers.all { entity in it } and excludeMappers.none { entity in it }

}

fun Set<KClass<out Component>>.toInclusiveQuery(): Query =
    BaseQuery(include = this)

fun Set<KClass<out Component>>.toExclusiveQuery(): Query =
    BaseQuery(exclude = this)

interface MapQuery<T : Component> : Query {
    val map: Map<T, Entity>
    operator fun get(component: T): Entity?
    operator fun contains(component: T): Boolean
}

open class BaseMapQuery<T : Component>(
    private val key: KClass<T>,
    include: Set<KClass<out Component>> = setOf(key),
    exclude: Set<KClass<out Component>> = emptySet(),
) : BaseQuery(include, exclude), MapQuery<T> {
    private val _map = mutableMapOf<T, Entity>()
    override val map: Map<T, Entity> = _map
    private var keyMapper: ComponentMapper<T>? = null

    override fun offer(entity: Entity, removeIfApplicable: Boolean): Boolean {
        if (requiredMatch(entity)) {
            _entities.add(entity)
            keyMapper?.let { keyMapper ->
                val key = keyMapper[entity]
                if (key != null) {
                    _map[key] = entity
                }
            }
            return true
        } else if (removeIfApplicable) {
            forget(entity)
            return true
        }
        return false
    }

    override fun forget(entity: Entity) {
        if (entity in _entities) {
            super.forget(entity)
            _map.remove(keyMapper?.get(entity))
        }
    }


    override fun ECSManager.init() {
        includeMappers.addAll(include.mapNotNull { this.mapper(it) })
        excludeMappers.addAll(exclude.mapNotNull { this.mapper(it) })
        keyMapper = this.mapper(key)
    }

    override fun get(component: T): Entity? = _map[component]
    override fun contains(component: T): Boolean = component in _map
}

interface MultimapQuery<T> : Query {
    val map: Map<T, Set<Entity>>
    operator fun get(component: T): Set<Entity>
    operator fun contains(component: T): Boolean
}

open class BaseMultimapQuery<T : Component>(
    private val key: KClass<T>,
    include: Set<KClass<out Component>> = setOf(key),
    exclude: Set<KClass<out Component>> = emptySet(),
) : BaseQuery(include, exclude), MultimapQuery<T> {
    private val _map = mutableMapOf<T, MutableSet<Entity>>()
    override val map: Map<T, Set<Entity>> = _map
    private var keyMapper: ComponentMapper<T>? = null

    override fun offer(entity: Entity, removeIfApplicable: Boolean): Boolean {
        if (requiredMatch(entity)) {
            _entities.add(entity)
            keyMapper?.let { keyMapper ->
                val key = keyMapper[entity]
                if (key != null) {
                    _map.getOrPut(key) { LinkedHashSet() }.add(entity)
                }
            }
            return true
        } else if (removeIfApplicable) {
            forget(entity)
            return true
        }
        return false
    }

    override fun forget(entity: Entity) {
        if (entity in _entities) {
            super.forget(entity)
            keyMapper?.let { keyMapper ->
                val key = keyMapper[entity]
                if (key != null) {
                    _map.getOrPut(key) { LinkedHashSet() }.remove(entity)
                }
            }
        }
    }

    override fun ECSManager.init() {
        includeMappers.addAll(include.mapNotNull { this.mapper(it) })
        excludeMappers.addAll(exclude.mapNotNull { this.mapper(it) })
        keyMapper = this.mapper(key)
    }

    override fun get(component: T): Set<Entity> = _map.getOrPut(component) { LinkedHashSet() }
    override fun contains(component: T): Boolean = component in _map
}
