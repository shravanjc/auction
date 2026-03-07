package com.bidding.auction.domain.repository

import com.bidding.auction.domain.model.Seller
import java.util.Optional
import java.util.UUID

interface SellerRepository {
    fun save(seller: Seller): Seller
    fun findById(id: UUID): Optional<Seller>
    fun findAll(): List<Seller>
}
