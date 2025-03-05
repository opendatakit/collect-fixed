package org.odk.collect.android.formmanagement

import org.odk.collect.android.utilities.WebCredentialsUtils
import org.odk.collect.openrosa.forms.OpenRosaClient
import org.odk.collect.openrosa.http.OpenRosaHttpInterface
import org.odk.collect.openrosa.parse.Kxml2OpenRosaResponseParser
import org.odk.collect.projects.ProjectDependencyFactory
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.shared.settings.Settings

class OpenRosaClientProvider(
    private val settingsFactory: ProjectDependencyFactory<Settings>,
    private val openRosaHttpInterface: OpenRosaHttpInterface
) {

    fun create(projectId: String): OpenRosaClient {
        val settings = settingsFactory.create(projectId)
        val serverURL = settings.getString(ProjectKeys.KEY_SERVER_URL)!!

        return OpenRosaClient(
            serverURL,
            openRosaHttpInterface,
            WebCredentialsUtils(settings),
            Kxml2OpenRosaResponseParser
        )
    }
}
