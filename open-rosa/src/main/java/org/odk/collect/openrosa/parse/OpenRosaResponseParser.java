package org.odk.collect.openrosa.parse;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kxml2.kdom.Document;
import org.odk.collect.forms.FormListItem;
import org.odk.collect.forms.MediaFile;
import org.odk.collect.openrosa.forms.EntityIntegrity;

import java.util.List;

public interface OpenRosaResponseParser {

    @Nullable List<FormListItem> parseFormList(Document document);
    @Nullable List<MediaFile> parseManifest(Document document);

    @NotNull List<EntityIntegrity> parseIntegrityResponse(Document doc);
}
