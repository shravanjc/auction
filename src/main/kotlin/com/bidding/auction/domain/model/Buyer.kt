package com.bidding.auction.domain.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.util.UUID

@Entity
class Buyer(
    @Id val id: UUID = UUID.randomUUID(),
    val name: String
)
