package com.offlinebrain.ecs

import kotlin.reflect.KClass

class QueryContainer {
    private val componentQueries = HashMap<KClass<out Component>, LinkedHashSet<Query>>()
    private val queries: MutableSet<Query> = LinkedHashSet()

    fun add(query: Query) {
        queries.add(query)
        for (component in query.include) {
            componentQueries.getOrPut(component) { LinkedHashSet() }.add(query)
        }
        for (component in query.exclude) {
            componentQueries.getOrPut(component) { LinkedHashSet() }.add(query)
        }
    }

    fun remove(query: Query) {
        queries.remove(query)
        for (component in query.include) {
            componentQueries.getOrPut(component) { LinkedHashSet() }.remove(query)
        }
        for (component in query.exclude) {
            componentQueries.getOrPut(component) { LinkedHashSet() }.remove(query)
        }
    }

    operator fun contains(query: Query): Boolean = query in queries

    fun offer(entity: Entity) {
        for (query in queries) {
            query.offer(entity, removeIfApplicable = true)
        }
    }

    fun offer(entity: Entity, vararg components: KClass<out Component>) {
        components.forEach { component ->
            componentQueries.getOrPut(component) { LinkedHashSet() }
                .forEach { it.offer(entity, removeIfApplicable = true) }
        }
    }

    fun forget(entity: Entity) {
        for (query in queries) {
            query.forget(entity)
        }
    }
}