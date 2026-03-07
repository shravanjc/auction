package com.bidding.auction.domain.workflow

import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface AuctionWorkflow {
    @WorkflowMethod
    fun execute(auctionId: String, endTimeEpochMillis: Long)

    @SignalMethod
    fun extendEndTime()

    @SignalMethod
    fun resetEndTime(newEndTimeEpochMillis: Long)
}
