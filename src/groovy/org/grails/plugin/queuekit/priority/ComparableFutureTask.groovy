package org.grails.plugin.queuekit.priority

import grails.converters.JSON

import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledThreadPoolExecutor

import org.grails.plugin.queuekit.ReportsQueue
/**
 *
 * ComparableFutureTask is just enhancements of FutureTask.
 * Capturing additional information required 
 * 
 * It is not a comparator. This class kicks in when config value:
 * 	standardRunnable = true
 * 
 * The actual comparison is carried out by either:
 * PriorityComparator class or
 * EnhancedPriorityComparator class depending on call 
 *
 * @author Vahid Hedayati
 *
 * @param <T>
 */
class ComparableFutureTask<T> extends FutureTask<T>  {


	volatile int priority = 0
	volatile int definedPriority = 0
	volatile int maxPoolSize = 0
	volatile int minPreserve = 0
	volatile boolean slotsFree = true

	/**
	 * A later addition
	 * captures new taks's params to be executed 
	 */
	volatile Map paramsMap=[:]
	volatile Long startTime = 0
	volatile Long userId = 0
	volatile Long queueId = 0
	ReportsQueue queue
	PriorityBlockingExecutor priorityBlockingExecutor
	EnhancedPriorityBlockingExecutor enhancedPriorityBlockingExecutor
	//volatile ExecutorService timeoutExecutor
	volatile ScheduledThreadPoolExecutor timeoutExecutor


	/**
	 * LinkedPriority/ArrayBlocking use this method
	 * 
	 * @param runnable
	 * @param result
	 */
	public ComparableFutureTask(Runnable runnable, T result) {
		super(runnable, result)
		updateDefaults(runnable)
	}



	/**
	 * 
	 * EnhancedPriorityExecutor uses this
	 * @param runnable
	 * @param result
	 * @param enhancedPriorityBlockingExecutor
	 * @param timeoutExecutor
	 * @param priority
	 * @param definedPriority
	 * @param maxPoolSize
	 * @param minPreserve
	 * @param slotsFree
	 */
	public ComparableFutureTask(Runnable runnable, T result,
	EnhancedPriorityBlockingExecutor enhancedPriorityBlockingExecutor,
	ScheduledThreadPoolExecutor timeoutExecutor,
	int priority, int definedPriority,
	int maxPoolSize,int minPreserve,boolean slotsFree) {

		super(runnable, result)
		this.enhancedPriorityBlockingExecutor=enhancedPriorityBlockingExecutor
		this.timeoutExecutor=timeoutExecutor
		updatePriority(priority,definedPriority,maxPoolSize,minPreserve,slotsFree)
		updateDefaults(runnable)
	}



	/**
	 * PriorityExecutor uses this
	 *
	 * @param runnable
	 * @param result
	 * @param priorityBlockingExecutor
	 * @param priority
	 * @param definedPriority
	 * @param maxPoolSize
	 * @param minPreserve
	 * @param slotsFree
	 */
	public ComparableFutureTask(Runnable runnable, T result,PriorityBlockingExecutor priorityBlockingExecutor,
	int priority, int definedPriority, int maxPoolSize,int minPreserve,boolean slotsFree) {

		super(runnable, result)
		this.priorityBlockingExecutor=priorityBlockingExecutor
		updatePriority(priority,definedPriority,maxPoolSize,minPreserve,slotsFree)
		updateDefaults(runnable)
	}

	private void updatePriority(int priority, int definedPriority, int maxPoolSize,int minPreserve,boolean slotsFree) {
		this.priority = priority
		this.definedPriority = definedPriority
		this.maxPoolSize = maxPoolSize
		this.minPreserve = minPreserve
		this.slotsFree = slotsFree
	}

	private void updateDefaults(Runnable runnable) {
		this.paramsMap =JSON.parse(runnable.queue.paramsMap)
		this.queueId =  runnable.queue.id
		this.queue = runnable.queue
		this.startTime=(new Date()).time
		this.userId = runnable.queue.userId
	}

	/*
	 public ComparableFutureTask(Callable<T> callable,EnhancedPriorityBlockingExecutor enhancedPriorityBlockingExecutor,
	 ExecutorService timeoutExecutor,
	 int priority, int definedPriority, int maxPoolSize,int minPreserve,boolean slotsFree) {
	 super(callable)
	 this.enhancedPriorityBlockingExecutor=enhancedPriorityBlockingExecutor
	 this.timeoutExecutor=timeoutExecutor
	 this.priority = priority
	 this.definedPriority = definedPriority
	 this.maxPoolSize = maxPoolSize
	 this.minPreserve = minPreserve
	 this.slotsFree = slotsFree
	 this.paramsMap =JSON.parse(callable.queue.paramsMap)
	 this.queueId =  callable.queue.id
	 this.startTime=(new Date()).time
	 this.userId = callable.queue.userId
	 }
	 */

	/*
	 public ComparableFutureTask(Callable<T> callable,PriorityBlockingExecutor priorityBlockingExecutor,
	 int priority, int definedPriority, int maxPoolSize,int minPreserve,boolean slotsFree) {
	 super(callable)
	 this.priorityBlockingExecutor=priorityBlockingExecutor
	 this.timeoutExecutor=timeoutExecutor
	 this.priority = priority
	 this.definedPriority = definedPriority
	 this.maxPoolSize = maxPoolSize
	 this.minPreserve = minPreserve
	 this.slotsFree = slotsFree
	 this.paramsMap =JSON.parse(callable.queue.paramsMap)
	 this.queueId =  callable.queue.id
	 this.startTime=(new Date()).time
	 this.userId = callable.queue.userId
	 }
	 */	
}