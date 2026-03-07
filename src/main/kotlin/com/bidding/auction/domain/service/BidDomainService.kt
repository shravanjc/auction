package com.bidding.auction.domain.service

import com.bidding.auction.domain.model.Bid
import com.bidding.auction.domain.repository.AuctionRepository
import com.bidding.auction.domain.repository.BidRepository
import com.bidding.auction.domain.repository.BuyerRepository
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class BidDomainService(
    private val auctionRepository: AuctionRepository,
    private val bidRepository: BidRepository,
    private val buyerRepository: BuyerRepository
) {

    /**
     * Places a bid and returns (savedBid, shouldSignalExtension).
     * shouldSignalExtension is true when the bid arrived within the last 30 seconds
     * of the effective auction end time, indicating the workflow should be signalled.
     */
    fun placeBid(auctionId: UUID, buyerId: UUID, price: BigDecimal): Pair<Bid, Boolean> {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NoSuchElementException("Auction not found: $auctionId") }
        val buyer = buyerRepository.findById(buyerId)
            .orElseThrow { NoSuchElementException("Buyer not found: $buyerId") }

        val effectiveEndTime = auction.getEndTimeWithBuffer()
        val now = Instant.now()

        check(now < effectiveEndTime) { "Auction has already ended" }

        val highestBid = bidRepository.findHighestBid(auctionId)
        val minimumPrice = highestBid?.price ?: auction.price

        require(price > minimumPrice) { "Bid price $price must be greater than current minimum $minimumPrice" }

        val bid = bidRepository.save(Bid(auction = auction, buyer = buyer, price = price))

        val shouldExtend = now >= effectiveEndTime.minusSeconds(30)
        if (shouldExtend) {
            auction.additionalBufferTime += 1
            auctionRepository.save(auction)
        }
        return Pair(bid, shouldExtend)
    }
}
