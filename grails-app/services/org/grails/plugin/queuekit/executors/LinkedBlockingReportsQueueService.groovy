package org.grails.plugin.queuekit.executors

import java.util.concurrent.RunnableFuture

import org.grails.plugin.queuekit.ReportRunnable
import org.grails.plugin.queuekit.LinkedBlockingReportsQueue
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.event.LinkedBlockingQueuedEvent
import org.springframework.context.ApplicationListener

/**
 * LinkedBlockingReportsQueueService uses linkedBlockingExecutor to manage LinkedBlockingQueue 
 * LinkedBlockingQueue system self manages queue. This class extends queuekitExecutorBaseService 
 * nothing additional required here
 */
class LinkedBlockingReportsQueueService extends QueuekitExecutorBaseService  implements ApplicationListener<LinkedBlockingQueuedEvent> {

	def linkedBlockingExecutor


	/*
	 * We are working with LinkedBlockingQueuedEvent which is a direct relation to
	 *  LinkedBlockingReportsQueue domainClass
	 *  
	 *  lets load up correct queue domainClass
	 */

	void onApplicationEvent(LinkedBlockingQueuedEvent event) {
		log.debug "Received ${event.source}"
		LinkedBlockingReportsQueue queue=LinkedBlockingReportsQueue.read(event.source)
		if (queue && (queue.status==ReportsQueue.QUEUED||queue.status==ReportsQueue.ERROR)) {

			def useEmergencyExecutor = (config.useEmergencyExecutor && config.useEmergencyExecutor == true)
			def manualDownloadEnabled = (config.manualDownloadEnabled &&  config.manualDownloadEnabled == true)
			
			if ((linkedBlockingExecutor.isShutdown() || linkedBlockingExecutor.isTerminated()) &&
			(!useEmergencyExecutor || (useEmergencyExecutor &&
			(linkedBlockingExecutor.alternateExecutor.isShutdown() || linkedBlockingExecutor.alternateExecutor.isTerminated()))
			&& manualDownloadEnabled)){

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
			RunnableFuture task = linkedBlockingExecutor.execute(currentTask,queue.id)
			task?.get()
		}
	}
}