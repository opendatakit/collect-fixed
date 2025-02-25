package org.odk.collect.entities.storage

import org.odk.collect.shared.Query

interface EntitiesRepository {
    fun save(list: String, vararg entities: Entity)
    fun getLists(): Set<String>
    fun getCount(list: String): Int
    fun addList(list: String)
    fun delete(list: String, id: String)
    fun query(list: String, query: Query? = null): List<Entity.Saved>
    fun getByIndex(list: String, index: Int): Entity.Saved?
    fun updateListHash(list: String, hash: String)
    fun getListHash(list: String): String?
}
