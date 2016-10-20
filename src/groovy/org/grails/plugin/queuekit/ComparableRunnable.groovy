package org.grails.plugin.queuekit

import grails.converters.JSON
import grails.util.Holders

import org.grails.plugin.queuekit.priority.Priority
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService

/**
 * 
 * One of those important classes I nearly forgot about
 * When a request comes in it is a ComparableRunnable unless it is a 
 * PriorityBlocking. It won't this this. 
 * EnhancedPriorityBlocking actually becomes 
 * CompareFutureTask and the comparison is done there
 * 
 * @author Vahid Hedayati
 *
 */
class ComparableRunnable implements Runnable, Comparable {
	

	private ReportsQueue queue
	
	public ComparableRunnable(ReportsQueue queue){
		this.queue=queue
	}
	
	/**
	 * Runnable finds queue.reportName
	 * calls .execute on relevant service bound to reportName
	 */
	@Override
	public void run() {
		/*
		 * This is where the task gets bound to end user generated service
		 * so if reportName was harryPotter
		 * it would generate
		 * HarryPotter${queue.serviceLabel} where
		 * 	-> queue.serviceLabel is bound to actual queue class
		 *
		 * in the case of all reports this will be
		 *  HarryPotterReportingService
		 * as outlined in notes
		 */
		String name = queue.reportName+queue.serviceLabel
		try {
			def currentService =  Holders.grailsApplication.mainContext.getBean(name)
			currentService.executeReport(queue)
		} catch (InterruptedException e) {
			e.printStackTrace()
		}
	}
	
	/**
	 * 
	 * This plugs into your specific report service that was made up from:
	 * reportNameReportingService
	 * 
	 * Then runs: getQueuePriority(queue,params) within the service
	 * 
	 * This should be updated by you to resemble what the form parameters Expected 
	 * in (params) should change current report priority to if:
	 * 
	 * date is between yesterday and today and report was actually set by config to be:
	 * LOWEST. Then maybe for this report it should now be HIGHEST 
	 * 
	 * If it is making sense - you need to apply the logic in each of those services
	 * 
	 */
	@Override
	public int compareTo(Object o) {
		int i = 0
		String name = this.queue.reportName+this.queue.serviceLabel
		def currentPriority = this.queue.priority
		def currentService =  Holders.grailsApplication.mainContext.getBean(name)
		Priority lhs = currentService.getQueuePriority(this.queue,JSON.parse(this.queue.paramsMap))
		if (o instanceof ComparableRunnable) {
			name = o.queue.reportName+o.queue.serviceLabel
			currentService =  Holders.grailsApplication.mainContext.getBean(name)
			Priority rhs = currentService.getQueuePriority(o.queue,JSON.parse(o.queue.paramsMap))
			//i = lhs.getValue() <=> rhs.getValue()			
			if (lhs.value < rhs.value) {
				i=-1
			} else if (lhs.value > rhs.value) {
				i=1
			}
			if (currentPriority!=lhs) {
				currentService =  Holders.grailsApplication.mainContext.getBean('enhancedPriorityBlockingExecutor')
				currentService.setLatestPriority(this.queue.id,lhs)
			}
		}
		return i
	}
}