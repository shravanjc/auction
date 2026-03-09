package com.bidding.auction.api

import com.bidding.auction.application.BuyerApplicationService
import com.bidding.auction.generated.api.BuyerApi
import com.bidding.auction.generated.api.model.BuyerRequest
import com.bidding.auction.generated.api.model.BuyerResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class BuyerController(private val service: BuyerApplicationService) : BuyerApi {

    override fun listBuyers(): ResponseEntity<List<BuyerResponse>> =
        ResponseEntity.ok(service.listBuyers())

    override fun createBuyer(buyerRequest: BuyerRequest): ResponseEntity<BuyerResponse> =
        ResponseEntity.status(201).body(service.createBuyer(buyerRequest))

    override fun getBuyer(id: UUID): ResponseEntity<BuyerResponse> =
        ResponseEntity.ok(service.getBuyer(id))
}
