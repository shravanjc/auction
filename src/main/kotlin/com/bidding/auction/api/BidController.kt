package com.bidding.auction.api

import com.bidding.auction.api.generated.BidApi
import com.bidding.auction.api.generated.model.BidRequest
import com.bidding.auction.api.generated.model.BidResponse
import com.bidding.auction.api.generated.model.WinningBidResponse
import com.bidding.auction.application.BidApplicationService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class BidController(private val service: BidApplicationService) : BidApi {

    override fun placeBid(auctionId: UUID, bidRequest: BidRequest): ResponseEntity<BidResponse> =
        ResponseEntity.status(201).body(service.placeBid(auctionId, bidRequest))

    override fun getWinningBid(auctionId: UUID): ResponseEntity<WinningBidResponse> =
        ResponseEntity.ok(service.getWinningBid(auctionId))
}
