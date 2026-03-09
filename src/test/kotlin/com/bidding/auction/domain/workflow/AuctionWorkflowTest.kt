package com.bidding.auction.domain.workflow

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.testing.TestWorkflowEnvironment
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import java.time.Duration

private const val TASK_QUEUE = "auction-task-queue"

/**
 * Unit tests for [AuctionWorkflowImpl] using Temporal's in-process test environment.
 * [TestWorkflowEnvironment.sleep] fast-forwards virtual time so timers fire instantly.
 */
class AuctionWorkflowTest {

    private lateinit var testEnv: TestWorkflowEnvironment
    private lateinit var client: WorkflowClient
    private lateinit var activities: AuctionActivities

    @BeforeEach
    fun setUp() {
        activities = mock()
        testEnv = TestWorkflowEnvironment.newInstance()
        client = testEnv.workflowClient

        val worker = testEnv.newWorker(TASK_QUEUE)
        worker.registerWorkflowImplementationTypes(AuctionWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(activities)
        testEnv.start()
    }

    @AfterEach
    fun tearDown() = testEnv.close()

    @Test
    fun `workflow marks winning bid when deadline passes without signals`() {
        val endMillis = testEnv.currentTimeMillis() + Duration.ofMinutes(1).toMillis()
        val stub = newStub("wf-1")

        WorkflowClient.start(stub::execute, "auction-1", endMillis)
        testEnv.sleep(Duration.ofMinutes(2))

        verify(activities).markWinningBid("auction-1")
    }

    @Test
    fun `extendEndTime postpones closing by 1 minute`() {
        val endMillis = testEnv.currentTimeMillis() + Duration.ofMinutes(1).toMillis()
        val stub = newStub("wf-2")

        WorkflowClient.start(stub::execute, "auction-2", endMillis)

        testEnv.sleep(Duration.ofSeconds(50))
        stub.extendEndTime()                        // new deadline = original + 60s

        testEnv.sleep(Duration.ofSeconds(15))       // T+65s — original deadline passed
        verify(activities, never()).markWinningBid("auction-2")

        testEnv.sleep(Duration.ofSeconds(60))       // T+125s — extended deadline passed
        verify(activities).markWinningBid("auction-2")
    }

    @Test
    fun `resetEndTime moves deadline to new absolute time`() {
        val now = testEnv.currentTimeMillis()
        val endMillis = now + Duration.ofMinutes(1).toMillis()
        val stub = newStub("wf-3")

        WorkflowClient.start(stub::execute, "auction-3", endMillis)

        testEnv.sleep(Duration.ofSeconds(30))
        val newEndMillis = now + Duration.ofMinutes(2).toMillis()
        stub.resetEndTime(newEndMillis)             // extend to 2 minutes from start

        testEnv.sleep(Duration.ofSeconds(35))       // T+65s — original deadline passed
        verify(activities, never()).markWinningBid("auction-3")

        testEnv.sleep(Duration.ofSeconds(60))       // T+125s — new deadline passed
        verify(activities).markWinningBid("auction-3")
    }

    @Test
    fun `multiple extendEndTime signals each add 1 minute`() {
        val endMillis = testEnv.currentTimeMillis() + Duration.ofMinutes(1).toMillis()
        val stub = newStub("wf-4")

        WorkflowClient.start(stub::execute, "auction-4", endMillis)

        testEnv.sleep(Duration.ofSeconds(50))
        stub.extendEndTime()   // +1 min → 2 min total
        stub.extendEndTime()   // +1 min → 3 min total

        testEnv.sleep(Duration.ofSeconds(70))       // T+120s — 2-min mark passed
        verify(activities, never()).markWinningBid("auction-4")

        testEnv.sleep(Duration.ofSeconds(65))       // T+185s — 3-min mark passed
        verify(activities).markWinningBid("auction-4")
    }

    private fun newStub(workflowId: String): AuctionWorkflow =
        client.newWorkflowStub(
            AuctionWorkflow::class.java,
            WorkflowOptions.newBuilder()
                .setWorkflowId(workflowId)
                .setTaskQueue(TASK_QUEUE)
                .build()
        )
}
