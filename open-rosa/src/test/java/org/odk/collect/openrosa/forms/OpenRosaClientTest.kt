package org.odk.collect.openrosa.forms

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.odk.collect.forms.FormSourceException
import org.odk.collect.forms.FormSourceException.FetchError
import org.odk.collect.forms.FormSourceException.SecurityError
import org.odk.collect.forms.FormSourceException.ServerNotOpenRosaError
import org.odk.collect.openrosa.http.HttpGetResult
import org.odk.collect.openrosa.http.OpenRosaConstants
import org.odk.collect.openrosa.http.OpenRosaHttpInterface
import org.odk.collect.openrosa.parse.OpenRosaResponseParser
import org.odk.collect.openrosa.support.StubWebCredentialsProvider
import java.io.ByteArrayInputStream
import java.net.SocketTimeoutException
import java.net.URI
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class OpenRosaClientTest {
    private val httpInterface = mock<OpenRosaHttpInterface>()
    private val webCredentialsProvider = StubWebCredentialsProvider()
    private val responseParser = mock<OpenRosaResponseParser>()

    @Test
    fun fetchFormList_removesTrailingSlashesFromUrl() {
        val formListApi = OpenRosaClient(
            "http://blah.com///",
            httpInterface,
            webCredentialsProvider,
            responseParser
        )

        whenever(
            httpInterface.executeGetRequest(any(), any(), any())
        ).thenReturn(
            HttpGetResult(
                ByteArrayInputStream(RESPONSE.toByteArray()),
                object : HashMap<String?, String?>() {
                    init {
                        put(OpenRosaConstants.VERSION_HEADER, "1.0")
                    }
                },
                "", 200
            )
        )

        formListApi.fetchFormList()
        verify(httpInterface).executeGetRequest(eq(URI("http://blah.com/formList")), any(), any())
    }

    @Test
    fun fetchFormList_whenThereIsAnUnknownHostException_throwsUnreachableFormApiException() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenThrow(
                UnknownHostException::class.java
            )

            formListApi.fetchFormList()
            fail("No exception thrown!")
        } catch (e: FormSourceException.Unreachable) {
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchFormList_whenThereIsAnSSLException_throwsSecurityErrorFormApiException() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenThrow(
                SSLException::class.java
            )

            formListApi.fetchFormList()
            fail("No exception thrown!")
        } catch (e: SecurityError) {
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchFormList_whenThereIsATimeout_throwsFetchError() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenThrow(
                SocketTimeoutException::class.java
            )

            formListApi.fetchFormList()
            fail("No exception thrown!")
        } catch (e: FetchError) {
            // pass
        }
    }

    @Test
    fun fetchFormList_whenThereIsA404_throwsUnreachableApiException() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenReturn(HttpGetResult(null, HashMap(), "hash", 404))

            formListApi.fetchFormList()
            fail("No exception thrown!")
        } catch (e: FormSourceException.Unreachable) {
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchFormList_whenThereIsAServerError_throwsServerError() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenReturn(HttpGetResult(null, HashMap(), "hash", 500))

            formListApi.fetchFormList()
            fail("No exception thrown!")
        } catch (e: FormSourceException.ServerError) {
            assertThat(e.statusCode, equalTo(500))
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchFormList_whenOpenRosaResponse_whenParserFails_throwsParseError() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenReturn(
                HttpGetResult(
                    ByteArrayInputStream("<xml></xml>".toByteArray()),
                    object : HashMap<String?, String?>() {
                        init {
                            put(OpenRosaConstants.VERSION_HEADER, "1.0")
                        }
                    },
                    "hash",
                    200
                )
            )

            whenever(responseParser.parseFormList(any())).thenReturn(null)
            formListApi.fetchFormList()
            fail("No exception thrown!")
        } catch (e: FormSourceException.ParseError) {
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchFormList_whenResponseHasNoOpenRosaHeader_throwsServerNotOpenRosaError() {
        val formListApi = OpenRosaClient(
            "http://blah.com///",
            httpInterface,
            webCredentialsProvider,
            responseParser
        )

        whenever(
            httpInterface.executeGetRequest(any(), any(), any())
        ).thenReturn(
            HttpGetResult(
                ByteArrayInputStream(RESPONSE.toByteArray()),
                HashMap(),
                "", 200
            )
        )

        try {
            formListApi.fetchFormList()
            fail("Expected exception because server is not OpenRosa server.")
        } catch (e: ServerNotOpenRosaError) {
            // pass
        }
    }

    @Test
    fun fetchManifest_whenThereIsAnUnknownHostException_throwsUnreachableFormApiException() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenThrow(
                UnknownHostException::class.java
            )

            formListApi.fetchManifest("http://blah.com/manifest")
            fail("No exception thrown!")
        } catch (e: FormSourceException.Unreachable) {
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchManifest_whenThereIsAServerError_throwsServerError() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenReturn(HttpGetResult(null, HashMap(), "hash", 503))

            formListApi.fetchManifest("http://blah.com/manifest")
            fail("No exception thrown!")
        } catch (e: FormSourceException.ServerError) {
            assertThat(e.statusCode, equalTo(503))
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchManifest_whenOpenRosaResponse_whenParserFails_throwsParseError() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenReturn(
                HttpGetResult(
                    ByteArrayInputStream("<xml></xml>".toByteArray()),
                    object : HashMap<String?, String?>() {
                        init {
                            put(OpenRosaConstants.VERSION_HEADER, "1.0")
                        }
                    },
                    "hash",
                    200
                )
            )

            whenever(responseParser.parseManifest(any())).thenReturn(null)
            formListApi.fetchManifest("http://blah.com/manifest")
            fail("No exception thrown!")
        } catch (e: FormSourceException.ParseError) {
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchManifest_whenNotOpenRosaResponse_throwsParseError() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenReturn(
                HttpGetResult(
                    ByteArrayInputStream("<xml></xml>".toByteArray()),
                    HashMap(),
                    "hash",
                    200
                )
            )

            formListApi.fetchManifest("http://blah.com/manifest")
            fail("No exception thrown!")
        } catch (e: FormSourceException.ParseError) {
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    fun fetchForm_whenThereIsAServerError_throwsServerError() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenReturn(HttpGetResult(null, HashMap(), "hash", 500))

            formListApi.fetchForm("http://blah.com/form")
            fail("No exception thrown!")
        } catch (e: FormSourceException.ServerError) {
            assertThat(e.statusCode, equalTo(500))
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    @Test
    @Throws(Exception::class)
    fun fetchMediaFile_whenThereIsAServerError_throwsServerError() {
        val formListApi =
            OpenRosaClient("http://blah.com", httpInterface, webCredentialsProvider, responseParser)

        try {
            whenever(
                httpInterface.executeGetRequest(any(), any(), any())
            ).thenReturn(HttpGetResult(null, HashMap(), "hash", 500))

            formListApi.fetchMediaFile("http://blah.com/mediaFile")
            fail("No exception thrown!")
        } catch (e: FormSourceException.ServerError) {
            assertThat(e.statusCode, equalTo(500))
            assertThat(e.serverUrl, equalTo("http://blah.com"))
        }
    }

    companion object {
        private fun join(vararg strings: String): String {
            val bob = StringBuilder()
            for (s in strings) {
                bob.append(s).append('\n')
            }
            return bob.toString()
        }

        private val RESPONSE = join(
            "<xforms xmlns=\"http://openrosa.org/xforms/xformsList\">",
            "<xform><formID>one</formID>",
            "<name>The First Form</name>",
            "<majorMinorVersion></majorMinorVersion>",
            "<version></version>",
            "<hash>md5:b71c92bec48730119eab982044a8adff</hash>",
            "<downloadUrl>https://example.com/formXml?formId=one</downloadUrl>",
            "</xform>",
            "<xform><formID>two</formID>",
            "<name>The Second Form</name>",
            "<majorMinorVersion></majorMinorVersion>",
            "<version></version>",
            "<hash>md5:4428adffbbec48771c9230119eab9820</hash>",
            "<downloadUrl>https://example.com/formXml?formId=two</downloadUrl>",
            "</xform>",
            "</xforms>"
        )
    }
}
