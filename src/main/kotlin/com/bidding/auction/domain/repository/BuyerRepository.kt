package com.bidding.auction.domain.repository

import com.bidding.auction.domain.model.Buyer
import java.util.Optional
import java.util.UUID

interface BuyerRepository {
    fun save(buyer: Buyer): Buyer
    fun findById(id: UUID): Optional<Buyer>
    fun findAll(): List<Buyer>
}
