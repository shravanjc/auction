package com.bidding.auction.domain.workflow

import io.temporal.activity.ActivityInterface

@ActivityInterface
interface AuctionActivities {
    fun markWinningBid(auctionId: String)
}
