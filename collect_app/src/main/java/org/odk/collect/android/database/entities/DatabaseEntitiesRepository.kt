package org.odk.collect.android.database.entities

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.provider.BaseColumns._ID
import org.odk.collect.db.sqlite.CursorExt.first
import org.odk.collect.db.sqlite.CursorExt.foldAndClose
import org.odk.collect.db.sqlite.CursorExt.getInt
import org.odk.collect.db.sqlite.CursorExt.getString
import org.odk.collect.db.sqlite.CursorExt.getStringOrNull
import org.odk.collect.db.sqlite.CursorExt.rowToMap
import org.odk.collect.db.sqlite.DatabaseMigrator
import org.odk.collect.db.sqlite.RowNumbers.invalidateRowNumbers
import org.odk.collect.db.sqlite.RowNumbers.rawQueryWithRowNumber
import org.odk.collect.db.sqlite.SQLiteColumns.ROW_NUMBER
import org.odk.collect.db.sqlite.SQLiteDatabaseExt.delete
import org.odk.collect.db.sqlite.SQLiteDatabaseExt.doesColumnExist
import org.odk.collect.db.sqlite.SQLiteDatabaseExt.getColumnNames
import org.odk.collect.db.sqlite.SQLiteDatabaseExt.query
import org.odk.collect.db.sqlite.SynchronizedDatabaseConnection
import org.odk.collect.db.sqlite.toSql
import org.odk.collect.entities.javarosa.parse.EntitySchema
import org.odk.collect.entities.storage.EntitiesRepository
import org.odk.collect.entities.storage.Entity
import org.odk.collect.entities.storage.QueryException
import org.odk.collect.shared.Query
import org.odk.collect.shared.mapColumns

private object ListsTable {
    const val TABLE_NAME = "lists"
    const val COLUMN_NAME = "name"
    const val COLUMN_HASH = "hash"
}

private object EntitiesTable {
    const val COLUMN_ID = "id"
    const val COLUMN_LABEL = "label"
    const val COLUMN_VERSION = "version"
    const val COLUMN_TRUNK_VERSION = "trunk_version"
    const val COLUMN_BRANCH_ID = "branch_id"
    const val COLUMN_STATE = "state"
    const val COLUMN_PROPERTY_PREFIX = "p_"

    fun getPropertyColumn(property: String) = "$COLUMN_PROPERTY_PREFIX$property"
}

class DatabaseEntitiesRepository(context: Context, dbPath: String) : EntitiesRepository {

    private val databaseConnection = SynchronizedDatabaseConnection(
        context,
        dbPath,
        "entities.db",
        EntitiesDatabaseMigrator(),
        DATABASE_VERSION
    )

    override fun save(list: String, vararg entities: Entity) {
        if (entities.isEmpty()) {
            return
        }

        val listExists = listExists(list)
        if (!listExists) {
            createList(list)
        }

        updatePropertyColumns(list, entities.first())

        databaseConnection.transaction {
            entities.forEach { entity ->
                val existing = if (listExists) {
                    query(
                        quote(list),
                        "${EntitiesTable.COLUMN_ID} = ?",
                        arrayOf(entity.id)
                    ).first { mapCursorRowToEntity(it, 0) }
                } else {
                    null
                }

                if (existing != null) {
                    val state = if (existing.state == Entity.State.OFFLINE) {
                        entity.state
                    } else {
                        Entity.State.ONLINE
                    }

                    val contentValues = ContentValues().also {
                        it.put(EntitiesTable.COLUMN_ID, entity.id)
                        it.put(EntitiesTable.COLUMN_LABEL, entity.label ?: existing.label)
                        it.put(EntitiesTable.COLUMN_VERSION, entity.version)
                        it.put(EntitiesTable.COLUMN_TRUNK_VERSION, entity.trunkVersion)
                        it.put(EntitiesTable.COLUMN_BRANCH_ID, entity.branchId)
                        it.put(EntitiesTable.COLUMN_STATE, convertStateToInt(state))

                        addPropertiesToContentValues(it, entity)
                    }

                    update(
                        quote(list),
                        contentValues,
                        "${EntitiesTable.COLUMN_ID} = ?",
                        arrayOf(entity.id)
                    )
                } else {
                    val contentValues = ContentValues().also {
                        it.put(EntitiesTable.COLUMN_ID, entity.id)
                        it.put(EntitiesTable.COLUMN_LABEL, entity.label)
                        it.put(EntitiesTable.COLUMN_VERSION, entity.version)
                        it.put(EntitiesTable.COLUMN_TRUNK_VERSION, entity.trunkVersion)
                        it.put(EntitiesTable.COLUMN_BRANCH_ID, entity.branchId)
                        it.put(EntitiesTable.COLUMN_STATE, convertStateToInt(entity.state))

                        addPropertiesToContentValues(it, entity)
                    }

                    insertOrThrow(
                        quote(list),
                        null,
                        contentValues
                    )
                }
            }
        }

        invalidateRowNumbers()
    }

    override fun getLists(): Set<String> {
        return databaseConnection.withConnection {
            getListsFromDB(readableDatabase)
        }
    }

    override fun updateListHash(list: String, hash: String) {
        val contentValues = ContentValues().also {
            it.put(ListsTable.COLUMN_HASH, hash)
        }

        databaseConnection.withConnection {
            writableDatabase.update(
                ListsTable.TABLE_NAME,
                contentValues,
                "${ListsTable.COLUMN_NAME} = ?",
                arrayOf(list)
            )
        }
    }

    override fun getListHash(list: String): String? {
        return databaseConnection.withConnection {
            readableDatabase
                .query(ListsTable.TABLE_NAME, "${ListsTable.COLUMN_NAME} = ?", arrayOf(list))
                .first { it.getStringOrNull(ListsTable.COLUMN_HASH) }
        }
    }

    override fun getCount(list: String): Int {
        if (!listExists(list)) {
            return 0
        }

        return databaseConnection.withConnection {
            readableDatabase.rawQuery(
                """
                SELECT COUNT(*)
                FROM "$list"
                """.trimIndent(),
                null
            ).first {
                it.getInt(0)
            }!!
        }
    }

    override fun addList(list: String) {
        if (!listExists(list)) {
            createList(list)
            invalidateRowNumbers()
        }
    }

    override fun delete(id: String) {
        databaseConnection.withConnection {
            getLists().forEach {
                writableDatabase.delete(
                    quote(it),
                    "${EntitiesTable.COLUMN_ID} = ?",
                    arrayOf(id)
                )
            }
        }

        invalidateRowNumbers()
    }

    override fun query(list: String, query: Query?): List<Entity.Saved> {
        if (!listExists(list)) {
            return emptyList()
        }

        return queryWithAttachedRowNumber(list, query?.mapColumns { columnName ->
            when (columnName) {
                EntitySchema.ID -> EntitiesTable.COLUMN_ID
                EntitySchema.LABEL -> EntitiesTable.COLUMN_LABEL
                EntitySchema.VERSION -> EntitiesTable.COLUMN_VERSION
                else -> EntitiesTable.getPropertyColumn(columnName)
            }
        })
    }

    override fun getById(list: String, id: String): Entity.Saved? {
        if (!listExists(list)) {
            return null
        }

        return queryWithAttachedRowNumber(list, Query.Eq(EntitiesTable.COLUMN_ID, id)).firstOrNull()
    }

    override fun getAllByProperty(
        list: String,
        property: String,
        value: String
    ): List<Entity.Saved> {
        if (!listExists(list)) {
            return emptyList()
        }

        val propertyExists = databaseConnection.withConnection {
            readableDatabase.doesColumnExist(quote(list), EntitiesTable.getPropertyColumn(property))
        }

        return if (propertyExists) {
            queryWithAttachedRowNumber(
                list,
                Query.Eq(EntitiesTable.getPropertyColumn(property), value)
            )
        } else if (value == "") {
            queryWithAttachedRowNumber(list, null)
        } else {
            emptyList()
        }
    }

    override fun getByIndex(list: String, index: Int): Entity.Saved? {
        if (!listExists(list)) {
            return null
        }

        val query = Query.Eq(ROW_NUMBER, (index + 1).toString())
        return queryWithAttachedRowNumber(list, query).firstOrNull()
    }

    private fun queryWithAttachedRowNumber(list: String, query: Query?): List<Entity.Saved> {
        try {
            return if (query == null) {
                databaseConnection.rawQueryWithRowNumber(list)
            } else {
                val sqlQuery = query.toSql()
                databaseConnection.rawQueryWithRowNumber(list, sqlQuery.selection, sqlQuery.selectionArgs)
            }.foldAndClose {
                mapCursorRowToEntity(it, it.getInt(ROW_NUMBER))
            }
        } catch (e: SQLiteException) {
            throw QueryException(e.message)
        }
    }

    private fun invalidateRowNumbers() {
        getLists().forEach {
            databaseConnection.invalidateRowNumbers(it)
        }
    }

    private fun listExists(list: String): Boolean {
        return databaseConnection.withConnection {
            readableDatabase
                .query(
                    ListsTable.TABLE_NAME,
                    selection = "${ListsTable.COLUMN_NAME} = ?",
                    selectionArgs = arrayOf(list)
                ).use { it.count } > 0
        }
    }

    private fun createList(list: String) {
        databaseConnection.resetTransaction {
            val contentValues = ContentValues()
            contentValues.put(ListsTable.COLUMN_NAME, list)
            insertOrThrow(
                ListsTable.TABLE_NAME,
                null,
                contentValues
            )

            execSQL(
                """
                CREATE TABLE IF NOT EXISTS "$list" (
                    $_ID integer PRIMARY KEY,
                    ${EntitiesTable.COLUMN_ID} text,
                    ${EntitiesTable.COLUMN_LABEL} text,
                    ${EntitiesTable.COLUMN_VERSION} integer,
                    ${EntitiesTable.COLUMN_TRUNK_VERSION} integer,
                    ${EntitiesTable.COLUMN_BRANCH_ID} text,
                    ${EntitiesTable.COLUMN_STATE} integer NOT NULL
                );
                """.trimIndent()
            )

            execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS "${list}_unique_id_index" ON "$list" (${EntitiesTable.COLUMN_ID});
                """.trimIndent()
            )
        }
    }

    private fun updatePropertyColumns(list: String, entity: Entity) {
        val columnNames = databaseConnection.withConnection {
            readableDatabase.getColumnNames(quote(list))
        }

        val missingColumns = entity.properties
            .map { EntitiesTable.getPropertyColumn(it.first) }
            .distinctBy { it.lowercase() }
            .filterNot { columnName -> columnNames.any { it.equals(columnName, ignoreCase = true) } }

        if (missingColumns.isNotEmpty()) {
            databaseConnection.resetTransaction {
                missingColumns.forEach {
                    execSQL(
                        """
                        ALTER TABLE "$list" ADD "$it" text NOT NULL DEFAULT "";
                        """.trimIndent()
                    )
                }
            }
        }
    }

    private fun addPropertiesToContentValues(contentValues: ContentValues, entity: Entity) {
        entity.properties.forEach { (name, value) ->
            contentValues.put(quote(EntitiesTable.getPropertyColumn(name)), value)
        }
    }

    private fun mapCursorRowToEntity(
        cursor: Cursor,
        rowId: Int
    ): Entity.Saved {
        val map = cursor.rowToMap()

        val propertyColumns = map.keys.filter {
            it.startsWith(EntitiesTable.COLUMN_PROPERTY_PREFIX)
        }

        val properties =
            propertyColumns.fold(emptyList<Pair<String, String>>()) { accum, property ->
                accum + Pair(
                    property.removePrefix(EntitiesTable.COLUMN_PROPERTY_PREFIX),
                    map[property] ?: ""
                )
            }

        val state = if (map[EntitiesTable.COLUMN_STATE]!!.toInt() == 0) {
            Entity.State.OFFLINE
        } else {
            Entity.State.ONLINE
        }

        return Entity.Saved(
            map[EntitiesTable.COLUMN_ID]!!,
            map[EntitiesTable.COLUMN_LABEL],
            map[EntitiesTable.COLUMN_VERSION]!!.toInt(),
            properties,
            state,
            rowId - 1,
            map[EntitiesTable.COLUMN_TRUNK_VERSION]?.toInt(),
            map[EntitiesTable.COLUMN_BRANCH_ID]!!
        )
    }

    /**
     * Store state as an Int rather than a string to avoid increasing the storage needed for
     * entities.
     */
    private fun convertStateToInt(state: Entity.State): Int {
        return when (state) {
            Entity.State.OFFLINE -> 0
            Entity.State.ONLINE -> 1
        }
    }

    private fun quote(text: String) = "\"$text\""

    companion object {
        private const val DATABASE_VERSION = 2
    }
}

private class EntitiesDatabaseMigrator :
    DatabaseMigrator {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ${ListsTable.TABLE_NAME} (
                    $_ID integer PRIMARY KEY, 
                    ${ListsTable.COLUMN_NAME} text NOT NULL,
                    ${ListsTable.COLUMN_HASH} text
            );
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int) {
        dropAllTablesFromDB(db)
    }
}

private fun dropAllTablesFromDB(db: SQLiteDatabase) {
    getListsFromDB(db).forEach {
        db.delete(it)
    }

    db.delete(ListsTable.TABLE_NAME)
}

private fun getListsFromDB(db: SQLiteDatabase): Set<String> {
    return db
        .query(ListsTable.TABLE_NAME)
        .foldAndClose(emptySet()) { set, cursor -> set + cursor.getString(ListsTable.COLUMN_NAME) }
}
