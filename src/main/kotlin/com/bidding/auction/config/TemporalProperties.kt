package com.bidding.auction.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("temporal")
data class TemporalProperties(
    val host: String = "localhost",
    val port: Int = 7233,
    val namespace: String = "default",
    val taskQueue: String = "auction-task-queue"
)
