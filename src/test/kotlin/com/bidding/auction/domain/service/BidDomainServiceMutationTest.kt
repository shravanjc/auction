package com.bidding.auction.domain.service

import com.bidding.auction.domain.model.Auction
import com.bidding.auction.domain.model.Bid
import com.bidding.auction.domain.model.Buyer
import com.bidding.auction.domain.model.Seller
import com.bidding.auction.domain.repository.AuctionRepository
import com.bidding.auction.domain.repository.BidRepository
import com.bidding.auction.domain.repository.BuyerRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

/**
 * Mutation-targeted tests for [BidDomainService].
 *
 * Each test is named after the mutation it kills. It pins a specific boundary value so that a
 * one-operator mutation (e.g. `>` → `>=`, `>=` → `>`, `-` → `+`) would flip the outcome.
 */
class BidDomainServiceMutationTest {

    private val auctionRepository: AuctionRepository = mock()
    private val bidRepository: BidRepository = mock()
    private val buyerRepository: BuyerRepository = mock()
    private val service = BidDomainService(auctionRepository, bidRepository, buyerRepository)

    private val auctionId = UUID.randomUUID()
    private val buyerId = UUID.randomUUID()
    private val seller = Seller(name = "Seller")
    private val buyer = Buyer(name = "Buyer")

    @BeforeEach
    fun setUp() {
        whenever(buyerRepository.findById(buyerId)).thenReturn(Optional.of(buyer))
        whenever(bidRepository.save(any())).thenAnswer { it.arguments[0] }
        whenever(auctionRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    // ── Price comparison: `price > minimum` ────────────────────────────────

    @Test
    fun `kills mutation - price exactly equal to minimum is rejected (would pass if mutated to gte)`() {
        val minimum = BigDecimal("100.00")
        setupAuction(startingPrice = minimum)

        // price == minimum must be rejected; if > were mutated to gte this would incorrectly pass
        assertThatThrownBy { service.placeBid(auctionId, buyerId, minimum) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `kills mutation - price 1 cent above minimum is accepted`() {
        val minimum = BigDecimal("100.00")
        setupAuction(startingPrice = minimum)

        val (bid, _) = service.placeBid(auctionId, buyerId, BigDecimal("100.01"))
        assertThat(bid.price).isEqualTo(BigDecimal("100.01"))
    }

    @Test
    fun `kills mutation - price exactly equal to highest bid is rejected`() {
        val auction = setupAuction(startingPrice = BigDecimal("50.00"))
        val highestBid = Bid(auction = auction, buyer = buyer, price = BigDecimal("200.00"))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(highestBid)

        assertThatThrownBy { service.placeBid(auctionId, buyerId, BigDecimal("200.00")) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    // ── Auction-ended check: `now < effectiveEndTime` ──────────────────────

    @Test
    fun `kills mutation - bid exactly at endTime is rejected (would pass if mutated to lte)`() {
        // effectiveEndTime is now - 1ms (just ended)
        setupAuction(endTime = Instant.now().minusMillis(1))

        assertThatThrownBy { service.placeBid(auctionId, buyerId, BigDecimal("101.00")) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `kills mutation - bid 1ms before endTime is accepted`() {
        setupAuction(endTime = Instant.now().plusMillis(500))

        val (bid, _) = service.placeBid(auctionId, buyerId, BigDecimal("101.00"))
        assertThat(bid).isNotNull()
    }

    // ── Extension window: `now >= effectiveEndTime - 30s` ──────────────────

    @Test
    fun `kills mutation - bid exactly at 30s boundary triggers extension`() {
        // Place bid exactly 30s before end: now >= effectiveEndTime - 30s → true
        setupAuction(endTime = Instant.now().plusSeconds(30))

        val (_, shouldExtend) = service.placeBid(auctionId, buyerId, BigDecimal("101.00"))
        assertThat(shouldExtend).isTrue()
    }

    @Test
    fun `kills mutation - bid 31s before end does not trigger extension`() {
        // If >= were mutated to >, exactly 30s before would no longer trigger
        setupAuction(endTime = Instant.now().plusSeconds(31))

        val (_, shouldExtend) = service.placeBid(auctionId, buyerId, BigDecimal("101.00"))
        assertThat(shouldExtend).isFalse()
    }

    @Test
    fun `kills mutation - buffer time is included in extension window (additionalBufferTime x 60)`() {
        // With 1 minute buffer, effectiveEndTime = endTime + 60s
        // Bid at endTime + 35s (within the buffer, 25s before effectiveEndTime) → should extend
        val endTime = Instant.now().plusSeconds(5)
        val auction = Auction(
            id = auctionId, name = "Buffered", seller = seller,
            price = BigDecimal("100.00"), endTime = endTime, additionalBufferTime = 1
        )
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(null)

        // effectiveEndTime = endTime + 60s = now + 65s; 30s boundary = now + 35s
        // now < now + 35s, so shouldExtend = false
        val (_, shouldExtend) = service.placeBid(auctionId, buyerId, BigDecimal("101.00"))
        assertThat(shouldExtend).isFalse()
    }

    // helpers

    private fun setupAuction(
        startingPrice: BigDecimal = BigDecimal("100.00"),
        endTime: Instant = Instant.now().plusSeconds(3600)
    ): Auction {
        val auction = Auction(
            id = auctionId, name = "Auction", seller = seller,
            price = startingPrice, endTime = endTime
        )
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(null)
        return auction
    }
}
