package org.grails.plugin.queuekit.executors

import java.util.concurrent.RunnableFuture

import org.grails.plugin.queuekit.ComparableRunnable
import org.grails.plugin.queuekit.PriorityBlockingReportsQueue
import org.grails.plugin.queuekit.ReportRunnable
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.event.PriorityBlockingQueuedEvent
import org.grails.plugin.queuekit.priority.Priority
import org.springframework.context.ApplicationListener

/**
 * Priority Blocking uses priorityBlockingExecutor to manage report queue.
 * This is the default listener provided by the plugin and report priority can be either provided 
 * per call or by Config.groovy
 * 
 */
class PriorityBlockingReportsQueueService extends QueuekitExecutorBaseService  implements ApplicationListener<PriorityBlockingQueuedEvent> {

	def priorityBlockingExecutor

	void onApplicationEvent(PriorityBlockingQueuedEvent event) {
		log.debug "Received ${event.source}"

		/*
		 * We are working with PriorityBlockingQueuedEvent which is a direct relation to
		 *  PriorityBlockingReportsQueue domainClass
		 *  in PriorityBlockingReportsQueue we have additional fields 
		 *  lets load up correct queue domainClass
		 */
		PriorityBlockingReportsQueue queue=PriorityBlockingReportsQueue.read(event.source)
		if (queue && (queue.status==ReportsQueue.QUEUED||queue.status==ReportsQueue.ERROR)) {

			def useEmergencyExecutor = (config.useEmergencyExecutor && config.useEmergencyExecutor == true)
			def manualDownloadEnabled = (config.manualDownloadEnabled &&  config.manualDownloadEnabled == true)

			if ((priorityBlockingExecutor.isShutdown() || priorityBlockingExecutor.isTerminated()) &&
			(!useEmergencyExecutor || (useEmergencyExecutor &&
			(priorityBlockingExecutor.alternateExecutor.isShutdown() || priorityBlockingExecutor.alternateExecutor.isTerminated()))
			&& manualDownloadEnabled)){
				log.error "priorityBlockingExecutor and the alternative executor not responding triggering manual download"
				setManualStatus(queue.id)
				executeManualReport(queue)

				return
			}

			Priority priority = queue.priority ?: queue.defaultPriority

			def currentTask
			RunnableFuture task
			if (config.standardRunnable) {
				currentTask = new ReportRunnable(queue)
				/*
				 * This now calls the overridden execute method in PriorityBlockingExecutor
				 * which converts RunnableFuture (FutureTask) to ComparableFutureTask
				 * This then captures queueId for usage in cancellation
				 *
				 * This uses advanced features of ComparableFutureTask since priority value is provided
				 * and a different block is called in PriorityBlockingExecutor
				 */

				task = priorityBlockingExecutor.execute(currentTask,priority.value)
			} else {
				currentTask = new ComparableRunnable(queue)
				task = priorityBlockingExecutor.execute(currentTask)
			}

			task?.get()
		}
	}
}