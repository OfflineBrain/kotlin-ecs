package com.offlinebrain.ecs

interface System {
    fun process(delta: Long)
}

abstract class BaseSystem(
    private val query: Query
) : System {
    protected abstract fun processEntities(delta: Long, entities: Set<Entity>)

    override fun process(delta: Long) {
        processEntities(delta, query.entities)
    }
}