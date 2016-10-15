package org.grails.plugin.queuekit.priority

import grails.converters.JSON
import grails.util.Holders

import java.util.concurrent.ScheduledThreadPoolExecutor

import org.grails.plugin.queuekit.ComparableRunnable

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
		def currentService =  Holders.grailsApplication.mainContext.getBean(name)
		Priority lhs = currentService.getQueuePriority(this.queue,JSON.parse(this.queue.paramsMap))
		if (o instanceof ComparableRunnable) {
			Priority rhs = currentService.getQueuePriority(o.queue,JSON.parse(o.queue.paramsMap))
			i = lhs.getValue() <=> rhs.getValue()
		}
		return i
	}
}

