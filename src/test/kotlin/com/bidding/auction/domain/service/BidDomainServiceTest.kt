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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

class BidDomainServiceTest {

    private val auctionRepository: AuctionRepository = mock()
    private val bidRepository: BidRepository = mock()
    private val buyerRepository: BuyerRepository = mock()

    private val service = BidDomainService(auctionRepository, bidRepository, buyerRepository)

    private val auctionId = UUID.randomUUID()
    private val buyerId = UUID.randomUUID()
    private val seller = Seller(name = "Alice")
    private val buyer = Buyer(name = "Bob")

    @BeforeEach
    fun setUp() {
        whenever(buyerRepository.findById(buyerId)).thenReturn(Optional.of(buyer))
        whenever(bidRepository.save(any())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `placeBid succeeds when price is above starting price and no prior bids`() {
        val auction = openAuction(startingPrice = BigDecimal("100.00"))
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(null)

        val (bid, _) = service.placeBid(auctionId, buyerId, BigDecimal("101.00"))

        assertThat(bid.price).isEqualTo(BigDecimal("101.00"))
        assertThat(bid.buyer).isEqualTo(buyer)
    }

    @Test
    fun `placeBid succeeds when price is above existing highest bid`() {
        val auction = openAuction(startingPrice = BigDecimal("100.00"))
        val existingBid = bid(auction, price = BigDecimal("150.00"))
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(existingBid)

        val (bid, _) = service.placeBid(auctionId, buyerId, BigDecimal("151.00"))

        assertThat(bid.price).isEqualTo(BigDecimal("151.00"))
    }

    @Test
    fun `placeBid throws when price equals starting price`() {
        val auction = openAuction(startingPrice = BigDecimal("100.00"))
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(null)

        assertThatThrownBy { service.placeBid(auctionId, buyerId, BigDecimal("100.00")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("must be greater than current minimum")
    }

    @Test
    fun `placeBid throws when price is below existing highest bid`() {
        val auction = openAuction(startingPrice = BigDecimal("100.00"))
        val existingBid = bid(auction, price = BigDecimal("200.00"))
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(existingBid)

        assertThatThrownBy { service.placeBid(auctionId, buyerId, BigDecimal("199.00")) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `placeBid throws when auction has ended`() {
        val auction = openAuction(endTime = Instant.now().minusSeconds(1))
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))

        assertThatThrownBy { service.placeBid(auctionId, buyerId, BigDecimal("999.00")) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Auction has already ended")
    }

    @Test
    fun `placeBid signals extension and increments buffer when bid within last 30 seconds`() {
        val endTime = Instant.now().plusSeconds(20)
        val auction = openAuction(endTime = endTime)
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(null)

        val (_, shouldExtend) = service.placeBid(auctionId, buyerId, BigDecimal("101.00"))

        assertThat(shouldExtend).isTrue()
        assertThat(auction.additionalBufferTime).isEqualTo(1)
        verify(auctionRepository).save(auction)
    }

    @Test
    fun `placeBid does not signal extension when bid is outside last 30 seconds`() {
        val auction = openAuction(endTime = Instant.now().plusSeconds(60))
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(bidRepository.findHighestBid(auctionId)).thenReturn(null)

        val (_, shouldExtend) = service.placeBid(auctionId, buyerId, BigDecimal("101.00"))

        assertThat(shouldExtend).isFalse()
        assertThat(auction.additionalBufferTime).isEqualTo(0)
        verify(auctionRepository, never()).save(auction)
    }

    @Test
    fun `placeBid throws when auction not found`() {
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.empty())

        assertThatThrownBy { service.placeBid(auctionId, buyerId, BigDecimal("100.00")) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    fun `placeBid throws when buyer not found`() {
        val auction = openAuction()
        whenever(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction))
        whenever(buyerRepository.findById(buyerId)).thenReturn(Optional.empty())

        assertThatThrownBy { service.placeBid(auctionId, buyerId, BigDecimal("101.00")) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    // helpers

    private fun openAuction(
        startingPrice: BigDecimal = BigDecimal("100.00"),
        endTime: Instant = Instant.now().plusSeconds(3600)
    ) = Auction(
        id = auctionId, name = "Test Auction", seller = seller,
        price = startingPrice, endTime = endTime
    )

    private fun bid(auction: Auction, price: BigDecimal) =
        Bid(auction = auction, buyer = buyer, price = price)
}
