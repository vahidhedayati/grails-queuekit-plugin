package org.grails.plugin.queuekit

import grails.util.Holders

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import org.grails.plugin.queuekit.priority.ComparableFutureTask

/**
 *
 * ArrayBlockingExecutor extends ThreadExecutor and
 * implements ArrayBlockingQueue mechanism
 *
 * This does not manage the queue instead relies on database interaction
 * to manage the queue
 *
 * execute command overridden to capture queueId for cancellation
 * of queued Items
 *
 * runningTasks captured to enable deletion of running tasks
 *
 * @author Vahid Hedayati
 *
 */

class ArrayBlockingExecutor extends ThreadPoolExecutor {
	
	private static long keepAliveTime=((Holders.grailsApplication.config.queuekit?.keepAliveTime ?: 300) as Long)
	
	private static TimeUnit timeoutUnit=TimeUnit.SECONDS

	/*
	 * Set the size of your corePoolSize this is your core/max size defined in one
	 */
	private static int corePoolSize = Holders.grailsApplication.config.queuekit?.corePoolSize ?: 1

	/*
	 * Set the size of your corePoolSize this is your core/max size defined in one
	 */
	private static int maximumPoolSize = Holders.grailsApplication.config.queuekit?.maximumPoolSize ?: 3
	
	private static int maxQueue = Holders.grailsApplication.config.queuekit.maxQueue?:100
	
	
	/**
	 * Main call
	 */
	public ArrayBlockingExecutor() {
		super(corePoolSize,maximumPoolSize,keepAliveTime,timeoutUnit,
			new ArrayBlockingQueue<Runnable>(maxQueue))
		/**
		 * ArrayBlocking has not been configured to bypass to EnhancedRejectedExecutionHandler
		 * When items are sent to ArrayBlocking, the mechanism has no queueing intelligence and  
		 *  will automatically reject tasks that exceed available limit.
		 * Refer to ArrayBlockingReportsQueueService which has a try catch and runs resetRequeueDate(queue.id)
		 * 
		 * Enabling this feature would directly conflict with that method and therefore bypass actual queue.
		 */
	}
	
	
	/**
	 * Override execute method so to capture QueueId
	 * used for job cancellation
	 *
	 * @param command
	 * @param queueId
	 * @return
	 */
	public ComparableFutureTask execute(Runnable command, Long queueId) {
		ComparableFutureTask task = new ComparableFutureTask(command,null)
		super.execute(task)
	}
}