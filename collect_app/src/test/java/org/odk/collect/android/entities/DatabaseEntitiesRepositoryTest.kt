package org.odk.collect.android.entities

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.odk.collect.android.database.entities.DatabaseEntitiesRepository
import org.odk.collect.android.entities.support.EntitySameAsMatcher.Companion.sameEntityAs
import org.odk.collect.entities.storage.EntitiesRepository
import org.odk.collect.entities.storage.Entity
import org.odk.collect.shared.TempFiles

@RunWith(AndroidJUnit4::class)
class DatabaseEntitiesRepositoryTest : EntitiesRepositoryTest() {
    override fun buildSubject(): EntitiesRepository {
        return DatabaseEntitiesRepository(
            ApplicationProvider.getApplicationContext(),
            TempFiles.createTempDir().absolutePath
        )
    }

    @Test
    fun `#save supports properties with db column names saving new entities and updating existing ones`() {
        val repository = buildSubject()
        val entity = Entity.New(
            "1",
            "One",
            properties = listOf(Pair("_id", "value"), Pair("version", "value"))
        )

        repository.save("things", entity)
        val savedEntity = repository.query("things")[0]
        assertThat(savedEntity, sameEntityAs(entity))

        repository.save("things", savedEntity)
        assertThat(repository.query("things")[0], sameEntityAs(savedEntity))
    }
}
