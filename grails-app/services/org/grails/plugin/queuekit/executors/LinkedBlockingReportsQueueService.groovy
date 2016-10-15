package org.grails.plugin.queuekit.executors

import org.grails.plugin.queuekit.LinkedBlockingReportsQueue
import org.grails.plugin.queuekit.ReportRunnable
import org.grails.plugin.queuekit.ReportsQueue
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

import java.util.concurrent.RunnableFuture

/**
 * LinkedBlockingReportsQueueService uses linkedBlockingExecutor to manage LinkedBlockingQueue 
 * LinkedBlockingQueue system self manages queue. This class extends queuekitExecutorBaseService 
 * nothing additional required here
 */
@Consumer
class LinkedBlockingReportsQueueService extends QueuekitExecutorBaseService {

	def linkedBlockingExecutor


	/*
	 * We are working with LinkedBlockingQueuedEvent which is a direct relation to
	 *  LinkedBlockingReportsQueue domainClass
	 *  
	 *  lets load up correct queue domainClass
	 */

	@Selector('method.linkedBlocking')
	void linkedBlocking(Long eventId) {
		log.info "Received ${eventId}"
		LinkedBlockingReportsQueue.withTransaction {
			LinkedBlockingReportsQueue queue = LinkedBlockingReportsQueue.read(eventId)
			if (queue && (queue.status == ReportsQueue.QUEUED || queue.status == ReportsQueue.ERROR)) {

				def useEmergencyExecutor = config.useEmergencyExecutor == true
				def manualDownloadEnabled = config.manualDownloadEnabled == true
				if ((linkedBlockingExecutor.isShutdown() || linkedBlockingExecutor.isTerminated()) &&
						(!useEmergencyExecutor || (useEmergencyExecutor &&
								(linkedBlockingExecutor.alternateExecutor.isShutdown() || linkedBlockingExecutor.alternateExecutor.isTerminated()))
								&& manualDownloadEnabled)) {

					setManualStatus(queue.id)
					executeManualReport(queue)

					return
				}


				ReportRunnable currentTask = new ReportRunnable(queue)
				/*
			 * This now calls the overridden execute method in LinkedBlockingExecutor
			 * which converts RunnableFuture (FutureTask) to ComparableFutureTask
			 * This then captures queueId for usage in cancellation
			 */
				RunnableFuture task = linkedBlockingExecutor.execute(currentTask, queue.id)
				task?.get()
			}
		}
	}
}