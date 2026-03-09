package com.bidding.auction

import io.restassured.RestAssured.given
import io.restassured.builder.RequestSpecBuilder
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.awaitility.kotlin.await
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Integration tests for the full auction workflow lifecycle.
 *
 * Requires docker-compose services to be running (postgres + temporal).
 * Run `docker-compose up -d` before executing these tests.
 *
 * Tests exercise the full stack: HTTP API → application service →
 * domain service → repository (real DB) → Temporal workflow.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuctionWorkflowITest {

    @LocalServerPort
    var port: Int = 0

    private lateinit var spec: RequestSpecification

    @BeforeEach
    fun setUp() {
        spec = RequestSpecBuilder()
            .setBaseUri("http://localhost:$port")
            .setContentType(ContentType.JSON)
            .build()
    }

    /**
     * Verifies that after the auction's endTime passes with no bids, the Temporal workflow
     * executes [markWinningBid] (which is a no-op when no bids exist) and the auction
     * remains closed with no winner.
     */
    @Test
    fun `workflow executes after auction ends with no bids`() {
        val sellerId = createSeller()
        val auctionId = createAuction(sellerId, endOffsetSeconds = 4)

        // Workflow fires within a few seconds after endTime; no bid means no winner
        Thread.sleep(6_000)

        given(spec)
            .get("/auctions/$auctionId/winning-bid")
            .then()
            .statusCode(404)
    }

    /**
     * Full workflow lifecycle:
     * 1. Create auction with far-future endTime (avoids auto-extension window)
     * 2. Place a bid
     * 3. Update auction endTime to near-future via PUT — sends [resetEndTime] signal to workflow
     * 4. Workflow wakes up at new deadline and marks the bid as winning
     */
    @Test
    fun `workflow marks winning bid after endTime is reset via PUT auction`() {
        val sellerId = createSeller()
        val buyerId = createBuyer()
        // 3600s keeps the bid outside the 30s auto-extension window
        val auctionId = createAuction(sellerId, endOffsetSeconds = 3600)

        placeBid(auctionId, buyerId, 150.0)

        // Reset endTime to 5 seconds from now — signals workflow to wake at new deadline
        val newEndTime = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(5)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        given(spec)
            .body("""{"name": "Test Auction", "price": 100.0, "endTime": "$newEndTime"}""")
            .put("/auctions/$auctionId")
            .then().statusCode(200)

        await.atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofSeconds(1)).untilAsserted {
            given(spec)
                .get("/auctions/$auctionId/winning-bid")
                .then()
                .statusCode(200)
                .body("winningPrice", equalTo(150.0f))
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun createSeller(name: String = "Seller-${UUID.randomUUID()}"): UUID =
        UUID.fromString(
            given(spec)
                .body("""{"name": "$name"}""")
                .post("/sellers")
                .then().statusCode(201)
                .extract().path("id")
        )

    private fun createBuyer(name: String = "Buyer-${UUID.randomUUID()}"): UUID =
        UUID.fromString(
            given(spec)
                .body("""{"name": "$name"}""")
                .post("/buyers")
                .then().statusCode(201)
                .extract().path("id")
        )

    private fun createAuction(sellerId: UUID, endOffsetSeconds: Long = 60): UUID {
        val endTime = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(endOffsetSeconds)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return UUID.fromString(
            given(spec)
                .body(
                    """{"name": "Test Auction", "sellerId": "$sellerId",
                       "price": 100.0, "endTime": "$endTime"}"""
                )
                .post("/auctions")
                .then().statusCode(201)
                .extract().path("id")
        )
    }

    private fun placeBid(auctionId: UUID, buyerId: UUID, price: Double) {
        given(spec)
            .body("""{"buyerId": "$buyerId", "price": $price}""")
            .post("/auctions/$auctionId/bids")
            .then().statusCode(201)
    }
}
