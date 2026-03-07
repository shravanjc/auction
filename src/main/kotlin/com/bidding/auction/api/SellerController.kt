package com.bidding.auction.api

import com.bidding.auction.api.generated.SellerApi
import com.bidding.auction.api.generated.model.SellerRequest
import com.bidding.auction.api.generated.model.SellerResponse
import com.bidding.auction.application.SellerApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class SellerController(private val service: SellerApplicationService) : SellerApi {

    override fun listSellers(): ResponseEntity<List<SellerResponse>> =
        ResponseEntity.ok(service.listSellers())

    override fun createSeller(sellerRequest: SellerRequest): ResponseEntity<SellerResponse> =
        ResponseEntity.status(201).body(service.createSeller(sellerRequest))

    override fun getSeller(id: UUID): ResponseEntity<SellerResponse> =
        ResponseEntity.ok(service.getSeller(id))
}
