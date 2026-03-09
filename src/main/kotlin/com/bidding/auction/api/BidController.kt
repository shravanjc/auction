package com.bidding.auction.api

import com.bidding.auction.application.BidApplicationService
import com.bidding.auction.generated.api.BidApi
import com.bidding.auction.generated.api.model.BidRequest
import com.bidding.auction.generated.api.model.BidResponse
import com.bidding.auction.generated.api.model.WinningBidResponse
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
