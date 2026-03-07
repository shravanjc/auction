package com.bidding.auction.application.persistence

import com.bidding.auction.domain.model.Buyer
import com.bidding.auction.domain.repository.BuyerRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface JpaBuyerRepository : JpaRepository<Buyer, UUID>, BuyerRepository
