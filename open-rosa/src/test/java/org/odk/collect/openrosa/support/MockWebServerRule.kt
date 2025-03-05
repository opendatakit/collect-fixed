package org.odk.collect.openrosa.support

import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MockWebServerRule : TestRule {

    private val mockWebServers: MutableList<MockWebServer> = ArrayList()

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } finally {
                    teardown()
                }
            }
        }

    fun start(): MockWebServer {
        val mockWebServer = MockWebServer()
        mockWebServers.add(mockWebServer)

        mockWebServer.start()
        return mockWebServer
    }

    fun teardown() {
        for (mockWebServer in mockWebServers) {
            mockWebServer.close()
        }
    }
}
