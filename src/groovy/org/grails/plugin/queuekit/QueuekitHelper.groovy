package org.grails.plugin.queuekit

import grails.util.Holders

import java.util.concurrent.PriorityBlockingQueue

import org.grails.plugin.queuekit.priority.ComparableFutureTask
import org.grails.plugin.queuekit.priority.EnhancedPriorityBlockingExecutor
import org.grails.plugin.queuekit.priority.Priority

/**
 * 
 * Helper class that retrieves useful information for QueueKit plugin
 * @author Vahid Hedayati
 *
 */
class QueuekitHelper {
	/**
	 * Retrieve configuration value of reportPriorties 
	 * this should be a list containing key value e.g: ReportName:Priority.HIGH
	 * 
	 * Assign report priority back if not set to DEFAULT LOW 
	 */
	static Priority sortPriority(String reportName) {
		Priority priority = Priority.LOW
		def priorities = Holders.grailsApplication.config.queuekit.reportPriorities
		if (priorities) {
			priority = priorities.get(reportName) ?: Priority.LOW
		}
		return priority
	}


	/**
	 * changeMaxPoolSize is another deeply complex bit of code that I have spent ages on 
	 * still not perfected it.
	 * 
	 * It controls whether a 
	 * 			Comparator: PriorityComparator or EnhancedPriorityComparator
	 * 	 	or    Executor: EnhancedPriorityExecutor or PriorityExecutor
	 * 
	 * Should place item with the given priority in the queue or not.
	 * It resizes actual given Executor's running size to disable the task from being able to execute
	 * 
	 * @param executor
	 * @param userId
	 * @param maxPoolSize
	 * @param minPreserve
	 * @param priority
	 * @param definedPriority
	 * @param running
	 * @param coreSize
	 * @return
	 */
	static boolean changeMaxPoolSize(executor,Long userId,int maxPoolSize,int minPreserve,int priority,int definedPriority,int running,int coreSize) {
		boolean slotsFree=true
		int available = maxPoolSize - running
		int poolSize = (coreSize+1 <= maxPoolSize) ? (coreSize+1) : maxPoolSize
		int minSize = maxPoolSize - minPreserve

		if (minPreserve > 0) {
			def executorCount = executorCount(executor.runningJobs,executor.getQueue(), definedPriority,userId)

			if (priority >= definedPriority) {
				available = coreSize - running
				slotsFree=available > minPreserve ? true : false
				if (!slotsFree) {

					/**
					 *  This are no free slots in situation where priority of task is higher
					 *  than default group Priority
					 *  
					 * If floodControl is enabled and set to 2
					 * and all running tasks are below than defaultGroup 
					 * so MEDIUM LOW + LOWER are all the running tasks
					 * + no one else is waiting in queue 
					 * + no jobs waiting for LESS than MEDIUM so nothing queued
					 * for HIGH / HIGHEST 
					 * 
					 * If all of above has been met give this LOW priority task  
					 * the reserved channel
					 */
					if (executor.forceFloodControl==2 && executorCount.runningAbove == coreSize &&
						 !executorCount.othersWaiting  && executorCount.queuedBelow == 0 ) {
						slotsFree=true
					}
				} else {					
					/**
					 *  There is a free slot where priority of task is higher than default group Priority
					 *  
					 *  Let's confirm if floodControl is enabled
					 *  and either others are waiting and then user also has a limitUserAbovePriority set or if not coreSize
					 *  if that value equals current users running tasks above default group plus this one (+1)
					 *  
					 *  or no other's using and user has used up all slots matching coreSize 
					 *  
					 *  then set slotsFree to false
					 *  
					 * 
					 */
					int counter = executorCount.runningAbove + 1
					def group = (executor.limitUserAbovePriority ? executor.limitUserAbovePriority : coreSize)
					if ((executor.forceFloodControl > 0 && executorCount.othersWaiting && (counter >= group ||
					executorCount.userRunningBelow + executorCount.userRunningAbove >= coreSize)
					) || executorCount.othersWaitingAbove > 0 &&  executorCount.userRunningBelow + executorCount.userRunningAbove >= coreSize){
						slotsFree=false
					}
				}
			} else {
				slotsFree=maxPoolSize > coreSize ? true : false
				if (!slotsFree) {
					/**
					 * This are no free slots and priority is less than defined group so
					 * HIGH OR HIGHEST tasks if default is MEDIUM
					 * 
					 * If forceFloodControl is enabled no one is waiting and there is that reserve slot
					 * then use it for higher priority task
					 * 
					 *At this point this block below should fight with block above no slotsFree
					 *both looking at opposite if you have set forceFloodControl to be 2
					 */
					if (executor.forceFloodControl > 0 && executorCount.runningBelow == coreSize && !executorCount.othersWaiting){
						slotsFree=true
					}
				} else {
					int counter = (executorCount.runningBelow + 1)
					def group = (executor.limitUserBelowPriority? executor.limitUserBelowPriority : coreSize)
					if (executor.forceFloodControl > 0 && (executorCount.othersWaiting||executorCount.queuedAbove > 0) && counter >= group ||
					(executorCount.queuedAbove > 0  || executorCount.othersWaiting) &&
					executorCount.userRunningBelow + executorCount.userRunningAbove >= coreSize)   {
						slotsFree=false
					}
				}
			}
		}

		int runSize = slotsFree ? poolSize : minSize
		if (runSize && executor) {
			executor.setCorePoolSize(runSize)
			executor.setMaximumPoolSize(runSize)
		}
		return slotsFree
	}

	/**
	 * Works out and returns size of running + queue elements in current Executor
	 * Used by above as well as queueReportService listing action
	 * 
	 * Fairly complex bit of code that looks within the runningJobs collection and 
	 * also within the queued elements.
	 * 
	 *
	 * @param runningJobs  (Pass in runningJobs of given Executor Choice of PriorityExecutor or EhancedPriorityExecutors)
	 * @param waitingQueue (The getQueue() element of any of the above executors)
	 * @param optional definedPriority  (provide configuration definedPriority or not)
	 * @param optional userId (provide a userId or not)
	 * @return
	 */
	public static Map executorCount(Collections.SynchronizedSet runningJobs,PriorityBlockingQueue waitingQueue,
			int definedPriority=EnhancedPriorityBlockingExecutor.definedPriority.value, Long userId=0) {

		def runBelowPriority = runningJobs?.findAll{it.priority < definedPriority}
		int runningBelowPriority =runBelowPriority?.size() ?: 0

		def runAbovePriority =runningJobs?.findAll{it.priority >= definedPriority}
		int runningAbovePriority=runAbovePriority?.size() ?:  0

		def queueAbovePriority=waitingQueue?.findAll{k-> if (k?.priority) {k.priority?.value >= definedPriority}}
		int queuedAbovePriority = queueAbovePriority?.size() ?: 0

		def queueBelowPriority=waitingQueue?.findAll{k-> if (k?.priority) {k.priority?.value < definedPriority}}
		int queuedBelowPriority = queueBelowPriority?.size() ?: 0

		int userQueuedAbove,userQueuedBelow,userRunningAbove,userRunningBelow,othersWaitingAbove,othersWaitingBelow
		boolean othersWaiting
		if (userId) {
			userQueuedAbove = queueAbovePriority?.findAll{ComparableFutureTask t ->  t.userId == userId}?.size() ?: 0
			userQueuedBelow = queueBelowPriority?.findAll{ComparableFutureTask t ->  t.userId == userId}?.size() ?: 0
			userRunningBelow = runBelowPriority?.findAll{ComparableFutureTask t ->  t.userId == userId}?.size() ?: 0
			userRunningAbove = runAbovePriority.findAll{ComparableFutureTask t ->  t.userId == userId}?.size() ?: 0
			othersWaitingBelow = queueAbovePriority?.findAll{ComparableFutureTask t ->  t.userId != userId}?.size() ?: 0
			othersWaitingAbove = queueBelowPriority?.findAll{ComparableFutureTask t ->  t.userId != userId}?.size() ?: 0
			othersWaiting = othersWaitingBelow+othersWaitingAbove>0
		}
		return [runningBelow: runningBelowPriority, runningAbove: runningAbovePriority,
			queuedBelow: queuedBelowPriority, queuedAbove: queuedAbovePriority,
			userBelow: userQueuedBelow, userAbove: userQueuedAbove,
			userRunningAbove:userRunningAbove,userRunningBelow:userRunningBelow,
			othersWaiting:othersWaiting,othersWaitingBelow:othersWaitingBelow,othersWaitingBelow:othersWaitingBelow
		]

	}

}
