package org.odk.collect.entities

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.blankOrNullString
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Test
import org.odk.collect.entities.javarosa.finalization.EntitiesExtra
import org.odk.collect.entities.javarosa.finalization.FormEntity
import org.odk.collect.entities.javarosa.parse.EntitySchema
import org.odk.collect.entities.javarosa.spec.EntityAction
import org.odk.collect.entities.server.EntitySource
import org.odk.collect.entities.storage.EntitiesRepository
import org.odk.collect.entities.storage.Entity
import org.odk.collect.entities.storage.InMemEntitiesRepository
import org.odk.collect.shared.Query
import org.odk.collect.shared.TempFiles
import java.io.File
import java.util.UUID

class LocalEntityUseCasesTest {

    private val entitiesRepository = InMemEntitiesRepository()
    private val entitySource = FakeEntitySource()

    @Test
    fun `updateLocalEntitiesFromForm saves a new entity on create`() {
        entitiesRepository.addList("things")

        val formEntity =
            FormEntity(EntityAction.CREATE, "things", "id", "label", listOf("property" to "value"))
        val formEntities = EntitiesExtra(listOf(formEntity))
        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)

        val entities = entitiesRepository.query("things")
        assertThat(entities.size, equalTo(1))
        assertThat(entities[0].id, equalTo(formEntity.id))
        assertThat(entities[0].label, equalTo(formEntity.label))
        assertThat(entities[0].properties, equalTo(formEntity.properties))
        assertThat(entities[0].branchId, not(blankOrNullString()))
    }

    @Test
    fun `updateLocalEntitiesFromForm increments version on update`() {
        entitiesRepository.save(
            "things",
            Entity.New(
                "id",
                "label",
                version = 1
            )
        )

        val formEntity =
            FormEntity(EntityAction.UPDATE, "things", "id", "label", emptyList())
        val formEntities = EntitiesExtra(listOf(formEntity))

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        val entities = entitiesRepository.query("things")
        assertThat(entities.size, equalTo(1))
        assertThat(entities[0].version, equalTo(2))
    }

    @Test
    fun `updateLocalEntitiesFromForm updates properties on update`() {
        entitiesRepository.save(
            "things",
            Entity.New(
                "id",
                "label",
                version = 1,
                properties = listOf("prop" to "value")
            )
        )

        val formEntity =
            FormEntity(EntityAction.UPDATE, "things", "id", "label", listOf("prop" to "value 2"))
        val formEntities = EntitiesExtra(listOf(formEntity))

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        val entities = entitiesRepository.query("things")
        assertThat(entities.size, equalTo(1))
        assertThat(entities[0].properties.size, equalTo(1))
        assertThat(entities[0].properties[0], equalTo("prop" to "value 2"))
    }

    @Test
    fun `updateLocalEntitiesFromForm updates properties and does not change label on update if label is null`() {
        entitiesRepository.save(
            "things",
            Entity.New(
                "id",
                "label",
                version = 1,
                properties = listOf("prop" to "value")
            )
        )

        val formEntity =
            FormEntity(EntityAction.UPDATE, "things", "id", null, listOf("prop" to "value 2"))
        val formEntities = EntitiesExtra(listOf(formEntity))

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        val entities = entitiesRepository.query("things")
        assertThat(entities.size, equalTo(1))
        assertThat(entities[0].label, equalTo("label"))
        assertThat(entities[0].properties.size, equalTo(1))
        assertThat(entities[0].properties[0], equalTo("prop" to "value 2"))
    }

    @Test
    fun `updateLocalEntitiesFromForm updates properties and does not change label on update if label is blank`() {
        entitiesRepository.save(
            "things",
            Entity.New(
                "id",
                "label",
                version = 1,
                properties = listOf("prop" to "value")
            )
        )

        val formEntity =
            FormEntity(EntityAction.UPDATE, "things", "id", " ", listOf("prop" to "value 2"))
        val formEntities = EntitiesExtra(listOf(formEntity))

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        val entities = entitiesRepository.query("things")
        assertThat(entities.size, equalTo(1))
        assertThat(entities[0].label, equalTo("label"))
        assertThat(entities[0].properties.size, equalTo(1))
        assertThat(entities[0].properties[0], equalTo("prop" to "value 2"))
    }

    @Test
    fun `updateLocalEntitiesFromForm does not override trunk version or branchId on update`() {
        entitiesRepository.save(
            "things",
            Entity.New(
                "id",
                "label",
                version = 1,
                trunkVersion = 1,
                branchId = "branch-1"
            )
        )

        val formEntity =
            FormEntity(EntityAction.UPDATE, "things", "id", "label", emptyList())
        val formEntities = EntitiesExtra(listOf(formEntity))

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        val entities = entitiesRepository.query("things")
        assertThat(entities.size, equalTo(1))
        assertThat(entities[0].trunkVersion, equalTo(1))
        assertThat(entities[0].branchId, equalTo("branch-1"))
    }

    @Test
    fun `updateLocalEntitiesFromForm does not save updated entity that doesn't already exist`() {
        val formEntity =
            FormEntity(EntityAction.UPDATE, "things", "1", "1", emptyList())
        val formEntities = EntitiesExtra(listOf(formEntity))
        entitiesRepository.addList("things")

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        assertThat(entitiesRepository.query("things").size, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromForm does not save entity that doesn't have an ID`() {
        val formEntity =
            FormEntity(EntityAction.CREATE, "things", null, "1", emptyList())
        val formEntities = EntitiesExtra(listOf(formEntity))
        entitiesRepository.addList("things")

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        assertThat(entitiesRepository.query("things").size, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromForm does not create entity that doesn't have a label`() {
        val formEntity =
            FormEntity(EntityAction.CREATE, "things", "1", null, emptyList())
        val formEntities = EntitiesExtra(listOf(formEntity))
        entitiesRepository.addList("things")

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        assertThat(entitiesRepository.query("things").size, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromForm does not create entity that has a blank label`() {
        val formEntity =
            FormEntity(EntityAction.CREATE, "things", "1", " ", emptyList())
        val formEntities = EntitiesExtra(listOf(formEntity))
        entitiesRepository.addList("things")

        LocalEntityUseCases.updateLocalEntitiesFromForm(formEntities, entitiesRepository)
        assertThat(entitiesRepository.query("things").size, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromServer saves entity from server`() {
        val csv = createEntityList(
            Entity.New(
                "noah",
                "Noah",
                2,
                properties = listOf("property" to "value")
            )
        )

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].label, equalTo("Noah"))
        assertThat(songs[0].version, equalTo(2))
        assertThat(songs[0].properties, equalTo(listOf("property" to "value")))
        assertThat(songs[0].state, equalTo(Entity.State.ONLINE))
        assertThat(songs[0].trunkVersion, equalTo(2))
        assertThat(songs[0].branchId, not(blankOrNullString()))
    }

    @Test
    fun `updateLocalEntitiesFromServer overrides offline version if the online version is newer`() {
        val offline = Entity.New("noah", "Noa", 1, trunkVersion = 1)
        entitiesRepository.save("songs", offline)
        val csv = createEntityList(Entity.New("noah", "Noah", 2))

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].label, equalTo("Noah"))
        assertThat(songs[0].version, equalTo(2))
        assertThat(songs[0].state, equalTo(Entity.State.ONLINE))
        assertThat(songs[0].trunkVersion, equalTo(2))
        assertThat(songs[0].branchId, not(blankOrNullString()))
        assertThat(songs[0].branchId, not(equalTo(offline.branchId)))
    }

    @Test
    fun `updateLocalEntitiesFromServer overrides offline version if the online version is newer and the offline version is dirty`() {
        val offline = Entity.New("noah", "Noa", 1, trunkVersion = null)
        entitiesRepository.save("songs", offline)
        val csv = createEntityList(Entity.New("noah", "Noah", 2))

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].label, equalTo("Noah"))
        assertThat(songs[0].version, equalTo(2))
        assertThat(songs[0].state, equalTo(Entity.State.ONLINE))
        assertThat(songs[0].trunkVersion, equalTo(2))
        assertThat(songs[0].branchId, not(blankOrNullString()))
        assertThat(songs[0].branchId, not(equalTo(offline.branchId)))
    }

    @Test
    fun `updateLocalEntitiesFromServer updates state if the online version is older`() {
        val offline = Entity.New("noah", "Noah", 2)
        entitiesRepository.save("songs", offline)
        val csv = createEntityList(Entity.New("noah", "Noa", 1))

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].label, equalTo("Noah"))
        assertThat(songs[0].version, equalTo(2))
        assertThat(songs[0].state, equalTo(Entity.State.ONLINE))
        assertThat(songs[0].trunkVersion, equalTo(null))
        assertThat(songs[0].branchId, equalTo(offline.branchId))
    }

    @Test
    fun `updateLocalEntitiesFromServer does not write to repository if online and local are exactly the same`() {
        val entitiesRepository = MeasurableEntitiesRepository(entitiesRepository)

        val local = Entity.New("noah", "Noah", 2, properties = listOf("length" to "4:33"))
        val csv1 = createEntityList(local)
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv1,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        assertThat(entitiesRepository.savedEntities, equalTo(1))

        val csv2 = createEntityList(local, Entity.New("perception", "Perception"))
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv2,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        assertThat(entitiesRepository.savedEntities, equalTo(2))
    }

    @Test
    fun `updateLocalEntitiesFromServer updates trunkVersion, branchId and state if the online version catches up to an offline branch`() {
        val offline = Entity.New("noah", "Noah", 2)
        entitiesRepository.save("songs", offline)

        val csv1 = createEntityList(Entity.New("noah", "Noah", 2))
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv1,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )

        val onlineBranched = Entity.New("noah", "Noah", 3)
        entitiesRepository.save("songs", onlineBranched)
        val csv2 = createEntityList(Entity.New("noah", "Noah", 3))
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv2,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )

        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].version, equalTo(3))
        assertThat(songs[0].state, equalTo(Entity.State.ONLINE))
        assertThat(songs[0].trunkVersion, equalTo(3))
        assertThat(songs[0].branchId, not(blankOrNullString()))
        assertThat(songs[0].branchId, not(equalTo(onlineBranched.branchId)))
    }

    @Test
    fun `updateLocalEntitiesFromServer overrides offline version if the online version is the same`() {
        val offline = Entity.New("noah", "Noah", 2)
        entitiesRepository.save("songs", offline)
        val csv = createEntityList(
            Entity.New(
                "noah",
                "Noa",
                2,
                properties = listOf("length" to "4:33")
            )
        )

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].label, equalTo("Noa"))
        assertThat(songs[0].properties, containsInAnyOrder("length" to "4:33"))
        assertThat(songs[0].version, equalTo(2))
        assertThat(songs[0].state, equalTo(Entity.State.ONLINE))
        assertThat(songs[0].trunkVersion, equalTo(2))
        assertThat(songs[0].branchId, not(blankOrNullString()))
        assertThat(songs[0].branchId, not(equalTo(offline.branchId)))
    }

    @Test
    fun `updateLocalEntitiesFromServer ignores properties not in offline version from older online version`() {
        entitiesRepository.save("songs", Entity.New("noah", "Noah", 3))
        val csv =
            createEntityList(Entity.New("noah", "Noah", 2, listOf(Pair("length", "6:38"))))

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].properties, equalTo(emptyList()))
    }

    @Test
    fun `updateLocalEntitiesFromServer overrides properties in offline version from newer list version`() {
        entitiesRepository.save(
            "songs",
            Entity.New("noah", "Noah", 1, listOf(Pair("length", "6:38")))
        )
        val csv =
            createEntityList(Entity.New("noah", "Noah", 2, listOf(Pair("length", "4:58"))))

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].version, equalTo(2))
        assertThat(songs[0].properties, equalTo(listOf(Pair("length", "4:58"))))
    }

    @Test
    fun `updateLocalEntitiesFromServer does nothing if version does not exist in online entities`() {
        val csv =
            createCsv(
                listOf("name", "label"),
                listOf("grisaille", "Grisaille")
            )

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        assertThat(entitiesRepository.getLists().size, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromServer does nothing if name does not exist in online entities`() {
        val csv =
            createCsv(
                listOf("label", "__version"),
                listOf("Grisaille", "2")
            )

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        assertThat(entitiesRepository.getLists().size, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromServer does nothing if label does not exist in online entities`() {
        val csv =
            createCsv(
                listOf("name", "__version"),
                listOf("grisaille", "2")
            )

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        assertThat(entitiesRepository.getLists().size, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromServer adds online entity when its label is blank`() {
        val csv = createEntityList(Entity.New("cathedrals", label = ""))

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].label, equalTo(""))
    }

    @Test
    fun `updateLocalEntitiesFromServer does nothing if passed a non-CSV file`() {
        val file = TempFiles.createTempFile(".xml")

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            file,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        assertThat(entitiesRepository.getLists().size, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromServer does not remove offline entities that are not in online entities`() {
        entitiesRepository.save("songs", Entity.New("noah", "Noah"))
        val csv = createEntityList(Entity.New("cathedrals", "Cathedrals"))

        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )
        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(2))
    }

    @Test
    fun `updateLocalEntitiesFromServer removes offline entities that are not in online entities but are deleted according to the entity source`() {
        entitiesRepository.save("songs", Entity.New("noah", "Noah"))
        entitiesRepository.save("songs", Entity.New("midnightCity", "Midnight City"))
        entitySource.delete("noah")
        entitySource.delete("midnightCity")

        val csv = createEntityList(Entity.New("cathedrals", "Cathedrals"))
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            entitySource.integrityUrl
        )

        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs.first().id, equalTo("cathedrals"))
    }

    @Test
    fun `updateLocalEntitiesFromServer only checks for deletions with the entity source once`() {
        entitiesRepository.save("songs", Entity.New("noah", "Noah"))
        entitiesRepository.save("songs", Entity.New("midnightCity", "Midnight City"))
        entitySource.delete("noah")
        entitySource.delete("midnightCity")

        val csv = createEntityList()
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            entitySource.integrityUrl
        )

        assertThat(entitySource.accesses, equalTo(1))
    }

    @Test
    fun `updateLocalEntitiesFromServer does not check for deletions with the entity source if it does not need to`() {
        val csv = createEntityList()
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            csv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            entitySource.integrityUrl
        )

        assertThat(entitySource.accesses, equalTo(0))
    }

    @Test
    fun `updateLocalEntitiesFromServer removes offline entity that was in online list, but isn't any longer`() {
        entitiesRepository.save("songs", Entity.New("cathedrals", "Cathedrals"))

        val firstCsv = createEntityList(Entity.New("cathedrals", "Cathedrals"))
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            firstCsv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )

        val secondCsv = createEntityList(Entity.New("noah", "Noah"))
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            secondCsv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )

        val songs = entitiesRepository.query("songs")
        assertThat(songs.size, equalTo(1))
        assertThat(songs[0].id, equalTo("noah"))
    }

    @Test
    fun `updateLocalEntitiesFromServer removes offline entity that was updated in online list, but isn't any longer`() {
        entitiesRepository.save("songs", Entity.New("cathedrals", "Cathedrals", version = 1))

        val firstCsv =
            createEntityList(Entity.New("cathedrals", "Cathedrals (A Song)", version = 2))
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            firstCsv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )

        val secondCsv = createEntityList()
        LocalEntityUseCases.updateLocalEntitiesFromServer(
            "songs",
            secondCsv,
            entitiesRepository,
            entitySource,
            UUID.randomUUID().toString(),
            null
        )

        val songs = entitiesRepository.query("songs")
        assertThat(songs.isEmpty(), equalTo(true))
    }

    private fun createEntityList(vararg entities: Entity): File {
        if (entities.isNotEmpty()) {
            val header = listOf(
                EntitySchema.ID,
                EntitySchema.LABEL,
                EntitySchema.VERSION
            ) + entities[0].properties.map { it.first }

            val rows = entities.map { entity ->
                listOf(
                    entity.id,
                    entity.label,
                    entity.version.toString()
                ) + entity.properties.map { it.second }
            }.toTypedArray()

            return createCsv(header, *rows)
        } else {
            val header = listOf(
                EntitySchema.ID,
                EntitySchema.LABEL,
                EntitySchema.VERSION
            )

            return createCsv(header)
        }
    }

    private fun createCsv(header: List<String>, vararg rows: List<String?>): File {
        val csv = TempFiles.createTempFile()
        csv.writer().use { it ->
            val csvPrinter = CSVPrinter(it, CSVFormat.DEFAULT)
            csvPrinter.printRecord(header)

            rows.forEach {
                csvPrinter.printRecord(it)
            }
        }

        return csv
    }
}

private class MeasurableEntitiesRepository(private val wrapped: EntitiesRepository) :
    EntitiesRepository {

    var accesses: Int = 0
        private set

    var savedEntities: Int = 0
        private set

    override fun save(list: String, vararg entities: Entity) {
        accesses += 1
        savedEntities += entities.size
        wrapped.save(list, *entities)
    }

    override fun getLists(): Set<String> {
        accesses += 1
        return wrapped.getLists()
    }

    override fun getCount(list: String): Int {
        return wrapped.getCount(list)
    }

    override fun addList(list: String) {
        accesses += 1
        wrapped.addList(list)
    }

    override fun delete(list: String, id: String) {
        accesses += 1
        wrapped.delete(list, id)
    }

    override fun query(list: String, query: Query?): List<Entity.Saved> {
        accesses += 1
        return wrapped.query(list, query)
    }

    override fun getByIndex(list: String, index: Int): Entity.Saved? {
        accesses += 1
        return wrapped.getByIndex(list, index)
    }

    override fun updateListHash(list: String, hash: String) {
        accesses += 1
        wrapped.updateListHash(list, hash)
    }

    override fun getListHash(list: String): String? {
        accesses += 1
        return wrapped.getListHash(list)
    }
}

private class FakeEntitySource : EntitySource {

    val integrityUrl = "http://example.com/${UUID.randomUUID()}"
    var accesses: Int = 0
        private set

    private val deleted = mutableListOf<String>()

    override fun isDeleted(integrityUrl: String, ids: List<String>): List<Pair<String, Boolean>> {
        accesses += 1

        if (integrityUrl == this.integrityUrl) {
            return ids.map {
                Pair(it, deleted.contains(it))
            }
        } else {
            throw IllegalArgumentException()
        }
    }

    fun delete(id: String) {
        deleted.add(id)
    }
}
