package org.grails.plugin.queuekit.priority

import grails.util.Holders

import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import org.grails.plugin.queuekit.QueuekitHelper

/**
 * Customised ThreadExecutor extended class
 * 
 * Manages ThreadExecutor PriorityBlockingQueue mechanism
 * And provides channel preservation based on priority limit
 * 
 * Remember to star project on github if this helps you !
 * 
 * @author Vahid Hedayati
 *
 */
class PriorityBlockingExecutor extends ThreadPoolExecutor {

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
	/*
	 * set this configuration value in order to preserve a given limit of threads
	 * for priority levels matching below given value
	 * so if 1 then 3 reportThreads means 2 can be hogged by   Priority.MEDIUM
	 * and remaining 1 will only accept below Priority.MEDIUM so HIGH HIGHEST can 
	 * go through that 1 channel
	 * 
	 * meaning if you correctly prioritise so small reports always have HIGH OR HIGHEST
	 * they will always go through without having to wait for 3 large reports to complete
	 */
	private static int minPreserve = Holders.grailsApplication.config.queuekit?.preserveThreads ?: 0

	/*
	 * Priority to use for threshold of minPreserve
	 * if minPreserve is above 0 then anything of definedPriority below matching preservation
	 * will not be bound by limitation of queue size
	 */
	private static Priority definedPriority = Holders.grailsApplication.config.queuekit?.preservePriority ?: Priority.MEDIUM

	/*
	 *Used by QueuekitHelper
	 *configured here to allow ease of update per executor
	 *
	 */
	private static int forceFloodControl = Holders.grailsApplication.config.queuekit.forceFloodControl ?: 0

	/*
	 *Used by QueuekitHelper
	 *configured here to allow ease of update per executor
	 *
	 */
	private static boolean defaultComparator = Holders.grailsApplication.config.queuekit.defaultComparator

	private static boolean standardRunnable = Holders.grailsApplication.config.queuekit.standardRunnable

	/*
	 *Used by QueuekitHelper
	 *configured here to allow ease of update per executor
	 *
	 */

	private static int limitUserAbovePriority = Holders.grailsApplication.config.queuekit.limitUserAbovePriority?:0
	private static int limitUserBelowPriority= Holders.grailsApplication.config.queuekit.limitUserBelowPriority?:0

	/**
	 * In order to capture ComparableFutureTask object
	 * runningTasks arrayList is used to capture beforeExecute
	 * and afterExecute to remove
	 *
	 * This will only capture current live running jobs
	 * which should not exceed over queuekit?.reportThreads limit
	 *
	 */
	static final Set<ArrayList> runningJobs = ([] as Set).asSynchronized()

	/**
	 * A test method created after having a play around with shutdown feature
	 *
	 * It appears when shutdown has been triggered since this Exectuor is injected and
	 * behaves like a grails service. The shutdown causes entire centralised executor
	 * process service to reject all new tasks.
	 *
	 */
	public static ThreadPoolExecutor alternateExecutor=(ThreadPoolExecutor) Executors.newFixedThreadPool(1)
	//public static ThreadPoolExecutor alternateExecutor=(ThreadPoolExecutor) Executors.newFixedThreadPool(maxPoolSize)

	public PriorityBlockingExecutor() {

		super(corePoolSize,maximumPoolSize,keepAliveTime,timeoutUnit,
		standardRunnable ? new PriorityBlockingQueue<Runnable>(maxQueue,new PriorityComparator()) :
		new PriorityBlockingQueue<Runnable>(maxQueue)
		)

		/**
		 * Bypass Rejection Handler and use our custom method which
		 *  attempts to call alternateExecutor if configuration is enabled
		 */
		super.setRejectedExecutionHandler(new EnhancedRejectedExecutionHandler())
	}

	/**
	 * override afterExecute actual method
	 * and add the Runnable from runningJobs
	 */
	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		runningJobs.add(r)
		super.beforeExecute(t, r)
	}

	/**
	 * override afterExecute actual method
	 * and remove the Runnable from runningJobs
	 */
	@Override
	protected void afterExecute(Runnable r, Throwable t) {
		runningJobs.remove(r)
		super.afterExecute(r, t)
	}

	/**
	 * new method of execute used by onApplicationEvent of classes that
	 * extend QueuekitExecutorBaseService and then call enhancedExecutor.execute
	 * 
	 * This execute method happens well before beforeExecute or afterExecute
	 * 
	 * Interesting fact: ---
	 * 
	 * This is the initiating point and after going around in circles a difference identified
	 * whilst Runnable command is at this stage it is still a CurrentTask class
	 * 
	 * So waiting.add(command) had a listing of all queued items but all in currentTask form
	 * 
	 * CurrentTask can not be cancelled. Instead to capture elements in current Queue of your 
	 * TaskExecutor simply fall back to getQueue() (as Found in QueueReportService):
	 * 
	 *  	priorityBlockingExecutor?.getQueue()?.find{it.queueId == queue.id}.cancel(true)
	 *  	priorityBlockingExecutor.purge()
	 * 
	 * @param command
	 * @param queueId
	 * @param priority
	 * @return
	 */
	public ComparableFutureTask execute(Runnable command, int priority) {

		boolean slotsFree
		if (!defaultComparator) {
			slotsFree=QueuekitHelper.changeMaxPoolSize(this,command.queue.userId,maximumPoolSize,minPreserve,priority,definedPriority.value,
					super.getActiveCount(),super.getCorePoolSize())
		}

		ComparableFutureTask task = new ComparableFutureTask(command,null,this,priority,definedPriority.value, maximumPoolSize, minPreserve,slotsFree)

		super.execute(task)
	}
}