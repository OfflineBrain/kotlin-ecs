package com.offlinebrain.ecs

import kotlin.reflect.KClass

class ECSManager {
    private var size: Int = 0
    private val entities = LinkedHashSet<Entity>()
    private val systems = mutableSetOf<System>()
    private val componentMappers = HashMap<KClass<out Component>, ComponentMapper<Component>>()
    private val queries = QueryContainer()
    internal var processing = false
        private set

    operator fun <R> invoke(block: ECSManager.() -> R): R = block()

    fun register(vararg system: System) = systems.addAll(system)

    fun <C : Component> register(componentType: KClass<C>) {
        componentMappers[componentType] = ComponentMapper(this)
    }

    inline fun <reified C : Component> register() = register(C::class)

    @Suppress("UNCHECKED_CAST")
    fun <C : Component> mapper(componentType: KClass<C>): ComponentMapper<C>? =
        componentMappers[componentType] as? ComponentMapper<C>

    inline fun <reified C : Component> mapper(): ComponentMapper<C>? = mapper(C::class)

    fun register(vararg query: Query) {
        query.forEach {
            if (!contains(it)) {
                it.apply { init() }
                queries.add(it)
                entities.forEach { entity -> it.offer(entity) }
            }
        }
    }

    fun unregister(query: Query) = queries.remove(query)
    fun contains(query: Query) = query in queries

    fun create(): Entity {
        val entity = ++size
        entities.add(entity)
        queries.offer(entity)
        return entity
    }

    fun create(block: Entity.() -> Unit): Entity {
        val entity = ++size
        entities.add(entity)
        entity.block()
        queries.offer(entity)
        return entity
    }

    fun get(id: Int): Entity? = entities.find { it == id }

    fun notify(entity: Entity, vararg components: KClass<out Component>) {
        queries.offer(entity, *components)
    }

    @Suppress("UNCHECKED_CAST")
    fun <C : Component> Entity.get(type: KClass<C>): C? = componentMappers[type]?.get(this) as C?
    inline fun <reified C : Component> Entity.get(): C? = get(C::class)

    fun Entity.components(): Set<Component> = componentMappers.values.mapNotNull { it[this] }.toSet()

    @Suppress("UNCHECKED_CAST")
    fun <C : Component> Entity.add(type: KClass<C>, component: C) {
        (componentMappers[type] as ComponentMapper<C>?)?.set(this, component)
        queries.offer(this, type)
    }

    inline fun <reified C : Component> Entity.add(component: C) = add(C::class, component)

    @Suppress("UNCHECKED_CAST")
    fun <C : Component> Entity.remove(type: KClass<C>) {
        (componentMappers[type] as ComponentMapper<C>?)?.remove(this)
        queries.offer(this, type)
    }

    inline fun <reified C : Component> Entity.remove() = remove(C::class)

    fun Entity.destroy() {
        componentMappers.values.forEach { it.destroy(this) }
        queries.forget(this)
        entities.remove(this)
    }

    fun destroyAll(entts: Iterable<Entity>) {
        componentMappers.values.forEach { it.destroyAll(entts) }
        entts.forEach {
            queries.forget(it)
        }
        entities.removeAll(entts.toSet())
    }


    fun process(delta: Long) {
        if (processing) return
        processing = true
        systems.forEach { it.process(delta) }
        processing = false

        componentMappers.values.forEach { it.flush(true) }
    }
}

typealias Entity = Int


