package org.oremif.deepseek.api

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import org.oremif.deepseek.models.CurrencyType
import org.oremif.deepseek.testing.mockEngine
import org.oremif.deepseek.testing.testClient
import kotlin.test.Test

class EndpointApiTests {

    @Test
    fun `userBalance GETs the balance endpoint and parses the payload`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null
        val engine = mockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            respond(
                content = """
                    {
                        "is_available": true,
                        "balance_infos": [
                            {
                                "currency": "CNY",
                                "total_balance": "110.00",
                                "granted_balance": "10.00",
                                "topped_up_balance": "100.00"
                            },
                            {
                                "currency": "USD",
                                "total_balance": "10.00",
                                "granted_balance": "5.00",
                                "topped_up_balance": "5.00"
                            }
                        ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        val balance = client.userBalance()

        capturedMethod shouldBe HttpMethod.Get
        capturedPath.shouldNotBeNull().shouldEndWith("/user/balance")
        balance.isAvailable.shouldBeTrue()
        balance.balanceInfos shouldHaveSize 2
        val cny = balance.balanceInfos[0]
        cny.currency shouldBe CurrencyType.CNY
        cny.totalBalance shouldBe "110.00"
        cny.grantedBalance shouldBe "10.00"
        cny.toppedUpBalance shouldBe "100.00"
        val usd = balance.balanceInfos[1]
        usd.currency shouldBe CurrencyType.USD
        usd.totalBalance shouldBe "10.00"
    }

    @Test
    fun `models GETs the models endpoint and parses the list`() = runTest {
        var capturedMethod: HttpMethod? = null
        var capturedPath: String? = null
        val engine = mockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            respond(
                content = """
                    {
                        "object": "list",
                        "data": [
                            {"id": "deepseek-v4-flash", "object": "model", "owned_by": "deepseek"},
                            {"id": "deepseek-v4-pro", "object": "model", "owned_by": "deepseek"}
                        ]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val client = testClient(engine)

        val models = client.models()

        capturedMethod shouldBe HttpMethod.Get
        capturedPath.shouldNotBeNull().shouldEndWith("/models")
        models.`object` shouldBe "list"
        models.data shouldHaveSize 2
        models.data.map { it.id } shouldBe listOf("deepseek-v4-flash", "deepseek-v4-pro")
    }
}
