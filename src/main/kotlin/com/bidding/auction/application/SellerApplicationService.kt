package com.bidding.auction.application

import com.bidding.auction.api.generated.model.SellerRequest
import com.bidding.auction.api.generated.model.SellerResponse
import com.bidding.auction.domain.model.Seller
import com.bidding.auction.domain.repository.SellerRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class SellerApplicationService(private val sellerRepository: SellerRepository) {

    fun createSeller(request: SellerRequest): SellerResponse =
        sellerRepository.save(Seller(name = request.name)).toResponse()

    fun getSeller(id: UUID): SellerResponse =
        sellerRepository.findById(id)
            .orElseThrow { NoSuchElementException("Seller not found: $id") }
            .toResponse()

    fun listSellers(): List<SellerResponse> =
        sellerRepository.findAll().map { it.toResponse() }

    private fun Seller.toResponse() = SellerResponse(id = id, name = name)
}
