package com.bidding.auction.api

import com.bidding.auction.application.BidApplicationService
import com.bidding.auction.generated.api.model.BidResponse
import com.bidding.auction.generated.api.model.WinningBidResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Contract tests for [BidController]: verifies HTTP mapping, status codes and response shape
 * using standalone MockMvc. No Spring context is loaded.
 */
@ExtendWith(MockitoExtension::class)
class BidControllerTest {

    @Mock private lateinit var service: BidApplicationService
    @InjectMocks private lateinit var controller: BidController

    private lateinit var mockMvc: MockMvc

    private val auctionId: UUID = UUID.randomUUID()
    private val buyerId: UUID = UUID.randomUUID()
    private val bidId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(ErrorControllerAdvice())
            .build()
    }

    @Test
    fun `placeBid returns 201 with bid details`() {
        val response = BidResponse(
            id = bidId, auctionId = auctionId, buyerId = buyerId,
            price = 150.0, timestamp = OffsetDateTime.now(), winningBid = false
        )
        whenever(service.placeBid(eq(auctionId), any())).thenReturn(response)

        mockMvc.perform(
            post("/auctions/{auctionId}/bids", auctionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "buyerId": "$buyerId", "price": 150.0 }""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(bidId.toString()))
            .andExpect(jsonPath("$.price").value(150.0))
            .andExpect(jsonPath("$.winningBid").value(false))
    }

    @Test
    fun `placeBid returns 400 when bid price is too low`() {
        whenever(service.placeBid(eq(auctionId), any()))
            .thenThrow(IllegalArgumentException("Bid price 5.0 must be greater than current minimum 10.0"))

        mockMvc.perform(
            post("/auctions/{auctionId}/bids", auctionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "buyerId": "$buyerId", "price": 5.0 }""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.statusCode").value(400))
            .andExpect(jsonPath("$.message").value("Bid price 5.0 must be greater than current minimum 10.0"))
    }

    @Test
    fun `placeBid returns 409 when auction has ended`() {
        whenever(service.placeBid(eq(auctionId), any()))
            .thenThrow(IllegalStateException("Auction has already ended"))

        mockMvc.perform(
            post("/auctions/{auctionId}/bids", auctionId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{ "buyerId": "$buyerId", "price": 100.0 }""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.statusCode").value(409))
    }

    @Test
    fun `getWinningBid returns 200 with winner details`() {
        val response = WinningBidResponse(
            sellerId = UUID.randomUUID(), name = "Rare Watch",
            buyerId = buyerId, initialPrice = 100.0, winningPrice = 250.0,
            initialEndTime = OffsetDateTime.now(), eventualEndTime = OffsetDateTime.now()
        )
        whenever(service.getWinningBid(auctionId)).thenReturn(response)

        mockMvc.perform(get("/auctions/{auctionId}/winning-bid", auctionId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.winningPrice").value(250.0))
            .andExpect(jsonPath("$.name").value("Rare Watch"))
    }

    @Test
    fun `getWinningBid returns 404 when no winner yet`() {
        whenever(service.getWinningBid(auctionId))
            .thenThrow(NoSuchElementException("No winning bid found for auction: $auctionId"))

        mockMvc.perform(get("/auctions/{auctionId}/winning-bid", auctionId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.statusCode").value(404))
    }
}
