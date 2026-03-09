package com.bidding.auction.api

import com.bidding.auction.application.AuctionApplicationService
import com.bidding.auction.generated.api.AuctionApi
import com.bidding.auction.generated.api.model.AuctionRequest
import com.bidding.auction.generated.api.model.AuctionResponse
import com.bidding.auction.generated.api.model.AuctionUpdateRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AuctionController(private val service: AuctionApplicationService) : AuctionApi {

    override fun updateAuction(id: UUID, auctionUpdateRequest: AuctionUpdateRequest): ResponseEntity<AuctionResponse> =
        ResponseEntity.ok(service.updateAuction(id, auctionUpdateRequest))

    override fun listAuctions(): ResponseEntity<List<AuctionResponse>> =
        ResponseEntity.ok(service.listAuctions())

    override fun createAuction(auctionRequest: AuctionRequest): ResponseEntity<AuctionResponse> =
        ResponseEntity.status(201).body(service.createAuction(auctionRequest))

    override fun getAuction(id: UUID): ResponseEntity<AuctionResponse> =
        ResponseEntity.ok(service.getAuction(id))
}
