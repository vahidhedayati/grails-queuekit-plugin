package org.grails.plugin.queuekit.executors

import org.grails.plugin.queuekit.ComparableRunnable
import org.grails.plugin.queuekit.EnhancedPriorityBlockingReportsQueue
import org.grails.plugin.queuekit.ReportRunnable
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.Priority
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

import java.util.concurrent.RunnableFuture

/**
 * Priority Blocking uses priorityBlockingExecutor to manage report queue.
 * This is the default listener provided by the plugin and report priority can be either provided 
 * per call or by Config.groovy
 * 
 * 
 */

@Consumer
class EnhancedPriorityBlockingReportsQueueService extends QueuekitExecutorBaseService  {

	def enhancedPriorityBlockingExecutor

	@Selector('method.enhancedPriorityBlocking')
	void linkedBlocking(Long eventId) {
		log.debug "Received ${eventId}"
		/*
		 * We are working with EnhancedPriorityBlockingQueuedEvent which is a direct relation to
		 *  EnhancedPriorityBlockingReportsQueue domainClass
		 *  in EnhancedPriorityBlockingReportsQueue we have additional fields 
		 *  lets load up correct queue domainClass
		 */
		EnhancedPriorityBlockingReportsQueue.withTransaction {
			EnhancedPriorityBlockingReportsQueue queue = EnhancedPriorityBlockingReportsQueue.read(eventId)
			if (queue && (queue.status == ReportsQueue.QUEUED || queue.status == ReportsQueue.ERROR)) {

				def useEmergencyExecutor = (config.useEmergencyExecutor && config.useEmergencyExecutor == true)
				def manualDownloadEnabled = (config.manualDownloadEnabled &&  config.manualDownloadEnabled == true)

				if ((enhancedPriorityBlockingExecutor.isShutdown() || enhancedPriorityBlockingExecutor.isTerminated()) &&
						(!useEmergencyExecutor || (useEmergencyExecutor &&
								(enhancedPriorityBlockingExecutor.alternateExecutor.isShutdown() || enhancedPriorityBlockingExecutor.alternateExecutor.isTerminated()))
								&& manualDownloadEnabled)) {
					log.error "enhancedPriorityBlockingExecutor and alternative executor not responding triggering manual download"
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
					task = enhancedPriorityBlockingExecutor.execute(currentTask,priority.value)
				} else {
					currentTask = new ComparableRunnable(queue)
					task = enhancedPriorityBlockingExecutor.execute(currentTask)
				}

				task?.get()
			}
		}
	}

}
