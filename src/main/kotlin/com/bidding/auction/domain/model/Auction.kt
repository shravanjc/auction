package com.bidding.auction.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
class Auction(
    @Id val id: UUID = UUID.randomUUID(),
    var name: String,
    @ManyToOne @JoinColumn(name = "seller_id") val seller: Seller,
    var price: BigDecimal,
    var endTime: Instant,
    var additionalBufferTime: Int = 0
) {
    fun getEndTimeWithBuffer(): Instant {
        return endTime.plusSeconds(additionalBufferTime * 60L)
    }
}
