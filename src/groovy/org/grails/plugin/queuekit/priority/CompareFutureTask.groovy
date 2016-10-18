package org.grails.plugin.queuekit.priority

import grails.converters.JSON
import grails.util.Holders

import java.util.concurrent.ScheduledThreadPoolExecutor

import org.grails.plugin.queuekit.QueuekitHelper
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService

/**
 * 
 * In order to make the ComparableFutureTask actually  a 
 * comparable class this extended method introduced.
 * 
 * This is used when config value:
 * 	standardRunnable = false (or not declared)
 * 
 * The PriorityBlockingExecutor does not rely on this 
 * and uses default execute method of PriorityBlockingExecutor
 * (Or its super class) and relies on the default :
 * ComparableRunnable class that object was created as. 
 * 
 * @author Vahid Hedayati
 *
 * @param <T>
 */
class CompareFutureTask<T> extends ComparableFutureTask<T> implements Comparable {

	public CompareFutureTask(Runnable runnable, T result,
	EnhancedPriorityBlockingExecutor enhancedPriorityBlockingExecutor,
	ScheduledThreadPoolExecutor timeoutExecutor,
	int definedPriority,
	int maxPoolSize,int minPreserve,boolean slotsFree) {

		super(runnable, result)
		this.enhancedPriorityBlockingExecutor=enhancedPriorityBlockingExecutor
		this.timeoutExecutor=timeoutExecutor
		updatePriority(0,definedPriority,maxPoolSize,minPreserve,slotsFree)
		updateDefaults(runnable)
	}

	@Override
	public int compareTo(Object o) {
		int i = 0
		String name = this.queue.reportName+this.queue.serviceLabel
		String currentPriority = this.queue.priority
		def currentService =  Holders.grailsApplication.mainContext.getBean(name)
		Priority lhs = currentService.getQueuePriority(this.queue,JSON.parse(this.queue.paramsMap))
		if (o instanceof CompareFutureTask) {
			name = o.queue.reportName+o.queue.serviceLabel
			currentService =  Holders.grailsApplication.mainContext.getBean(name)
			Priority rhs = currentService.getQueuePriority(o.queue,JSON.parse(o.queue.paramsMap))
			//i = lhs.getValue() <=> rhs.getValue()
			if (lhs.value < rhs.value) {
				i=-1
			} else if (lhs.value > rhs.value) {
				i=1
			}
		}
		
		
		if (!enhancedPriorityBlockingExecutor.defaultComparator) {
			/**
			 * This is some complex stuff going on to figure out if the current item should have a slot free
			 * and given a slot if it should
			 *
			 * It has taken me a few days of going round to come up with this logic
			 *
			 * quite likely to have things changed within changeMaxPoolSize in future
			 *
			 * is also used by Actual PriorityBlockingExecutor as request comes in
			 */
			boolean slotsFree = QueuekitHelper.changeMaxPoolSize(enhancedPriorityBlockingExecutor,queue.userId,enhancedPriorityBlockingExecutor?.actualPoolSize ?: enhancedPriorityBlockingExecutor.maximumPoolSize,
					minPreserve, lhs.value,definedPriority, enhancedPriorityBlockingExecutor.getActiveCount(),enhancedPriorityBlockingExecutor.getCorePoolSize())

			/**
			 * Unsure about this feel free to give input
			 * as to if this should be actual return code or
			 * maybe this hack is working as expected.
			 *
			 */
			if (i>0 && slotsFree) {
				i=-1
			}
		}
		if (currentPriority!=lhs) {
			QueuekitBaseReportsService.setLatestPriority(this.queue.id,lhs)
		}
		return i
	}
}

