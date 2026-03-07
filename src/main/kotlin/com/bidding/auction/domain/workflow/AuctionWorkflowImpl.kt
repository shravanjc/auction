package com.bidding.auction.domain.workflow

import io.temporal.activity.ActivityOptions
import io.temporal.workflow.Workflow
import java.time.Duration

class AuctionWorkflowImpl : AuctionWorkflow {

    private val log = Workflow.getLogger(AuctionWorkflowImpl::class.java)

    private var endTimeMillis: Long = 0
    private var version: Int = 0

    private val activities = Workflow.newActivityStub(
        AuctionActivities::class.java,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(30))
            .build()
    )

    override fun execute(auctionId: String, endTimeEpochMillis: Long) {
        endTimeMillis = endTimeEpochMillis

        while (true) {
            val snapshotVersion = version
            val remainingMs = endTimeMillis - Workflow.currentTimeMillis()

            val timeout = Duration.ofMillis(remainingMs)
            log.info("Auction $auctionId to auto close in $timeout")
            if (remainingMs <= 0) break

            val signalReceived = Workflow.await(timeout) {
                //inside the Workflow await/lambda, the value of snapshotVersion will be the old one as its val/immutable.
                // It is compared against version that is mutable and is changed via a signal: extendEndTime and resetEndTime
                version > snapshotVersion
            }
            if (!signalReceived) break
        }
        log.info("Marking auction $auctionId as winning")
        activities.markWinningBid(auctionId)
    }

    override fun extendEndTime() {
        log.info("Extending auction timetime by 1 min")
        endTimeMillis += 60_000L
        version++
    }

    override fun resetEndTime(newEndTimeEpochMillis: Long) {
        log.info("Auction timetime by updated")
        endTimeMillis = newEndTimeEpochMillis
        version++
    }
}
