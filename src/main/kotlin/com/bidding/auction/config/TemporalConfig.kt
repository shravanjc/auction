package com.bidding.auction.config

import com.bidding.auction.domain.workflow.AuctionActivities
import com.bidding.auction.domain.workflow.AuctionWorkflowImpl
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.serviceclient.RpcRetryOptions
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.SmartLifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
@EnableConfigurationProperties(TemporalProperties::class)
class TemporalConfig(
    private val properties: TemporalProperties,
    private val activitiesImpl: AuctionActivities
) {

    private val log = LoggerFactory.getLogger(TemporalConfig::class.java)

    @Bean
    fun workflowClient(): WorkflowClient {
        val service = WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget("${properties.host}:${properties.port}")
                .setRpcRetryOptions(
                    RpcRetryOptions.newBuilder()
                        .setExpiration(Duration.ofSeconds(60))
                        .setInitialInterval(Duration.ofMillis(500))
                        .setMaximumInterval(Duration.ofSeconds(5))
                        .build()
                )
                .build()
        )
        return WorkflowClient.newInstance(
            service,
            WorkflowClientOptions.newBuilder()
                .setNamespace(properties.namespace)
                .build()
        )
    }

    @Bean
    fun temporalWorker(workflowClient: WorkflowClient): SmartLifecycle {
        val factory = WorkerFactory.newInstance(workflowClient)
        val worker = factory.newWorker(properties.taskQueue)
        worker.registerWorkflowImplementationTypes(AuctionWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(activitiesImpl)

        return object : SmartLifecycle {
            @Volatile private var running = false

            override fun start() {
                Thread({
                    var attempt = 0
                    while (!running) {
                        try {
                            factory.start()
                            running = true
                            log.info("Temporal worker started (queue: ${properties.taskQueue})")
                        } catch (e: Exception) {
                            attempt++
                            log.warn(
                                "Temporal not reachable at ${properties.host}:${properties.port} " +
                                "(attempt $attempt), retrying in 5s — ${e.message}"
                            )
                            Thread.sleep(5_000)
                        }
                    }
                }, "temporal-worker-init").also { it.isDaemon = true }.start()
            }

            override fun stop() {
                if (running) {
                    factory.shutdown()
                    running = false
                }
            }

            override fun isRunning() = running
        }
    }
}
