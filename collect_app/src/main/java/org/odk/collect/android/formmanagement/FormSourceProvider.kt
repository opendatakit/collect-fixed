package org.odk.collect.android.formmanagement

import org.odk.collect.android.utilities.WebCredentialsUtils
import org.odk.collect.forms.FormSource
import org.odk.collect.openrosa.forms.OpenRosaClient
import org.odk.collect.openrosa.http.OpenRosaHttpInterface
import org.odk.collect.openrosa.parse.OpenRosaResponseParserImpl
import org.odk.collect.projects.ProjectDependencyFactory
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.shared.settings.Settings

class FormSourceProvider(
    private val settingsFactory: ProjectDependencyFactory<Settings>,
    private val openRosaHttpInterface: OpenRosaHttpInterface
) : ProjectDependencyFactory<FormSource> {

    override fun create(projectId: String): FormSource {
        val settings = settingsFactory.create(projectId)
        val serverURL = settings.getString(ProjectKeys.KEY_SERVER_URL)!!

        return OpenRosaClient(
            serverURL,
            openRosaHttpInterface,
            WebCredentialsUtils(settings),
            OpenRosaResponseParserImpl()
        )
    }
}
