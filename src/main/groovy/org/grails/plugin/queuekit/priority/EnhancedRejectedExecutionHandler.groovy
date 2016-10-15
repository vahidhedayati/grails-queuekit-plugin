package org.grails.plugin.queuekit.priority

import grails.util.Holders

import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.RunnableFuture
import java.util.concurrent.ThreadPoolExecutor

import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class EnhancedRejectedExecutionHandler implements RejectedExecutionHandler {

	private boolean useEmergencyExecutor=(Holders.grailsApplication.config.queuekit?.useEmergencyExecutor==true ? true: false)

	private final Logger log = LoggerFactory.getLogger(getClass().name)

	/**
	 * Runnable is actually ComparableFutureTask
	 * which has been tooled up with queueId and
	 * original params
	 * 
	 * Now we can re-execute the job in a single thread when it is rejected i.e
	 * pool is broken/shutdown 
	 * 
	 */
	boolean runReport=false

	@Override
	public void rejectedExecution(Runnable worker, ThreadPoolExecutor executor) {

		/**
		 * There was no easy solution to starting the executor 
		 * when a manual request had been made to shut down the executor
		 * 
		 * To keep business process flowing you can set this in Config.groovy: 
		 * useEmergencyExecutor = true 
		 * 
		 * When it hits a rejectionHandler it will attempt to launch the alternateExecutor
		 * This is really a hack to bypass the shutdown of main Executor 
		 *
		 * To fix the underlying issue you need to restart your application.
		 * 
		 * This block as mentioned is emergency routine and imitates the execute
		 * process of ReportsService. 
		 * 
		 * The checks done to set status before / after job must also be done here
		 * 
		 */
		if (useEmergencyExecutor) {
			log.error worker.toString()+" rejected. Attempting to override execute "
			boolean hasException=false
			if (worker.queueId) {
				ReportsQueue queue = ReportsQueue.read(worker.queueId)
				new Thread({
					runReport=QueuekitBaseReportsService.verifyStatusBeforeStart(queue.id)
					if (runReport) {
						try{
							AttachedRunnable hookTask = new AttachedRunnable(queue,worker.paramsMap)
							RunnableFuture task = executor.alternateExecutor.submit(hookTask)
							task?.get()
						}catch(Exception e) {
							hasException=true
							log.error "Failed to override execute "+e.getMessage()
						}
						QueuekitBaseReportsService.setCompletedState(queue.id,hasException,queue.status,false)
						//executor.alternateExecutor.shutdown()
						log.error worker.toString()+" Override execution completed failed = ${hasException}"
					}
				} as Runnable ).start()
			}
		}
	}
}

