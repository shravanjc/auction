package com.bidding.auction.domain.service

import com.bidding.auction.domain.model.Auction
import com.bidding.auction.domain.model.Buyer
import com.bidding.auction.domain.model.Seller
import com.bidding.auction.domain.repository.AuctionRepository
import com.bidding.auction.domain.repository.BidRepository
import com.bidding.auction.domain.repository.BuyerRepository
import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.Combinators
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

/**
 * Property-based tests for [BidDomainService] covering invariants:
 * 1. Price ordering  — any bid <= current minimum is always rejected.
 * 2. Price ordering  — any bid strictly above minimum always succeeds (open auction).
 * 3. Auction closed  — any bid on an ended auction is always rejected regardless of price.
 * 4. Extension window — shouldExtend is always false when endTime is far in the future.
 */
class BidDomainServicePropertyTest {

    private val seller = Seller(name = "Seller")
    private val buyer = Buyer(name = "Buyer")
    private val buyerId: UUID = UUID.randomUUID()

    // Invariant 1: price <= minimum always rejected

    @Property
    fun `any bid at or below minimum is always rejected`(
        @ForAll("validPrices") minimum: BigDecimal,
        @ForAll("nonPositiveDeltas") delta: BigDecimal
    ) {
        val bidPrice = minimum.add(delta) // <= minimum
        val (service, auctionId) = serviceWithOpenAuction(startingPrice = minimum)

        assertThatCode { service.placeBid(auctionId, buyerId, bidPrice) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    // Invariant 2: price > minimum always accepted (open auction, no existing bids)

    @Property
    fun `any bid strictly above minimum always succeeds on open auction`(
        @ForAll("validPrices") minimum: BigDecimal,
        @ForAll("positiveDeltas") delta: BigDecimal
    ) {
        val bidPrice = minimum.add(delta) // > minimum
        val (service, auctionId) = serviceWithOpenAuction(startingPrice = minimum)

        assertThatCode { service.placeBid(auctionId, buyerId, bidPrice) }
            .doesNotThrowAnyException()
    }

    // Invariant 3: ended auction always rejects regardless of price

    @Property
    fun `any bid on ended auction is always rejected`(
        @ForAll("validPrices") bidPrice: BigDecimal
    ) {
        val (service, auctionId) = serviceWithEndedAuction()

        assertThatCode { service.placeBid(auctionId, buyerId, bidPrice) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    // Invariant 4: shouldExtend is always false when more than 30s remain

    @Property
    fun `shouldExtend is always false when bid arrives more than 30 seconds before close`(
        @ForAll("validPrices") minimum: BigDecimal,
        @ForAll("positiveDeltas") delta: BigDecimal
    ) {
        val bidPrice = minimum.add(delta)
        val (service, auctionId) = serviceWithOpenAuction(
            startingPrice = minimum,
            endTime = Instant.now().plusSeconds(3600) // well outside 30s window
        )

        val (_, shouldExtend) = service.placeBid(auctionId, buyerId, bidPrice)
        assertThat(shouldExtend).isFalse()
    }

    // ── Providers ──────────────────────────────────────────────────────────

    @Provide
    fun validPrices(): Arbitrary<BigDecimal> =
        Arbitraries.integers().between(1, 10_000)
            .map { BigDecimal.valueOf(it.toLong()) }

    @Provide
    fun positiveDeltas(): Arbitrary<BigDecimal> =
        Arbitraries.integers().between(1, 1_000)
            .map { BigDecimal.valueOf(it.toLong()) }

    @Provide
    fun nonPositiveDeltas(): Arbitrary<BigDecimal> =
        Arbitraries.integers().between(-1_000, 0)
            .map { BigDecimal.valueOf(it.toLong()) }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun serviceWithOpenAuction(
        startingPrice: BigDecimal = BigDecimal("100"),
        endTime: Instant = Instant.now().plusSeconds(3600)
    ): Pair<BidDomainService, UUID> {
        val auctionId = UUID.randomUUID()
        val auction = Auction(
            id = auctionId, name = "Auction", seller = seller,
            price = startingPrice, endTime = endTime
        )
        val auctionRepo = mock<AuctionRepository>()
        val bidRepo = mock<BidRepository>()
        val buyerRepo = mock<BuyerRepository>()

        whenever(auctionRepo.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepo.findHighestBid(auctionId)).thenReturn(null)
        whenever(buyerRepo.findById(buyerId)).thenReturn(Optional.of(buyer))
        whenever(bidRepo.save(any())).thenAnswer { it.arguments[0] }
        whenever(auctionRepo.save(any())).thenAnswer { it.arguments[0] }

        return BidDomainService(auctionRepo, bidRepo, buyerRepo) to auctionId
    }

    private fun serviceWithEndedAuction(): Pair<BidDomainService, UUID> {
        val auctionId = UUID.randomUUID()
        val auction = Auction(
            id = auctionId, name = "Ended", seller = seller,
            price = BigDecimal("100"), endTime = Instant.now().minusSeconds(1)
        )
        val auctionRepo = mock<AuctionRepository>()
        val bidRepo = mock<BidRepository>()
        val buyerRepo = mock<BuyerRepository>()

        whenever(auctionRepo.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(buyerRepo.findById(buyerId)).thenReturn(Optional.of(buyer))

        return BidDomainService(auctionRepo, bidRepo, buyerRepo) to auctionId
    }
}
