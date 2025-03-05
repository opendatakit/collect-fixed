package org.odk.collect.android.feature.formentry

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.odk.collect.android.support.StubOpenRosaServer.EntityListItem
import org.odk.collect.android.support.TestDependencies
import org.odk.collect.android.support.pages.FormEntryPage
import org.odk.collect.android.support.pages.MainMenuPage
import org.odk.collect.android.support.rules.CollectTestRule
import org.odk.collect.android.support.rules.TestRuleChain
import org.odk.collect.strings.R

@RunWith(AndroidJUnit4::class)
class EntityFormTest {

    private val rule = CollectTestRule(useDemoProject = false)
    private val testDependencies = TestDependencies()

    @get:Rule
    val ruleChain: RuleChain = TestRuleChain.chain(testDependencies)
        .around(rule)

    @Test
    fun fillingEntityRegistrationForm_createsEntityWithValuesTreatedAsOpaqueStrings() {
        testDependencies.server.addForm("entities-with-dates-registration.xml")
        testDependencies.server.addForm("entities-with-dates-follow-up.xml")

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankForm("Entities With Dates Registration")
            .swipeToEndScreen()
            .clickFinalize()

            .startBlankForm("Entities With Dates Follow Up")
            // .assertText("2024-11-15")
            .swipeToNextQuestion("Select date")
            .assertText("2024-11-15")
    }

    @Test
    fun fillingEntityRegistrationForm_createsEntityForFollowUpForms() {
        testDependencies.server.addForm("one-question-entity-registration.xml")
        testDependencies.server.addForm(
            "one-question-entity-update.xml",
            listOf(EntityListItem("people.csv"))
        )

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankForm("One Question Entity Registration")
            .fillOutAndFinalize(FormEntryPage.QuestionAndAnswer("Name", "Logan Roy"))

            .startBlankForm("One Question Entity Update")
            .assertQuestion("Select person")
            .assertText("Roman Roy")
            .assertText("Logan Roy")
    }

    @Test
    fun fillingEntityRegistrationForm_createsEntityForFollowUpFormsWithCachedFormDefs() {
        testDependencies.server.addForm("one-question-entity-registration.xml")
        testDependencies.server.addForm(
            "one-question-entity-update.xml",
            listOf(EntityListItem("people.csv"))
        )

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankForm("One Question Entity Update") // Open to create cached form def
            .pressBackAndDiscardForm()

            .startBlankForm("One Question Entity Registration")
            .fillOutAndFinalize(FormEntryPage.QuestionAndAnswer("Name", "Logan Roy"))

            .startBlankForm("One Question Entity Update")
            .assertQuestion("Select person")
            .assertText("Roman Roy")
            .assertText("Logan Roy")
    }

    @Test
    fun fillingEntityUpdateForm_updatesEntityForFollowUpForms() {
        testDependencies.server.addForm(
            "one-question-entity-update.xml",
            listOf(EntityListItem("people.csv"))
        )

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankForm("One Question Entity Update")
            .assertQuestion("Select person")
            .clickOnText("Roman Roy")
            .swipeToNextQuestion("Name")
            .answerQuestion("Name", "Romulus Roy")
            .swipeToEndScreen()
            .clickFinalize()

            .startBlankForm("One Question Entity Update")
            .assertText("Romulus Roy")
            .assertTextDoesNotExist("Roman Roy")
    }

    @Test
    fun fillingEntityCreateForm_withUpdate_doesNotCreateEntityForFollowUpForms() {
        testDependencies.server.addForm("one-question-entity-create-and-update.xml")
        testDependencies.server.addForm(
            "one-question-entity-update.xml",
            listOf(EntityListItem("people.csv"))
        )

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankForm("One Question Entity Registration")
            .fillOutAndFinalize(FormEntryPage.QuestionAndAnswer("Name", "Logan Roy"))

            .startBlankForm("One Question Entity Update")
            .assertQuestion("Select person")
            .assertText("Roman Roy")
            .assertTextDoesNotExist("Logan Roy")
    }

    @Test
    fun fillingEntityUpdateForm_withCreate_doesNotUpdateEntityForFollowUpForms() {
        testDependencies.server.addForm(
            "one-question-entity-update-and-create.xml",
            listOf(EntityListItem("people.csv"))
        )

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankForm("One Question Entity Update")
            .assertQuestion("Select person")
            .clickOnText("Roman Roy")
            .swipeToNextQuestion("Name")
            .answerQuestion("Name", "Romulus Roy")
            .swipeToEndScreen()
            .clickFinalize()

            .startBlankForm("One Question Entity Update")
            .assertTextDoesNotExist("Romulus Roy")
            .assertText("Roman Roy")
    }

    @Test
    fun entityListSecondaryInstancesAreConsistentBetweenFollowUpForms() {
        testDependencies.server.apply {
            addForm(
                "one-question-entity-update.xml",
                listOf(EntityListItem("people.csv", "people.csv", 1))
            )

            addForm(
                "one-question-entity-follow-up.xml",
                listOf(EntityListItem("people.csv", "updated-people.csv", 2))
            )
        }

        rule.withProject(testDependencies.server)
            .clickGetBlankForm()
            .clickClearAll()
            .clickForm("One Question Entity Update")
            .clickGetSelected()
            .clickOK(MainMenuPage())
            .startBlankForm("One Question Entity Update")
            .assertText("Roman Roy")
            .pressBackAndDiscardForm()

            .clickGetBlankForm()
            .clickGetSelected() // Collect automatically only selects the un-downloaded forms
            .clickOK(MainMenuPage())
            .startBlankForm("One Question Entity Update")
            .assertText("Ro-Ro Roy")
            .assertTextDoesNotExist("Roman Roy")
    }

    @Test
    fun fillingEntityRegistrationForm_whenFormUsesOldSpecVersion_doesNotCreateEntityForFollowUpForms() {
        testDependencies.server.addForm("one-question-entity-registration-v2023.1.xml")
        testDependencies.server.addForm(
            "one-question-entity-update.xml",
            listOf(EntityListItem("people.csv"))
        )

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankForm("One Question Entity Registration")
            .fillOutAndFinalize(FormEntryPage.QuestionAndAnswer("Name", "Logan Roy"))

            .startBlankForm("One Question Entity Update")
            .assertQuestion("Select person")
            .assertText("Roman Roy")
            .assertTextDoesNotExist("Logan Roy")
    }

    @Test
    fun manualEntityFormDownload_withUnsupportedSpecVersion_completesSuccessfully_butThrowsAnErrorAfterOpeningIt() {
        testDependencies.server.addForm("one-question-entity-registration-v2020.1.xml")

        rule.withProject(testDependencies.server)
            .clickGetBlankForm()
            .clickClearAll()
            .clickForm("One Question Entity Registration")
            .clickGetSelected()
            .clickOK(MainMenuPage())
            .startBlankFormWithError("One Question Entity Registration", true)
            .assertTextInDialog(R.string.unrecognized_entity_version, "2020.1.0")
            .clickOKOnDialog(MainMenuPage())
    }

    @Test
    fun automaticEntityFormDownload_withUnsupportedSpecVersion_completesSuccessfully_butThrowsAnErrorAfterOpeningIt() {
        testDependencies.server.addForm("one-question-entity-registration-v2020.1.xml")

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankFormWithError("One Question Entity Registration", true)
            .assertTextInDialog(R.string.unrecognized_entity_version, "2020.1.0")
            .clickOKOnDialog(MainMenuPage())
    }

    @Test
    fun syncEntityFormFromDisc_withUnsupportedSpecVersion_completesSuccessfully_butThrowsAnErrorAfterOpeningIt() {
        rule.startAtFirstLaunch()
            .clickTryCollect()
            .copyForm("one-question-entity-registration-v2020.1.xml")
            .startBlankFormWithError("One Question Entity Registration", true)
            .assertTextInDialog(R.string.unrecognized_entity_version, "2020.1.0")
            .clickOKOnDialog(MainMenuPage())
    }

    @Test
    fun closingEntityForm_releasesTheLockAndLetsOtherEntityFormsToBeStarted() {
        rule.startAtFirstLaunch()
            .clickTryCollect()
            .copyForm("one-question-entity-registration.xml")
            .startBlankForm("One Question Entity Registration")
            .pressBackAndDiscardForm()
            .startBlankForm("One Question Entity Registration")
    }

    @Test
    fun aLocallyCreatedEntity_thatIsDeletedOnTheServer_isNotAvailableToFollowUpForms() {
        testDependencies.server.addForm("one-question-entity-registration-id.xml")
        testDependencies.server.addForm(
            "one-question-entity-update.xml",
            listOf(EntityListItem("people.csv"))
        )

        rule.withMatchExactlyProject(testDependencies.server.url)
            .startBlankForm("One Question Entity Registration")
            .fillOutAndFinalize(FormEntryPage.QuestionAndAnswer("Name", "Logan Roy"))

            .also {
                testDependencies.server.deleteEntity("people.csv", "Logan Roy")
            }

            .clickFillBlankForm()
            .clickRefresh()

            .clickOnForm("One Question Entity Update")
            .assertQuestion("Select person")
            .assertText("Roman Roy")
            .assertTextDoesNotExist("Logan Roy")
    }
}
