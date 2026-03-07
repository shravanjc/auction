package com.bidding.auction.application.persistence

import com.bidding.auction.domain.model.Bid
import com.bidding.auction.domain.repository.BidRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaBidRepository : JpaRepository<Bid, UUID>, BidRepository {

    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId ORDER BY b.price DESC LIMIT 1")
    override fun findHighestBid(auctionId: UUID): Bid?

    @Query("SELECT b FROM Bid b WHERE b.auction.id = :auctionId AND b.isWinningBid = true")
    override fun findWinningBid(auctionId: UUID): Bid?
}
