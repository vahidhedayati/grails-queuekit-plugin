package org.grails.plugin.queuekit.executors

import groovy.time.TimeCategory
import org.grails.plugin.queuekit.ArrayBlockingReportsQueue
import org.grails.plugin.queuekit.ReportRunnable
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.validation.QueueKitBean
import reactor.spring.context.annotation.Consumer
import reactor.spring.context.annotation.Selector

import java.util.concurrent.RunnableFuture
import java.util.concurrent.TimeUnit

/**
 * ArrayBlockingReportsQueueService extends the default functionality 
 * of queuekitExecutorBaseService but also rewrites checkQueue and adds
 * customised application Even and requeue mechanisms.  
 * 
 * ArrayBlocking will not manage your queue instead we are using the DB 
 * to manage and poll new tasks to the scheduler
 * 
 * There are additional segments of code in this Listener event to ensure 
 * queue for ArrayBlocking works more smoothly
 * 
 * @author Vahid Hedayati
 *
 */
@Consumer
class ArrayBlockingReportsQueueService extends QueuekitExecutorBaseService  {

	def arrayBlockingExecutor

	@Selector('method.arrayBlocking')
	void arrayBlocking(Long eventId) {
		log.debug "Received ${eventId}"

		/*
		 * We are working with ArrayBlockingQueuedEvent which is a direct relation to
		 *  ArrayBlockingReportsQueue domainClass
		 *
		 *  lets load up correct queue domainClass
		 */
		ArrayBlockingReportsQueue.withTransaction {
			ArrayBlockingReportsQueue queue = ArrayBlockingReportsQueue.read(eventId)
			if (queue && (queue.status == ReportsQueue.QUEUED || queue.status == ReportsQueue.ERROR)) {
				def result
				/*
			 * ArrayBlockingQueue does not automatically process queue. 
			 * When more requests than available slots are sent it will hit this try - 
			 * Catch exception when thread pool is exhausted then reset Queue Date 
			 * so when next task completes it calls checkQueue which then attempts to schedule 
			 */
				try {
					ReportRunnable currentTask = new ReportRunnable(queue)
					/*
				 * This now calls the overridden execute method in ArrayBlockingExecutor
				 * which converts RunnableFuture (FutureTask) to ComparableFutureTask
				 * This then captures queueId for usage in cancellation
				 */
					RunnableFuture task = arrayBlockingExecutor.execute(currentTask, queue.id)
					try {
						result = task?.get(reportTimeout, TimeUnit.SECONDS)
					} catch (e) {
						task.cancel(true)
						log.error "Report ${queue.reportName} ${queue.id} has timed out. Report being set to error status"
					}
				} catch (e) {
					/*
				 * Task java.util.concurrent exception is thrown when all threads are used up and a report is forced to queue
				 * ignore the exception and add id back in pool. checkQueue should pick up all queued reports
				 */
					resetRequeueDate(queue.id)
				}
			}
		}
	}

	/**
	 * Works out and returns a map of total jobs queued / running and available limit
	 */
	Map getJobsAvailable() {
		int limit = 0
		int queued = arrayBlockingExecutor.getQueue().size()
		int running = arrayBlockingExecutor.getActiveCount()
		int threadLimit = arrayBlockingExecutor.getCorePoolSize()
		if (!running||running==0) {
			limit = threadLimit
		} else {
			if (running < threadLimit) {
				limit =  threadLimit - running
			}
		}
		int incomplete = queued + running
		return [threadLimit:threadLimit,limit:limit,running:running,queued:queued,incomplete:incomplete]
	}

	/**
	 * This is an override method of checkQueue in queuekitExecutorBaseService
	 * It does a lot more since this is queues for ArrayBlocking
	 * 1. attempts to only look up ArrayBlockingQueue DB table
	 * 2. Looks up records with queuePeriod
	 */
	void checkQueue(Long id=null) {
		def whereParams=[:]
		String addon=''
		if (id) {
			addon='and rq.id!=:id'
			whereParams.id=id
		}
		whereParams.status=ReportsQueue.QUEUED
		whereParams.queuePeriod = queuePeriod

		def query="""select new map(rq.id as id, rq.class as className) from ArrayBlockingReportsQueue rq where rq.status=:status
							and (rq.requeued is null or rq.requeued < :queuePeriod)
							and rq.start is null $addon group by rq.id order by rq.queuePosition asc, id asc
					"""
		def metaParams=[readOnly:true,timeout:15,max:-1,cache: false]
		def waiting=ArrayBlockingReportsQueue.executeQuery(query,whereParams,metaParams)
		def jobs = jobsAvailable
		def running = jobs.running
		def threadLimit = jobs.threadLimit
		log.debug "waiting reports ${waiting.size()} Jobs available: ${jobs}"
		waiting?.each{queue ->
			if (running < threadLimit) {
				setRequeueDate(queue.id)
				new Thread({
					sleep(500)
					notify( "method.arrayBlocking",queue.id)
				} as Runnable ).start()
				running++
			}

		}

	}

	/**
	 * Gets executed by application event at the very top, when it has hit a situation that
	 * it had been queued but it didn't really manage to get on to the queue
	 * ensure it gets picked up next without causing issues
	 * @param queueId
	 */
	private void resetRequeueDate(Long queueId) {
		ReportsQueue.withNewTransaction {
			ReportsQueue queue=ReportsQueue.get(queueId)
			if (queue) {
				queue.requeued=null
				queue.save()
			}
		}

	}

	/**
	 * Sets the queue requeued  Date to be now
	 * if in error adds retries count
	 * simply ensures this item will not be picked up on next checkQueue 
	 * if it was last checked within the period of report time out.
	 * Gives some time to not keep locking same records.
	 * @param queueId
	 */
	private void setRequeueDate(Long queueId) {
		ReportsQueue.withNewTransaction {
			ReportsQueue queue=ReportsQueue.get(queueId)
			if (queue) {
				queue.requeued=new Date()
				if (queue.status==ReportsQueue.ERROR) {
					queue.retries=queue.retries ? queue.retries+1 : 1
				}
				queue.save(flush:true)
			}
		}

	}

	/**
	 * Requeue task
	 * must save before submitting Event otherwise Row was updated or deleted by another transaction
	 */
	public boolean requeue(Long queueId) {
		boolean result=false
		def whereParams=[:]
		def query="""select rq.id as id from ArrayBlockingReportsQueue rq where 
			(rq.status=:queued or rq.status=:error) 
			and (rq.requeued is null or rq.requeued < :queuePeriod) and rq.id=:queueId
		"""
		whereParams.queued=ReportsQueue.QUEUED
		whereParams.error=ReportsQueue.ERROR
		whereParams.queuePeriod = queuePeriod
		whereParams.queueId = queueId
		def metaParams=[readOnly:true,timeout:15,max:1,cache: false]
		def queue=ArrayBlockingReportsQueue.executeQuery(query,whereParams,metaParams)[0]
		if (queue) {
			def jobs = jobsAvailable
			def running = jobs.running
			def threadLimit = jobs.threadLimit
			// To enable queueing to be handled by Threading remove if statement below
			if (running < threadLimit ) {
				setRequeueDate(queueId)
				notify( "method.arrayBlocking",queue.id)
				result=true
			}
		}
		return result
	}

	/**
	 * RescheduleAll is a front end button feature that allows the end user to attempt to 
	 * requeue all items belonging to them
	 * @param bean
	 * @return
	 */
	boolean rescheduleAll(QueueKitBean bean) {
		def jobs = jobsAvailable
		def running = jobs.running
		def threadLimit = jobs.threadLimit
		def query=""" from ArrayBlockingReportsQueue rq where rq.userId =:currentUserId 
			and rq.status =:queued and
				(rq.requeued is null or rq.requeued < :queuePeriod) order by rq.queuePosition asc, id asc"""
		def whereParams=[:]
		boolean startedJob=false
		def metaParams=[readOnly:true,timeout:15,max:-1]
		whereParams.currentUserId=bean.userId
		whereParams.queued=ReportsQueue.QUEUED
		whereParams.queuePeriod=queuePeriod
		def results=ArrayBlockingReportsQueue.executeQuery(query,whereParams,metaParams)
		if (running < threadLimit ) {
			results?.each {ReportsQueue queue ->
				// To enable queueing to be handled by Threading remove if statement below
				if (running < threadLimit ) {
					startedJob=true
					setRequeueDate(queue.id)
					new Thread({
						sleep(400)
						notify("method.arrayBlocking", queue.id)
					})
					running++
				}
			}
		}
		return startedJob
	}

	/*
	 *  reportCheckRunning works whilst checkQueue works in conjunction with: executeReport:
	 *  	timeouts or completions
	 *
	 *  When user clicks cancel on web interface reportCheckRunning is always false:
	 *  this leads to same record being requeued
	 *  To stop same record being requeued additional check added to check and include ID's that :
	 *  Either have no requeued date or have been requeued date matching timeout period
	 */
	Date getQueuePeriod() {
		def queuePeriod = new Date()
		use (TimeCategory) {
			queuePeriod=queuePeriod-reportTimeout.seconds
		}
		return queuePeriod
	}

	/**
	 * 
	 * @return report timeout according to config
	 */
	Integer getReportTimeout() {
		return config.reports.timeout ?: 300
	}
}
