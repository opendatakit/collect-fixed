package org.odk.collect.android.database.forms;

import static android.provider.BaseColumns._ID;
import static org.odk.collect.android.database.DatabaseConstants.FORMS_TABLE_NAME;
import static org.odk.collect.android.database.DatabaseObjectMapper.getFormFromCurrentCursorPosition;
import static org.odk.collect.android.database.DatabaseObjectMapper.getValuesFromForm;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.AUTO_DELETE;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.AUTO_SEND;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.BASE64_RSA_PUBLIC_KEY;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.DATE;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.DELETED_DATE;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.DESCRIPTION;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.DISPLAY_NAME;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.USES_ENTITIES;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.FORM_FILE_PATH;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.FORM_MEDIA_PATH;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.GEOMETRY_XPATH;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.JRCACHE_FILE_PATH;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.JR_FORM_ID;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.JR_VERSION;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.LANGUAGE;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.LAST_DETECTED_ATTACHMENTS_UPDATE_DATE;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.MD5_HASH;
import static org.odk.collect.android.database.forms.DatabaseFormColumns.SUBMISSION_URI;
import static org.odk.collect.shared.PathUtils.getRelativeFilePath;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteBlobTooBigException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.StrictMode;

import org.jetbrains.annotations.NotNull;
import org.odk.collect.db.sqlite.DatabaseConnection;
import org.odk.collect.android.database.DatabaseConstants;
import org.odk.collect.android.utilities.FileUtils;
import org.odk.collect.forms.Form;
import org.odk.collect.forms.FormsRepository;
import org.odk.collect.forms.savepoints.SavepointsRepository;
import org.odk.collect.shared.files.FileExt;
import org.odk.collect.shared.strings.Md5;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import timber.log.Timber;

public class DatabaseFormsRepository implements FormsRepository {

    private final DatabaseConnection databaseConnection;
    private final String formsPath;
    private final String cachePath;
    private final Supplier<Long> clock;
    private final SavepointsRepository savepointsRepository;

    public DatabaseFormsRepository(Context context, String dbPath, String formsPath, String cachePath, Supplier<Long> clock, SavepointsRepository savepointsRepository) {
        this.formsPath = formsPath;
        this.cachePath = cachePath;
        this.clock = clock;
        this.databaseConnection = new DatabaseConnection(
                context,
                dbPath,
                DatabaseConstants.FORMS_DATABASE_NAME,
                new FormDatabaseMigrator(),
                DatabaseConstants.FORMS_DATABASE_VERSION
        );
        this.savepointsRepository = savepointsRepository;
    }

    @Nullable
    @Override
    public Form get(Long id) {
        return queryForForm(_ID + "=?", new String[]{id.toString()});
    }

    @Nullable
    @Override
    public Form getLatestByFormIdAndVersion(String jrFormId, @Nullable String jrVersion) {
        List<Form> all = getAllByFormIdAndVersion(jrFormId, jrVersion);
        if (!all.isEmpty()) {
            return all.stream().max(Comparator.comparingLong(Form::getDate)).get();
        } else {
            return null;
        }
    }

    @Nullable
    @Override
    public Form getOneByPath(String path) {
        String selection = FORM_FILE_PATH + "=?";
        String[] selectionArgs = {getRelativeFilePath(formsPath, path)};
        return queryForForm(selection, selectionArgs);
    }

    @Nullable
    @Override
    public Form getOneByMd5Hash(@NotNull String hash) {
        if (hash == null) {
            throw new IllegalArgumentException("Missing form hash. ODK-compatible servers must include form hashes in their form lists. Please talk to the person who asked you to collect data.");
        }

        String selection = DatabaseFormColumns.MD5_HASH + "=?";
        String[] selectionArgs = {hash};
        return queryForForm(selection, selectionArgs);
    }

    @Override
    public List<Form> getAll() {
        StrictMode.noteSlowCall("Accessing readable DB");
        return queryForForms(null, null);
    }

    @Override
    public List<Form> getAllByFormIdAndVersion(String jrFormId, @Nullable String jrVersion) {
        if (jrVersion != null) {
            return queryForForms(JR_FORM_ID + "=? AND " + JR_VERSION + "=?", new String[]{jrFormId, jrVersion});
        } else {
            return queryForForms(JR_FORM_ID + "=? AND " + JR_VERSION + " IS NULL", new String[]{jrFormId});
        }
    }

    @Override
    public List<Form> getAllByFormId(String formId) {
        StrictMode.noteSlowCall("Accessing readable DB");
        return queryForForms(JR_FORM_ID + "=?", new String[]{formId});
    }

    @Override
    public List<Form> getAllNotDeletedByFormId(String jrFormId) {
        StrictMode.noteSlowCall("Accessing readable DB");
        return queryForForms(JR_FORM_ID + "=? AND " + DELETED_DATE + " IS NULL", new String[]{jrFormId});
    }


    @Override
    public List<Form> getAllNotDeletedByFormIdAndVersion(String jrFormId, @Nullable String jrVersion) {
        StrictMode.noteSlowCall("Accessing readable DB");

        if (jrVersion != null) {
            return queryForForms(DELETED_DATE + " IS NULL AND " + JR_FORM_ID + "=? AND " + JR_VERSION + "=?", new String[]{jrFormId, jrVersion});
        } else {
            return queryForForms(DELETED_DATE + " IS NULL AND " + JR_FORM_ID + "=? AND " + JR_VERSION + " IS NULL", new String[]{jrFormId});
        }
    }

    @Override
    public Form save(@NotNull Form form) {
        final ContentValues values = getValuesFromForm(form, formsPath);

        String md5Hash = Md5.getMd5Hash(new File(form.getFormFilePath()));
        values.put(MD5_HASH, md5Hash);
        values.put(FORM_MEDIA_PATH, getRelativeFilePath(formsPath, FileUtils.constructMediaPath(form.getFormFilePath())));
        values.put(JRCACHE_FILE_PATH, md5Hash + ".formdef");

        if (form.isDeleted()) {
            values.put(DELETED_DATE, 0L);
        } else {
            values.putNull(DELETED_DATE);
        }

        if (form.getDbId() == null) {
            values.put(DATE, clock.get());

            List<Form> existingFormsWithSameFormId = getAllByFormId(form.getFormId());
            if (!existingFormsWithSameFormId.isEmpty()) {
                Form latestFormWithSameFormId = Collections.max(existingFormsWithSameFormId, Comparator.comparing(Form::getDate));
                values.put(LANGUAGE, latestFormWithSameFormId.getLanguage());
            }

            Long idFromUri = insertForm(values);
            if (idFromUri == -1) {
                return getOneByMd5Hash(md5Hash);
            }
            return get(idFromUri);
        } else {
            updateForm(form.getDbId(), values);
            return get(form.getDbId());
        }
    }

    @Override
    public void delete(Long id) {
        String selection = _ID + "=?";
        String[] selectionArgs = {String.valueOf(id)};

        deleteForms(selection, selectionArgs);
    }

    @Override
    public void softDelete(Long id) {
        ContentValues values = new ContentValues();
        values.put(DELETED_DATE, System.currentTimeMillis());
        updateForm(id, values);
        savepointsRepository.delete(id, null);
    }

    @Override
    public void deleteByMd5Hash(@NotNull String md5Hash) {
        String selection = DatabaseFormColumns.MD5_HASH + "=?";
        String[] selectionArgs = {md5Hash};

        deleteForms(selection, selectionArgs);
    }

    @Override
    public void deleteAll() {
        deleteForms(null, null);
    }

    @Override
    public void restore(Long id) {
        ContentValues values = new ContentValues();
        values.putNull(DELETED_DATE);
        updateForm(id, values);
    }

    public Cursor rawQuery(Map<String, String> projectionMap, String[] projection, String selection, String[] selectionArgs, String sortOrder, String groupBy) {
        return queryAndReturnCursor(projectionMap, projection, selection, selectionArgs, sortOrder, groupBy);
    }

    @Nullable
    private Form queryForForm(String selection, String[] selectionArgs) {
        StrictMode.noteSlowCall("Accessing readable DB");
        List<Form> forms = queryForForms(selection, selectionArgs);
        return !forms.isEmpty() ? forms.get(0) : null;
    }

    private List<Form> queryForForms(String selection, String[] selectionArgs) {
        try (Cursor cursor = queryAndReturnCursor(null, null, selection, selectionArgs, null, null)) {
            return getFormsFromCursor(cursor, formsPath, cachePath);
        }
    }

    private Cursor queryAndReturnCursor(Map<String, String> projectionMap, String[] projection, String selection, String[] selectionArgs, String sortOrder, String groupBy) {
        SQLiteDatabase readableDatabase = databaseConnection.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(FORMS_TABLE_NAME);

        if (projection == null) {
            /*
             For some reason passing null as the projection doesn't always give us all the
             columns so we hardcode them here so it's explicit that we need these all back.
             The problem can occur, for example, when a new column is added to a database and the
             database needs to be updated. After the upgrade, the new column might not be returned,
             even though it already exists.
             */
            projection = new String[]{
                    _ID,
                    DISPLAY_NAME,
                    DESCRIPTION,
                    JR_FORM_ID,
                    JR_VERSION,
                    MD5_HASH,
                    DATE,
                    FORM_MEDIA_PATH,
                    FORM_FILE_PATH,
                    LANGUAGE,
                    SUBMISSION_URI,
                    BASE64_RSA_PUBLIC_KEY,
                    JRCACHE_FILE_PATH,
                    AUTO_SEND,
                    AUTO_DELETE,
                    GEOMETRY_XPATH,
                    DELETED_DATE,
                    LAST_DETECTED_ATTACHMENTS_UPDATE_DATE,
                    USES_ENTITIES
            };
        }

        if (projectionMap != null) {
            qb.setProjectionMap(projectionMap);
        }

        return qb.query(readableDatabase, projection, selection, selectionArgs, groupBy, null, sortOrder);
    }

    private Long insertForm(ContentValues values) {
        SQLiteDatabase writableDatabase = databaseConnection.getWritableDatabase();
        return writableDatabase.insertOrThrow(FORMS_TABLE_NAME, null, values);
    }

    private void updateForm(Long id, ContentValues values) {
        SQLiteDatabase writableDatabase = databaseConnection.getWritableDatabase();
        writableDatabase.update(FORMS_TABLE_NAME, values, _ID + "=?", new String[]{String.valueOf(id)});
    }

    private void deleteForms(String selection, String[] selectionArgs) {
        StrictMode.noteSlowCall("Accessing readable DB");

        List<Form> forms = queryForForms(selection, selectionArgs);
        for (Form form : forms) {
            deleteFilesForForm(form);
        }

        SQLiteDatabase writableDatabase = databaseConnection.getWritableDatabase();
        writableDatabase.delete(FORMS_TABLE_NAME, selection, selectionArgs);
    }

    @NotNull
    private static List<Form> getFormsFromCursor(Cursor cursor, String formsPath, String cachePath) {
        List<Form> forms = new ArrayList<>();
        if (cursor != null) {
            Object cursorSize = null;
            try {
                Field field = CursorWindow.class.getDeclaredField("sCursorWindowSize");
                field.setAccessible(true);
                cursorSize = field.get(null);
            } catch (Throwable e) {
                // ignore
            }

            try {
                cursor.moveToPosition(-1);
                while (cursor.moveToNext()) {
                    Form form = getFormFromCurrentCursorPosition(cursor, formsPath, cachePath);
                    forms.add(form);
                }
            } catch (SQLiteBlobTooBigException e) {
                Timber.w("SQLiteBlobTooBigException, sCursorWindowSize: %sB", cursorSize != null ? cursorSize : "?");
                throw e;
            }
        }
        return forms;
    }

    private void deleteFilesForForm(Form form) {
        // Delete form file
        if (form.getFormFilePath() != null) {
            new File(form.getFormFilePath()).delete();
        }

        // Delete cache file
        if (form.getJrCacheFilePath() != null) {
            new File(form.getJrCacheFilePath()).delete();
        }

        // Delete media files
        if (form.getFormMediaPath() != null) {
            File mediaDir = new File(form.getFormMediaPath());

            if (mediaDir.isDirectory()) {
                FileExt.deleteDirectory(mediaDir);
            } else {
                mediaDir.delete();
            }
        }

        savepointsRepository.delete(form.getDbId(), null);
    }
}
