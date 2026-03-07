package com.bidding.auction.domain.workflow

import io.temporal.activity.ActivityInterface
import io.temporal.activity.ActivityMethod

@ActivityInterface
fun interface AuctionActivities {
    @ActivityMethod
    fun markWinningBid(auctionId: String)
}
