package org.odk.collect.googlemaps;

import static org.odk.collect.androidshared.ui.PrefUtils.createListPref;
import static org.odk.collect.androidshared.ui.PrefUtils.getInt;
import static kotlin.collections.SetsKt.setOf;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.preference.Preference;

import com.google.android.gms.maps.GoogleMap;

import org.odk.collect.androidshared.system.OpenGLVersionChecker;
import org.odk.collect.androidshared.system.PlayServicesChecker;
import org.odk.collect.androidshared.ui.ToastUtils;
import org.odk.collect.maps.MapConfigurator;
import org.odk.collect.maps.layers.MbtilesFile;
import org.odk.collect.maps.layers.MbtilesFile.LayerType;
import org.odk.collect.settings.keys.ProjectKeys;
import org.odk.collect.shared.settings.Settings;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class GoogleMapConfigurator implements MapConfigurator {
    private final String prefKey;
    private final int sourceLabelId;
    private final GoogleMapTypeOption[] options;

    /** Constructs a configurator with a few Google map type options to choose from. */
    public GoogleMapConfigurator(String prefKey, int sourceLabelId, GoogleMapTypeOption... options) {
        this.prefKey = prefKey;
        this.sourceLabelId = sourceLabelId;
        this.options = options;
    }

    @Override public boolean isAvailable(Context context) {
        try {
            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            String apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY");

            return isGoogleMapsSdkAvailable(context) && isGooglePlayServicesAvailable(context) && !apiKey.equals("");
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isGooglePlayServicesAvailable(Context context) {
        return new PlayServicesChecker().isGooglePlayServicesAvailable(context);
    }

    private static boolean isGoogleMapsSdkAvailable(Context context) {
        /*
         * The Google Maps SDK for Android requires OpenGL ES version 2.
         * See: https://developers.google.com/maps/documentation/android-sdk/config
         */
        return OpenGLVersionChecker.isOpenGLv2Supported(context);
    }

    @Override public void showUnavailableMessage(Context context) {
        if (!isGoogleMapsSdkAvailable(context)) {
            ToastUtils.showLongToast(context.getString(
                org.odk.collect.strings.R.string.basemap_source_unavailable, context.getString(sourceLabelId)));
        }

        PlayServicesChecker playServicesChecker = new PlayServicesChecker();
        if (!playServicesChecker.isGooglePlayServicesAvailable(context)) {
            playServicesChecker.showGooglePlayServicesAvailabilityErrorDialog(context);
        }
    }

    @Override public List<Preference> createPrefs(Context context, Settings settings) {
        int[] labelIds = new int[options.length];
        String[] values = new String[options.length];
        for (int i = 0; i < options.length; i++) {
            labelIds[i] = options[i].labelId;
            values[i] = Integer.toString(options[i].mapType);
        }
        String prefTitle = context.getString(
            org.odk.collect.strings.R.string.map_style_label, context.getString(sourceLabelId));
        return Collections.singletonList(createListPref(
            context, prefKey, prefTitle, labelIds, values, settings
        ));
    }

    @Override public Set<String> getPrefKeys() {
        return prefKey.isEmpty() ? setOf(ProjectKeys.KEY_REFERENCE_LAYER) :
                setOf(prefKey, ProjectKeys.KEY_REFERENCE_LAYER);
    }

    @Override public Bundle buildConfig(Settings prefs) {
        Bundle config = new Bundle();
        config.putInt(GoogleMapFragment.KEY_MAP_TYPE,
            getInt(ProjectKeys.KEY_GOOGLE_MAP_STYLE, GoogleMap.MAP_TYPE_NORMAL, prefs));
        config.putString(GoogleMapFragment.KEY_REFERENCE_LAYER,
            prefs.getString(ProjectKeys.KEY_REFERENCE_LAYER));
        return config;
    }

    @Override public boolean supportsLayer(File file) {
        // GoogleMapFragment supports only raster tiles.
        return MbtilesFile.readLayerType(file) == LayerType.RASTER;
    }

    @Override public String getDisplayName(File file) {
        String name = MbtilesFile.readName(file);
        return name != null ? name : file.getName();
    }

    public static class GoogleMapTypeOption {
        final int mapType;
        final int labelId;

        public GoogleMapTypeOption(int mapType, int labelId) {
            this.mapType = mapType;
            this.labelId = labelId;
        }
    }
}
