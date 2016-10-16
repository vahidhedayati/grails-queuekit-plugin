package org.grails.plugin.queuekit.priority

import grails.util.Holders

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.RunnableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

import org.grails.plugin.queuekit.QueuekitHelper
import org.grails.plugin.queuekit.ReportsQueue

/**
 * Customised ThreadExecutor extended class
 * and an expansion from PriorityBlockingExecutor
 * 
 * The limitations of PriorityBlockingExecutor is that you can not cancel a running task
 * I spent ages attempting to force thread cancellation to main process.
 * The issue is if you have a long running SQL query how would you notify the query to ask 
 * it to stop ? It is simply near enough impossible
 * 
 * Instead this class runs the actual running report within a newSingleThreadScheduledExecutor
 * When this master thread hits a cancellation the back-end thread is shutdown thus cancelling
 * what ever it was doing.
 * 
 * This has been tested and works, it just means a few additional concurrentHashMaps to capture
 * the existing / new threads launched and dealing with each in case of user cancellation
 * 
 * the delete button will only be made available to enhancedPriorityExecutor calls 
 * since this method is only one that is entangled with ThreadExecutors controlling other threads.
 * 
 * 
 * Remember to star project on github if this helps you !
 * 
 * @author Vahid Hedayati
 *
 */
class EnhancedPriorityBlockingExecutor extends ThreadPoolExecutor {


	private static long keepAliveTime=((Holders.grailsApplication.config.queuekit?.keepAliveTime ?: 300) as Long)

	private static TimeUnit timeoutUnit=TimeUnit.SECONDS

	/*
	 * Set the size of your corePoolSize this is your core/max size defined in one
	 */
	private static int corePoolSize = Holders.grailsApplication.config.queuekit?.corePoolSize ?: 3

	/*
	 * Set the size of your corePoolSize this is your core/max size defined in one
	 */
	private static final int actualPoolSize = Holders.grailsApplication.config.queuekit?.maximumPoolSize ?: 3
	private static int maximumPoolSize = actualPoolSize

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
	private  static int minPreserve = Holders.grailsApplication.config.queuekit?.preserveThreads ?: 0

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
	private static int forceFloodControl = Holders.grailsApplication.config.queuekit.forceFloodControl ?:0

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

	private static int limitUserBelowPriority = Holders.grailsApplication.config.queuekit.limitUserBelowPriority ?:0

	private static int limitUserAbovePriority = Holders.grailsApplication.config.queuekit.limitUserAbovePriority ?:0

	/**
	 * If killLongRunningTasks configuration value is provided in seconds
	 * before launching a new task, it will look up and kill any running
	 * beyond defined period.
	 * 
	 * This feature only works in enhancedPriorityExecutor since it runs the requirements
	 * in another self contained thread that it cancels along side this.
	 * 
	 * Refer to beforeExecute further down
	 */
	private static int killLongRunningTasks = Holders.grailsApplication.config.queuekit?.killLongRunningTasks ?: 0

	//private final ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor()

	/**
	 * runningTasks is a concurrentHashMap that holds 
	 *  -> Key: Current Runnable - again relates to inner launched task
	 *  -> Value: RunnableFuture FutureTask that was launched 
	 *  Controlled by 
	 */
	private static final ConcurrentMap<Runnable, RunnableFuture> runningTasks = new ConcurrentHashMap<Runnable, RunnableFuture>()

	/**
	 * waitingQueue is a concurrentHashMap that holds 
	 *  -> Key: queueId
	 *  -> Value: Runnable queue element for queueId
	 *  
	 *  This is for new threads launched to execute report
	 *   by ReportsService.execute call
	 *  
	 */

	private static final ConcurrentMap<Long, Runnable> waitingQueue = new ConcurrentHashMap<Long, Runnable>()


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
	 * It appears when shutdown has been triggered since this Executor is injected and 
	 * behaves like a grails service. The shutdown causes entire centralised executor
	 * process service to reject all new tasks.
	 * 
	 * Spent ages trying to work a way around re-starting schedule refer to startup() 
	 * further down in this class.
	 * 
	 * For this to work queuekit.useEmergencyExecutor = true must be set in configuration
	 * 
	 * When this is triggered all the rules set up for priority etc goes out of the window
	 * this is really an emergency solution to keep business flowing
	 * 
	 */
	public static ScheduledExecutorService alternateExecutor= Executors.newSingleThreadScheduledExecutor()
	//public static ThreadPoolExecutor alternateExecutor=(ThreadPoolExecutor) Executors.newFixedThreadPool(maxPoolSize)


	public EnhancedPriorityBlockingExecutor() {

		super(corePoolSize,maximumPoolSize,keepAliveTime,timeoutUnit,
		standardRunnable ? new PriorityBlockingQueue<Runnable>(maxQueue,new EnhancedPriorityComparator()) :
		new PriorityBlockingQueue<Runnable>(maxQueue)
		)


		/**
		 * Bypass Rejection Handler and use our custom method which
		 *  attempts to call alternateExecutor if configuration is enabled
		 */
		super.setRejectedExecutionHandler(new EnhancedRejectedExecutionHandler())

	}


	/**
	 * This ends the current and attempts to end the attached thread as well 
	 * @param id
	 * @param timeoutExecutor
	 */
	static void endRunningTask(Long id, ExecutorService timeoutExecutor) {
		def  r = waitingQueue.get(id)
		if (r) {
			removeRunningTask(r)
		}
		removeWaitingQueue(id)
		//timeoutExecutor.shutdown()
		timeoutExecutor.shutdownNow()
	}


	/**
	 * used by ReportsService to trigger a background
	 * thread that is then bound to this thread
	 * @param id
	 * @param r
	 * @param scheduled
	 */
	static void addScheduledTask(Long id,Runnable r,RunnableFuture scheduled) {
		addRunningTask(r,scheduled)
		addWaitingQueue(id,r)
	}


	/**
	 * remove the runningTask map for runnable and FutureTask
	 * @param r
	 * @return
	 */
	static boolean removeRunningTask(AttachedRunnable r) {
		RunnableFuture timeoutTask = runningTasks.get(r)
		if(timeoutTask) {
			timeoutTask.cancel(true)
			runningJobs.remove(r)
			r.shutdown()
			return true
		}
		runningTasks.remove(r)
		return false
	}

	/**
	 * add the actual attached thread queues runnable along with queueId
	 * @param id
	 * @param r
	 * @return
	 */
	static boolean addWaitingQueue(Long id, Runnable r) {
		waitingQueue.put(id, r)
		return true
	}

	/**
	 * when job completes attempt to close
	 * attached thread
	 * @param id
	 * @return
	 */
	static boolean removeWaitingQueue(Long id) {
		Runnable timeoutTask = waitingQueue.remove(id)
		if(timeoutTask) {
			//timeoutTask.cancel(false)
			return true
		}
		return false
	}

	/**
	 * This is used for when binding additional thread
	 * when job starts
	 * @param r
	 * @param scheduled
	 * @return
	 */
	static boolean addRunningTask(Runnable r,RunnableFuture scheduled) {
		runningTasks.put(r, scheduled)
		return true
	}


	/**
	 * override afterExecute actual method
	 * and add the Runnable from runningJobs
	 */

	@Override
	protected void beforeExecute(Thread t, Runnable r) {
		/**
		 * If you have configured in seconds a value for 
		 * killLongRunningTasks
		 * Then it will attempt to kill of any active running tasks 
		 * that have been running beyond given value
		 * 
		 */
		if (killLongRunningTasks > 0) {
			Long now = (new Date()).time
			boolean found=false
			runningJobs.findAll{k->((now - k.startTime) / 1000.0) >  killLongRunningTasks}?.each {ComparableFutureTask k->
				endRunningTask(k.queueId,k.timeoutExecutor)
				k.cancel(true)
				runningJobs.remove(k)
				sleep(600)
				found=true
				ReportsQueue.withTransaction {
					ReportsQueue c = ReportsQueue.get(k.queueId)
					c.status=ReportsQueue.CANCELLED
					c.save(flush:true)
				}
			}
			if (found) {
				super.purge()
			}
		}
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
	 * If you set standardRunnable = false or did not declare it then execute runnable is called by 
	 * EnhancedPriorityBlockingReportsQueueService
	 * 
	 */
	public void  execute(Runnable command) {
		boolean slotsFree
		if (!defaultComparator) {
			slotsFree=QueuekitHelper.changeMaxPoolSize(this,command.queue.userId,actualPoolSize,minPreserve,command.queue?.priority?.value ?: 0,definedPriority.value,
					super.getActiveCount(),super.getCorePoolSize())
		}
		ScheduledThreadPoolExecutor timeoutExecutor= new ScheduledThreadPoolExecutor(1)
		timeoutExecutor.setRemoveOnCancelPolicy(true)

		CompareFutureTask task = new CompareFutureTask(command,null,this,timeoutExecutor,definedPriority.value,
				actualPoolSize, minPreserve,slotsFree)

		super.execute(task)
	}

	/**
	 * If you have set standardRunnable = true then execute runnable is called by 
	 * EnhancedPriorityBlockingReportsQueueService
	 * 
	 * new method of execute used by onApplicationEvent of classes that
	 * extend QueuekitExecutorBaseService and then call enhancedExecutor.execute
	 * 
	 * This execute method happens well before beforeExecute or afterExecute
	 * 
	 * Interesting findings: ---
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
		/*
		 * if slots have been preserved.
		 * Then check current incoming task
		 * if it is above definedPriority to reduce the poolQueue
		 * so that it goes into queued mode
		 * When the new task is less than definedPriority 
		 * queue Size is increased to allow it to be processed
		 * 
		 * If you have reportThreads=3 & preserveThreads = 2 
		 *  -> with preservePriority = Priority.MEDIUM
		 * configured only 1 job will be used for all those above = MEDIUM Priority
		 * The other 2 remaining jobs will only accept HIGH AND HIGHEST 
		 * Priorities - so 2 for fast quick jobs 1 for slow jobs
		 */
		boolean slotsFree
		if (!defaultComparator) {
			slotsFree=QueuekitHelper.changeMaxPoolSize(this,command.queue.userId,maximumPoolSize,minPreserve,priority,definedPriority.value,
					super.getActiveCount(),super.getCorePoolSize())
		}

		/*
		 * Generate a singleThread to be bound to actual task
		 * only when it starts to execute i.e. beforeExecute
		 * so whilst in queue bind timeExecutor object to it
		 */
		//ExecutorService timeoutExecutor = Executors.newSingleThreadExecutor()
		ScheduledThreadPoolExecutor timeoutExecutor= new ScheduledThreadPoolExecutor(1)
		timeoutExecutor.setRemoveOnCancelPolicy(true)
		/*
		 * Use the override method to load in timeoutExecutor as well
		 */
		ComparableFutureTask task = new ComparableFutureTask(command,null,this,timeoutExecutor,priority,definedPriority.value,
				maximumPoolSize, minPreserve,slotsFree)

		super.execute(task)
	}
	@Override
	public void shutdown() {
		super.shutdown()
	}

	/**
	 * Attempt to startup process after shutdown
	 * This felt like a logical solution. Unfortunately does not 
	 * work as expected. Refer to alternateExecuor and EnhancedRejectedExecutionHander
	 * as a work around for when user triggers a shutdown or something goes wrong 
	 * and thread executor is shutdown. 
	 * 
	 * Will comment out below but leave as reference. Maybe someone will find a better way
	 * 
	 * 
	 * @return
	 */
	//public EnhancedPriorityBlockingExecutor startup() {
	//	return new EnhancedPriorityBlockingExecutor(this)
	//}
	
	
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
	void setMinPreserve(int i) {
		this.minPreserve=i
	}
	void setDefinedPriority(Priority p) {
		this.definedPriority=p
	}
	void setForceFloodControl(int i) {
		this.forceFloodControl=i
	}
	void setLimitUserAbovePriority(int i) {
		this.limitUserAbovePriority=i
	}
	void setLimitUserBelowPriority(int i) {
		this.limitUserBelowPriority=i
	}
	void setDefaultComparator(boolean b) {
		this.defaultComparator=b
	}
}
