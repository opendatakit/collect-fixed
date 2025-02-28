package org.odk.collect.formstest;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.odk.collect.forms.Form;
import org.odk.collect.forms.FormsRepository;
import org.odk.collect.forms.savepoints.Savepoint;
import org.odk.collect.forms.savepoints.SavepointsRepository;
import org.odk.collect.shared.strings.Md5;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.odk.collect.formstest.FormUtils.createXFormBody;

public abstract class FormsRepositoryTest {
    protected final SavepointsRepository savepointsRepository = new InMemSavepointsRepository();

    public abstract FormsRepository buildSubject();

    public abstract FormsRepository buildSubject(Supplier<Long> clock);

    public abstract String getFormFilesPath();

    @Test
    public void getLatestByFormIdAndVersion_whenFormHasNullVersion_returnsForm() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("1", null, getFormFilesPath())
                .build());

        Form form = formsRepository.getLatestByFormIdAndVersion("1", null);
        assertThat(form, notNullValue());
        assertThat(form.getDbId(), is(1L));
    }

    @Test
    public void getLatestByFormIdAndVersion_whenMultipleExist_returnsLatest() {
        Supplier<Long> mockClock = mock(Supplier.class);
        when(mockClock.get()).thenReturn(2L, 3L, 1L);

        FormsRepository formsRepository = buildSubject(mockClock);
        formsRepository.save(FormUtils.buildForm("1", "1", getFormFilesPath(), createXFormBody("1", "1", "Form1"))
                .build());
        formsRepository.save(FormUtils.buildForm("1", "1", getFormFilesPath(), createXFormBody("1", "1", "Form2"))
                .build());
        formsRepository.save(FormUtils.buildForm("1", "1", getFormFilesPath(), createXFormBody("1", "1", "Form3"))
                .build());

        Form form = formsRepository.getLatestByFormIdAndVersion("1", "1");
        assertThat(form, notNullValue());
        assertThat(form.getDbId(), is(2L));
    }

    @Test
    public void getAllByFormIdAndVersion_whenFormHasNullVersion_returnsAllMatchingForms() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("1", null, getFormFilesPath(), createXFormBody("1", null, "Form1"))
                .build());

        formsRepository.save(FormUtils.buildForm("1", null, getFormFilesPath(), createXFormBody("1", null, "Form2"))
                .build());

        formsRepository.save(FormUtils.buildForm("1", "7", getFormFilesPath(), createXFormBody("1", "7", "Form3"))
                .build());

        List<Form> forms = formsRepository.getAllByFormIdAndVersion("1", null);
        assertThat(forms.size(), is(2));
        assertThat(forms.get(0).getVersion(), is(nullValue()));
        assertThat(forms.get(1).getVersion(), is(nullValue()));
    }

    @Test
    public void getAllNotDeletedByFormId_doesNotReturnDeletedForms() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("1", "deleted", getFormFilesPath())
                .deleted(true)
                .build()
        );

        formsRepository.save(FormUtils.buildForm("1", "not-deleted", getFormFilesPath())
                .deleted(false)
                .build()
        );

        List<Form> forms = formsRepository.getAllNotDeletedByFormId("1");
        assertThat(forms.size(), is(1));
        assertThat(forms.get(0).getVersion(), equalTo("not-deleted"));
    }

    @Test
    public void getAllNotDeletedByFormIdAndVersion_onlyReturnsNotDeletedFormsThatMatchVersion() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("id", "1", getFormFilesPath(), createXFormBody("id", "1", "Form1"))
                .deleted(true)
                .build()
        );
        formsRepository.save(FormUtils.buildForm("id", "1", getFormFilesPath(), createXFormBody("id", "1", "Form2"))
                .deleted(false)
                .build()
        );

        formsRepository.save(FormUtils.buildForm("id", "2", getFormFilesPath(), createXFormBody("id", "2", "Form3"))
                .deleted(true)
                .build()
        );
        formsRepository.save(FormUtils.buildForm("id", "2", getFormFilesPath(), createXFormBody("id", "2", "Form4"))
                .deleted(false)
                .build()
        );

        List<Form> forms = formsRepository.getAllNotDeletedByFormIdAndVersion("id", "2");
        assertThat(forms.size(), is(1));
        assertThat(forms.get(0).getVersion(), equalTo("2"));
    }

    @Test
    public void softDelete_marksDeletedAsTrue() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("1", null, getFormFilesPath())
                .build());

        formsRepository.softDelete(1L);
        assertThat(formsRepository.get(1L).isDeleted(), is(true));
    }

    @Test
    public void softDelete_deletesTheSavepointThatBelongsToTheFormThatShouldBeDeleted() {
        FormsRepository formsRepository = buildSubject();
        Form form1 = formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath()).build());
        Form form2 = formsRepository.save(FormUtils.buildForm("id2", "version", getFormFilesPath()).build());

        Savepoint savepoint1 = new Savepoint(form1.getDbId(), null, "", "");
        Savepoint savepoint2 = new Savepoint(form2.getDbId(), null, "", "");
        savepointsRepository.save(savepoint1);
        savepointsRepository.save(savepoint2);

        formsRepository.softDelete(form1.getDbId());

        assertThat(savepointsRepository.getAll(), contains(savepoint2));
    }

    @Test
    public void restore_marksDeletedAsFalse() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("1", null, getFormFilesPath())
                .deleted(true)
                .build());

        formsRepository.restore(1L);
        assertThat(formsRepository.get(1L).isDeleted(), is(false));
    }

    @Test
    public void save_multipleFormsWithTheSameHashIgnoresDuplicatesAndReturnsTheExistingForm() {
        FormsRepository formsRepository = buildSubject();
        Form form = FormUtils.buildForm("id", "version", getFormFilesPath()).build();

        Form form1 = formsRepository.save(form);
        Form form2 = formsRepository.save(form);

        assertThat(formsRepository.getAll().size(), is(1));
        assertNotNull(form1);
        assertNotNull(form2);
        assertEquals(form1, form2);
    }

    @Test
    public void save_addsId() {
        FormsRepository formsRepository = buildSubject();
        Form form = FormUtils.buildForm("id", "version", getFormFilesPath()).build();

        formsRepository.save(form);
        assertThat(formsRepository.getAll().get(0).getDbId(), notNullValue());
    }

    @Test
    public void save_addsMediaPath_whereMediaDirCanBeCreated() {
        FormsRepository formsRepository = buildSubject();
        Form form = FormUtils.buildForm("id", "version", getFormFilesPath()).build();
        assertThat(form.getFormMediaPath(), equalTo(null));

        Form savedForm = formsRepository.save(form);
        assertThat(new File(savedForm.getFormMediaPath()).mkdir(), is(true));
    }

    @Test
    public void save_addsHashBasedOnFormFile() {
        FormsRepository formsRepository = buildSubject();
        Form form = FormUtils.buildForm("id", "version", getFormFilesPath()).build();
        assertThat(form.getMD5Hash(), equalTo(null));

        formsRepository.save(form);

        String expectedHash = Md5.getMd5Hash(new File(form.getFormFilePath()));
        assertThat(formsRepository.get(1L).getMD5Hash(), equalTo(expectedHash));
    }

    @Test(expected = Exception.class)
    public void save_whenNoFormFilePath_explodes() {
        FormsRepository formsRepository = buildSubject();
        Form form = FormUtils.buildForm("id", "version", getFormFilesPath()).build();
        form = new Form.Builder(form)
                .formFilePath(null)
                .build();

        formsRepository.save(form);
    }

    @Test
    public void save_whenFormHasId_updatesExisting() {
        FormsRepository formsRepository = buildSubject();
        Form originalForm = formsRepository.save(FormUtils.buildForm("id", "version", getFormFilesPath())
                .displayName("original")
                .build());

        formsRepository.save(new Form.Builder(originalForm)
                .displayName("changed")
                .build());

        assertThat(formsRepository.get(originalForm.getDbId()).getDisplayName(), is("changed"));
    }

    @Test
    public void save_whenFormHasId_updatesHash() throws IOException {
        FormsRepository formsRepository = buildSubject();
        Form originalForm = formsRepository.save(FormUtils.buildForm("id", "version", getFormFilesPath())
                .displayName("original")
                .build());

        String newFormBody = FormUtils.createXFormBody("id", "version", "A different title");
        File formFile = new File(originalForm.getFormFilePath());
        FileUtils.writeByteArrayToFile(formFile, newFormBody.getBytes());

        formsRepository.save(new Form.Builder(originalForm)
                .displayName("changed")
                .build());

        String expectedHash = Md5.getMd5Hash(formFile);
        assertThat(formsRepository.get(originalForm.getDbId()).getMD5Hash(), is(expectedHash));
    }

    @Test
    public void save_withNewFormVersion_copiesLanguageFromLatestFormVersion() {
        FormsRepository formsRepository = buildSubject();

        Form formV1 = formsRepository.save(FormUtils.buildForm("id", "1", getFormFilesPath()).build());
        formsRepository.save(new Form.Builder(formV1)
                .language("Spanish")
                .build());

        Form formV2 = formsRepository.save(FormUtils.buildForm(formV1.getFormId(), "2", getFormFilesPath()).build());
        formsRepository.save(new Form.Builder(formV2)
                .language("Polish")
                .build());

        Form formV3 = formsRepository.save(FormUtils.buildForm(formV1.getFormId(), "3", getFormFilesPath()).build());

        assertThat(formsRepository.get(formV3.getDbId()).getLanguage(), is("Polish"));
    }

    @Test
    public void delete_deletesFiles() throws Exception {
        FormsRepository formsRepository = buildSubject();
        Form form = formsRepository.save(FormUtils.buildForm("id", "version", getFormFilesPath()).build());

        // FormRepository doesn't automatically create all form files
        File mediaDir = new File(form.getFormMediaPath());
        mediaDir.mkdir();
        File cacheFile = new File(form.getJrCacheFilePath());
        cacheFile.createNewFile();

        File formFile = new File(form.getFormFilePath());
        assertThat(formFile.exists(), is(true));
        assertThat(mediaDir.exists(), is(true));
        assertThat(cacheFile.exists(), is(true));

        formsRepository.delete(form.getDbId());
        assertThat(formFile.exists(), is(false));
        assertThat(mediaDir.exists(), is(false));
        assertThat(cacheFile.exists(), is(false));
    }

    @Test
    public void delete_deletesTheSavepointThatBelongsToTheFormThatShouldBeDeleted() {
        FormsRepository formsRepository = buildSubject();
        Form form1 = formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath()).build());
        Form form2 = formsRepository.save(FormUtils.buildForm("id2", "version", getFormFilesPath()).build());

        Savepoint savepoint1 = new Savepoint(form1.getDbId(), null, "", "");
        Savepoint savepoint2 = new Savepoint(form2.getDbId(), null, "", "");
        savepointsRepository.save(savepoint1);
        savepointsRepository.save(savepoint2);

        formsRepository.delete(form1.getDbId());

        assertThat(savepointsRepository.getAll(), contains(savepoint2));
    }

    @Test
    public void delete_whenMediaPathIsFile_deletesFiles() throws Exception {
        FormsRepository formsRepository = buildSubject();
        Form form = formsRepository.save(FormUtils.buildForm("id", "version", getFormFilesPath()).build());

        // FormRepository currently doesn't manage media file path other than deleting it
        String mediaPath = form.getFormMediaPath();
        new File(mediaPath).createNewFile();

        File formFile = new File(form.getFormFilePath());
        File mediaDir = new File(form.getFormMediaPath());
        assertThat(formFile.exists(), is(true));
        assertThat(mediaDir.exists(), is(true));

        formsRepository.delete(1L);
        assertThat(formFile.exists(), is(false));
        assertThat(mediaDir.exists(), is(false));
    }

    @Test
    public void deleteAll_deletesAllForms() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath()).build());
        formsRepository.save(FormUtils.buildForm("id2", "version", getFormFilesPath()).build());

        List<Form> forms = formsRepository.getAll();

        formsRepository.deleteAll();
        assertThat(formsRepository.getAll().size(), is(0));

        for (Form form : forms) {
            assertThat(new File(form.getFormFilePath()).exists(), is(false));
            assertThat(new File(form.getFormMediaPath()).exists(), is(false));
        }
    }

    @Test
    public void deleteAll_deletesAllSavepointsThatBelongToFormsThatShouldBeDeleted() {
        FormsRepository formsRepository = buildSubject();
        Form form1 = formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath()).build());
        Form form2 = formsRepository.save(FormUtils.buildForm("id2", "version", getFormFilesPath()).build());

        Savepoint savepoint1 = new Savepoint(form1.getDbId(), null, "", "");
        Savepoint savepoint2 = new Savepoint(form2.getDbId(), null, "", "");
        savepointsRepository.save(savepoint1);
        savepointsRepository.save(savepoint2);

        formsRepository.deleteAll();

        assertThat(savepointsRepository.getAll().isEmpty(), equalTo(true));
    }

    @Test
    public void deleteByMd5Hash_deletesFormsWithMatchingHash() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath(), createXFormBody("id1", "version", "Form1")).build());
        formsRepository.save(FormUtils.buildForm("id2", "version", getFormFilesPath(), createXFormBody("id2", "version", "Form2")).build());

        List<Form> id1Forms = formsRepository.getAllByFormIdAndVersion("id1", "version");
        formsRepository.deleteByMd5Hash(id1Forms.get(0).getMD5Hash());

        assertThat(formsRepository.getAll().size(), is(1));
        assertThat(formsRepository.getAll().get(0).getFormId(), is("id2"));
    }

    @Test
    public void deleteByMd5Hash_deletesTheSavepointThatBelongsToTheFormThatShouldBeDeleted() {
        FormsRepository formsRepository = buildSubject();
        Form form1 = formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath(), createXFormBody("id1", "version", "Form1")).build());
        Form form2 = formsRepository.save(FormUtils.buildForm("id2", "version", getFormFilesPath(), createXFormBody("id2", "version", "Form2")).build());

        Savepoint savepoint1 = new Savepoint(form1.getDbId(), null, "", "");
        Savepoint savepoint2 = new Savepoint(form2.getDbId(), null, "", "");
        savepointsRepository.save(savepoint1);
        savepointsRepository.save(savepoint2);

        List<Form> id1Forms = formsRepository.getAllByFormIdAndVersion("id1", "version");
        formsRepository.deleteByMd5Hash(id1Forms.get(0).getMD5Hash());

        assertThat(savepointsRepository.getAll(), contains(savepoint2));
    }

    @Test(expected = Exception.class)
    public void getOneByMd5Hash_whenHashIsNull_explodes() {
        buildSubject().getOneByMd5Hash(null);
    }

    @Test
    public void getOneByMd5Hash_returnsMatchingForm() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath()).build());
        Form form2 = formsRepository.save(FormUtils.buildForm("id2", "version", getFormFilesPath()).build());

        assertThat(formsRepository.getOneByMd5Hash(form2.getMD5Hash()), is(form2));
    }

    @Test
    public void getOneByPath_returnsMatchingForm() {
        FormsRepository formsRepository = buildSubject();
        formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath()).build());

        Form form2 = FormUtils.buildForm("id2", "version", getFormFilesPath()).build();
        formsRepository.save(form2);

        assertThat(formsRepository.getOneByPath(form2.getFormFilePath()).getFormId(), is("id2"));
    }

    @Test
    public void getAllFormId_returnsMatchingForms() {
        FormsRepository formsRepository = buildSubject();
        Form form1 = formsRepository.save(FormUtils.buildForm("id1", "version", getFormFilesPath()).build());
        Form form2 = formsRepository.save(FormUtils.buildForm("id1", "other_version", getFormFilesPath()).build());
        formsRepository.save(FormUtils.buildForm("id2", "version", getFormFilesPath()).build());

        List<Form> forms = formsRepository.getAllByFormId("id1");
        assertThat(forms.size(), is(2));
        assertThat(forms, contains(form1, form2));
    }
}
