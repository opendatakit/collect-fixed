package org.odk.collect.openrosa.parse

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.javarosa.xform.parse.XFormParser
import org.junit.Test
import org.kxml2.kdom.Document

class OpenRosaResponseParserImplTest {

    @Test
    fun `parseFormList() when document is empty, returns null`() {
        val formList = OpenRosaResponseParserImpl().parseFormList(Document())
        assertThat(formList, equalTo(null))
    }

    @Test
    fun `parseFormList() when xform hash is missing prefix, returns null hash for item`() {
        val response = StringBuilder()
            .appendLine("<?xml version='1.0' encoding='UTF-8' ?>")
            .appendLine("<xforms xmlns=\"http://openrosa.org/xforms/xformsList\">")
            .appendLine("<xform>")
            .appendLine("<formID>id</formID>")
            .appendLine("<name>form name</name>")
            .appendLine("<version>1</version>")
            .appendLine("<hash>blahblah</hash>")
            .appendLine("<downloadUrl>http://example.com</downloadUrl>")
            .appendLine("</xform>")
            .appendLine("</xforms>")
            .toString()

        val doc = XFormParser.getXMLDocument(response.reader())
        val formList = OpenRosaResponseParserImpl().parseFormList(doc)
        assertThat(formList!![0].hash, equalTo(null))
    }

    @Test
    fun `parseManifest() when media file hash is empty, returns null`() {
        val response = StringBuilder()
            .appendLine("<?xml version='1.0' encoding='UTF-8' ?>")
            .appendLine("<manifest xmlns=\"http://openrosa.org/xforms/xformsManifest\">")
            .appendLine("<mediaFile>")
            .appendLine("<filename>badger.png</filename>")
            .appendLine("<hash></hash>")
            .appendLine("<downloadUrl>http://funk.appspot.com/binaryData?blobKey=%3A477e3</downloadUrl>")
            .appendLine("</mediaFile>")
            .appendLine("</manifest>")
            .toString()

        val doc = XFormParser.getXMLDocument(response.reader())
        val mediaFiles = OpenRosaResponseParserImpl().parseManifest(doc)
        assertThat(mediaFiles, equalTo(null))
    }

    @Test
    fun `parseManifest() when document is empty, returns null`() {
        val formList = OpenRosaResponseParserImpl().parseManifest(Document())
        assertThat(formList, equalTo(null))
    }

    @Test
    fun `parseManifest() sanitizes media file names`() {
        val response = StringBuilder()
            .appendLine("<?xml version='1.0' encoding='UTF-8' ?>")
            .appendLine("<manifest xmlns=\"http://openrosa.org/xforms/xformsManifest\">")
            .appendLine("<mediaFile>")
            .appendLine("<filename>/../badgers.csv</filename>")
            .appendLine("<hash>blah</hash>")
            .appendLine("<downloadUrl>http://funk.appspot.com/binaryData?blobKey=%3A477e3</downloadUrl>")
            .appendLine("</mediaFile>")
            .appendLine("</manifest>")
            .toString()

        val doc = XFormParser.getXMLDocument(response.reader())
        val mediaFiles = OpenRosaResponseParserImpl().parseManifest(doc)!!
        assertThat(mediaFiles.size, equalTo(1))
        assertThat(mediaFiles[0].filename, equalTo("badgers.csv"))
    }

    @Test
    fun `parseManifest() when media file has type entityList returns isEntityList as true`() {
        val response = StringBuilder()
            .appendLine("<?xml version='1.0' encoding='UTF-8' ?>")
            .appendLine("<manifest xmlns=\"http://openrosa.org/xforms/xformsManifest\">")
            .appendLine("<mediaFile type=\"entityList\">")
            .appendLine("<filename>badgers.csv</filename>")
            .appendLine("<hash>blah</hash>")
            .appendLine("<downloadUrl>http://funk.appspot.com/binaryData?blobKey=%3A477e3</downloadUrl>")
            .appendLine("</mediaFile>")
            .appendLine("</manifest>")
            .toString()

        val doc = XFormParser.getXMLDocument(response.reader())
        val mediaFiles = OpenRosaResponseParserImpl().parseManifest(doc)!!
        assertThat(mediaFiles.size, equalTo(1))
        assertThat(mediaFiles[0].isEntityList, equalTo(true))
    }

    @Test
    fun `parseManifest() when media file does not have type returns isEntityList as false`() {
        val response = StringBuilder()
            .appendLine("<?xml version='1.0' encoding='UTF-8' ?>")
            .appendLine("<manifest xmlns=\"http://openrosa.org/xforms/xformsManifest\">")
            .appendLine("<mediaFile>")
            .appendLine("<filename>badgers.csv</filename>")
            .appendLine("<hash>blah</hash>")
            .appendLine("<downloadUrl>http://funk.appspot.com/binaryData?blobKey=%3A477e3</downloadUrl>")
            .appendLine("</mediaFile>")
            .appendLine("</manifest>")
            .toString()

        val doc = XFormParser.getXMLDocument(response.reader())
        val mediaFiles = OpenRosaResponseParserImpl().parseManifest(doc)!!
        assertThat(mediaFiles.size, equalTo(1))
        assertThat(mediaFiles[0].isEntityList, equalTo(false))
    }
}
