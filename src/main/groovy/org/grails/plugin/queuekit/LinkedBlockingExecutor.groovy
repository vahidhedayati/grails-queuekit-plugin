package org.grails.plugin.queuekit

import grails.util.Holders

import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import org.grails.plugin.queuekit.priority.ComparableFutureTask
import org.grails.plugin.queuekit.priority.EnhancedRejectedExecutionHandler

/**
 * 
 * LinkedBlockExecutor extends ThreadExecutor and 
 * implements LinkedBlockingQueue mechanism
 * 
 * This auto manages queue
 * 
 * execute command overridden to capture queueId for cancellation 
 * of queued Items
 * 
 * runningTasks captured to enable deletion of running tasks
 * 
 * @author Vahid Hedayati
 *
 */
class LinkedBlockingExecutor extends ThreadPoolExecutor {
	
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
	 * A test method created after having a play around with shutdown feature
	 *
	 * It appears when shutdown has been triggered since this Exectuor is injected and
	 * behaves like a grails service. The shutdown causes entire centralised executor
	 * process service to reject all new tasks.
	 *
	 */
	public static ThreadPoolExecutor alternateExecutor=(ThreadPoolExecutor) Executors.newFixedThreadPool(1)
	//Opublic static ThreadPoolExecutor alternateExecutor=(ThreadPoolExecutor) Executors.newFixedThreadPool(maxPoolSize)
	
	
	public LinkedBlockingExecutor() {
		super(corePoolSize,maximumPoolSize,keepAliveTime,timeoutUnit,
			new LinkedBlockingQueue<Runnable>(maxQueue))
		
		/**
		 * Bypass Rejection Handler and use our custom method which
		 *  attempts to call alternateExecutor if configuration is enabled
		 */
		super.setRejectedExecutionHandler(new EnhancedRejectedExecutionHandler())
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

	/**
	 * Grails 3 being fussier about setting/overriding
	 * static variables
	 * will copy methods to grails 2 to keep things consistent
	 */
	void setMaximumPoolSize(int i) {
		this.maximumPoolSize=i
	}
	void setMaxQueue(int i) {
		this.maxQueue=i
	}
	
}