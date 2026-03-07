package com.bidding.auction.application.persistence

import com.bidding.auction.domain.model.Auction
import com.bidding.auction.domain.repository.AuctionRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaAuctionRepository : JpaRepository<Auction, UUID>, AuctionRepository
