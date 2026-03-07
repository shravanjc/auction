package com.bidding.auction.config

import com.bidding.auction.domain.repository.AuctionRepository
import com.bidding.auction.domain.repository.BidRepository
import com.bidding.auction.domain.repository.BuyerRepository
import com.bidding.auction.domain.service.BidDomainService
import com.bidding.auction.domain.workflow.AuctionActivities
import com.bidding.auction.domain.workflow.AuctionActivitiesImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DomainConfig {

    @Bean
    fun bidDomainService(
        auctionRepository: AuctionRepository,
        bidRepository: BidRepository,
        buyerRepository: BuyerRepository
    ) = BidDomainService(auctionRepository, bidRepository, buyerRepository)

    @Bean
    fun auctionActivities(bidRepository: BidRepository): AuctionActivities =
        AuctionActivitiesImpl(bidRepository)
}
