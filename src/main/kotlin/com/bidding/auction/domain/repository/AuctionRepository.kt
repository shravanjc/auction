package com.bidding.auction.domain.repository

import com.bidding.auction.domain.model.Auction
import java.util.Optional
import java.util.UUID

interface AuctionRepository {
    fun save(auction: Auction): Auction
    fun findById(id: UUID): Optional<Auction>
    fun findAll(): List<Auction>
}
