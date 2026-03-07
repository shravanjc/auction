package com.bidding.auction.api

import com.bidding.auction.api.generated.BuyerApi
import com.bidding.auction.api.generated.model.BuyerRequest
import com.bidding.auction.api.generated.model.BuyerResponse
import com.bidding.auction.application.BuyerApplicationService
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
