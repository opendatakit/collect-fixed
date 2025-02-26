package org.odk.collect.android.formmanagement

import org.odk.collect.entities.server.EntitySource

class StubEntitySource : EntitySource {
    override fun isDeleted(integrityUrl: String, ids: List<String>): List<Pair<String, Boolean>> {
        return ids.map { Pair(it, false) }
    }
}
