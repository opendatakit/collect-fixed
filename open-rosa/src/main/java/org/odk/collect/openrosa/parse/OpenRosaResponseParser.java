package org.odk.collect.openrosa.parse;

import org.jetbrains.annotations.Nullable;
import org.kxml2.kdom.Document;
import org.odk.collect.forms.FormListItem;
import org.odk.collect.forms.MediaFile;

import java.util.List;

public interface OpenRosaResponseParser {

    @Nullable List<FormListItem> parseFormList(Document document);
    @Nullable List<MediaFile> parseManifest(Document document);
}
