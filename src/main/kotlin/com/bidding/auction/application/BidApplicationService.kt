package com.bidding.auction.application

import com.bidding.auction.domain.repository.AuctionRepository
import com.bidding.auction.domain.repository.BidRepository
import com.bidding.auction.domain.service.BidDomainService
import com.bidding.auction.domain.workflow.AuctionWorkflow
import com.bidding.auction.generated.api.model.BidRequest
import com.bidding.auction.generated.api.model.BidResponse
import com.bidding.auction.generated.api.model.WinningBidResponse
import io.temporal.client.WorkflowClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZoneOffset
import java.util.UUID

@Service
class BidApplicationService(
    private val bidDomainService: BidDomainService,
    private val bidRepository: BidRepository,
    private val auctionRepository: AuctionRepository,
    private val workflowClient: WorkflowClient
) {

    @Transactional
    fun placeBid(auctionId: UUID, request: BidRequest): BidResponse {
        val (bid, shouldExtend) = bidDomainService.placeBid(
            auctionId = auctionId,
            buyerId = request.buyerId,
            price = BigDecimal.valueOf(request.price)
        )

        if (shouldExtend) {
            val stub = workflowClient.newWorkflowStub(AuctionWorkflow::class.java, auctionId.toString())
            stub.extendEndTime()
        }

        return BidResponse(
            id = bid.id,
            auctionId = bid.auction.id,
            buyerId = bid.buyer.id,
            price = bid.price.toDouble(),
            timestamp = bid.timestamp.atOffset(ZoneOffset.UTC),
            winningBid = bid.isWinningBid
        )
    }

    fun getWinningBid(auctionId: UUID): WinningBidResponse {
        val auction = auctionRepository.findById(auctionId)
            .orElseThrow { NoSuchElementException("Auction not found: $auctionId") }
        val bid = bidRepository.findWinningBid(auctionId)
            ?: throw NoSuchElementException("No winning bid found for auction: $auctionId")

        val eventualEndTime = auction.endTime.plusSeconds(auction.additionalBufferTime * 60L)
        return WinningBidResponse(
            sellerId = auction.seller.id,
            name = auction.name,
            buyerId = bid.buyer.id,
            initialPrice = auction.price.toDouble(),
            winningPrice = bid.price.toDouble(),
            initialEndTime = auction.endTime.atOffset(ZoneOffset.UTC),
            eventualEndTime = eventualEndTime.atOffset(ZoneOffset.UTC)
        )
    }
}
