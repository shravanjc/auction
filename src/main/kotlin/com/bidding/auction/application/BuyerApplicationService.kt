package com.bidding.auction.application

import com.bidding.auction.api.generated.model.BuyerRequest
import com.bidding.auction.api.generated.model.BuyerResponse
import com.bidding.auction.domain.model.Buyer
import com.bidding.auction.domain.repository.BuyerRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BuyerApplicationService(private val buyerRepository: BuyerRepository) {

    fun createBuyer(request: BuyerRequest): BuyerResponse =
        buyerRepository.save(Buyer(name = request.name)).toResponse()

    fun getBuyer(id: UUID): BuyerResponse =
        buyerRepository.findById(id)
            .orElseThrow { NoSuchElementException("Buyer not found: $id") }
            .toResponse()

    fun listBuyers(): List<BuyerResponse> =
        buyerRepository.findAll().map { it.toResponse() }

    private fun Buyer.toResponse() = BuyerResponse(id = id, name = name)
}
