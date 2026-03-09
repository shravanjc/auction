package com.bidding.auction.application

import com.bidding.auction.config.TemporalProperties
import com.bidding.auction.domain.model.Auction
import com.bidding.auction.domain.repository.AuctionRepository
import com.bidding.auction.domain.repository.SellerRepository
import com.bidding.auction.domain.workflow.AuctionWorkflow
import com.bidding.auction.generated.api.model.AuctionRequest
import com.bidding.auction.generated.api.model.AuctionResponse
import com.bidding.auction.generated.api.model.AuctionUpdateRequest
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.ZoneOffset
import java.util.UUID

@Service
class AuctionApplicationService(
    private val auctionRepository: AuctionRepository,
    private val sellerRepository: SellerRepository,
    private val workflowClient: WorkflowClient,
    private val temporalProperties: TemporalProperties
) {

    fun createAuction(request: AuctionRequest): AuctionResponse {
        val seller = sellerRepository.findById(request.sellerId)
            .orElseThrow { NoSuchElementException("Seller not found: ${request.sellerId}") }
        val auction = auctionRepository.save(
            Auction(
                name = request.name,
                seller = seller,
                price = BigDecimal.valueOf(request.price),
                endTime = request.endTime.toInstant()
            )
        )

        val stub = workflowClient.newWorkflowStub(
            AuctionWorkflow::class.java,
            WorkflowOptions.newBuilder()
                .setWorkflowId(auction.id.toString())
                .setTaskQueue(temporalProperties.taskQueue)
                .build()
        )
        WorkflowClient.start(stub::execute, auction.id.toString(), auction.endTime.toEpochMilli())

        return auction.toResponse()
    }

    fun updateAuction(id: UUID, request: AuctionUpdateRequest): AuctionResponse {
        val auction = auctionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Auction not found: $id") }
            .apply {
                name = request.name
                price = BigDecimal.valueOf(request.price)
                endTime = request.endTime.toInstant()
            }
        auctionRepository.save(auction)

        val stub = workflowClient.newWorkflowStub(AuctionWorkflow::class.java, id.toString())
        stub.resetEndTime(auction.getEndTimeWithBuffer().toEpochMilli())

        return auction.toResponse()
    }

    fun getAuction(id: UUID): AuctionResponse =
        auctionRepository.findById(id)
            .orElseThrow { NoSuchElementException("Auction not found: $id") }
            .toResponse()

    fun listAuctions(): List<AuctionResponse> =
        auctionRepository.findAll().map { it.toResponse() }

    private fun Auction.toResponse() = AuctionResponse(
        id = id,
        name = name,
        sellerId = seller.id,
        price = price.toDouble(),
        endTime = endTime.atOffset(ZoneOffset.UTC),
        additionalBufferTime = additionalBufferTime
    )
}
