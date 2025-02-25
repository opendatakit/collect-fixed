package org.odk.collect.android.formmanagement

import android.os.Handler
import android.os.Looper
import org.javarosa.core.model.FormDef
import org.javarosa.core.model.condition.EvaluationContext
import org.javarosa.core.model.condition.FilterStrategy
import org.javarosa.core.model.instance.DataInstance
import org.javarosa.core.model.instance.TreeReference
import org.javarosa.form.api.FormEntryController
import org.javarosa.form.api.FormEntryModel
import org.javarosa.xpath.expr.XPathExpression
import org.odk.collect.android.application.Collect
import org.odk.collect.android.dynamicpreload.ExternalDataManagerImpl
import org.odk.collect.android.dynamicpreload.handler.ExternalDataHandlerPull
import org.odk.collect.android.preferences.SettingsExt.getExperimentalOptIn
import org.odk.collect.android.tasks.FormLoaderTask.FormEntryControllerFactory
import org.odk.collect.androidshared.ui.ToastUtils
import org.odk.collect.entities.javarosa.filter.LocalEntitiesFilterStrategy
import org.odk.collect.entities.javarosa.filter.PullDataFunctionHandler
import org.odk.collect.entities.javarosa.finalization.EntityFormFinalizationProcessor
import org.odk.collect.entities.storage.EntitiesRepository
import org.odk.collect.settings.keys.ProjectKeys
import org.odk.collect.shared.settings.Settings
import java.io.File
import java.util.function.Supplier
import kotlin.system.measureTimeMillis

class CollectFormEntryControllerFactory(
    private val entitiesRepository: EntitiesRepository,
    private val settings: Settings
) :
    FormEntryControllerFactory {
    override fun create(formDef: FormDef, formMediaDir: File): FormEntryController {
        val externalDataManager = ExternalDataManagerImpl(formMediaDir).also {
            Collect.getInstance().externalDataManager = it
        }

        return FormEntryController(FormEntryModel(formDef)).also {
            val externalDataHandlerPull = ExternalDataHandlerPull(externalDataManager)
            it.addFunctionHandler(
                PullDataFunctionHandler(
                    entitiesRepository,
                    externalDataHandlerPull
                )
            )
            it.addPostProcessor(EntityFormFinalizationProcessor())

            if (settings.getExperimentalOptIn(ProjectKeys.KEY_DEBUG_FILTERS)) {
                it.addFilterStrategy(LoggingFilterStrategy())
            }

            it.addFilterStrategy(LocalEntitiesFilterStrategy(entitiesRepository))
        }
    }
}

private class LoggingFilterStrategy : FilterStrategy {
    override fun filter(
        sourceInstance: DataInstance<*>,
        nodeSet: TreeReference,
        predicate: XPathExpression,
        children: MutableList<TreeReference>,
        evaluationContext: EvaluationContext,
        next: Supplier<MutableList<TreeReference>>
    ): MutableList<TreeReference> {
        val result: MutableList<TreeReference>
        val filterTime = measureTimeMillis { result = next.get() }
        Handler(Looper.getMainLooper()).post {
            ToastUtils.showShortToast("Filter took ${filterTime / 1000.0}s")
        }

        return result
    }
}
