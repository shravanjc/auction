package com.bidding.auction.domain.workflow

import com.bidding.auction.domain.repository.BidRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class AuctionActivitiesImpl(
    private val bidRepository: BidRepository
) : AuctionActivities {

    private val log = LoggerFactory.getLogger(AuctionActivitiesImpl::class.java)

    override fun markWinningBid(auctionId: String) {
        val bid = bidRepository.findHighestBid(UUID.fromString(auctionId)) ?: return

        log.info("Marking $auctionId as winning for ${bid.id}")
        bid.isWinningBid = true
        bidRepository.save(bid)
    }
}
