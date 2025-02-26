package org.odk.collect.entities.server

interface EntitySource {
    fun isDeleted(integrityUrl: String, ids: List<String>): List<Pair<String, Boolean>>
}
