package com.bidding.auction.domain.repository

import com.bidding.auction.domain.model.Bid
import java.util.UUID

interface BidRepository {
    fun save(bid: Bid): Bid
    fun findHighestBid(auctionId: UUID): Bid?
    fun findWinningBid(auctionId: UUID): Bid?
}
