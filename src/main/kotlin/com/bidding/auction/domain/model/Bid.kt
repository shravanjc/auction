package com.bidding.auction.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
class Bid(
    @Id val id: UUID = UUID.randomUUID(),
    @ManyToOne @JoinColumn(name = "auction_id") val auction: Auction,
    @ManyToOne @JoinColumn(name = "buyer_id") val buyer: Buyer,
    val price: BigDecimal,
    val timestamp: Instant = Instant.now(),
    var isWinningBid: Boolean = false
)
