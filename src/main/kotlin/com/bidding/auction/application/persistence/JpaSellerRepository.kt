package com.bidding.auction.application.persistence

import com.bidding.auction.domain.model.Seller
import com.bidding.auction.domain.repository.SellerRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaSellerRepository : JpaRepository<Seller, UUID>, SellerRepository
