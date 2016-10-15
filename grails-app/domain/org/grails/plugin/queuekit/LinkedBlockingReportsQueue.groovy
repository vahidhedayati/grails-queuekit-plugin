package org.grails.plugin.queuekit


/**
 * 
 * Extends Reports queue with no addition requirement
 * When buildReport typs is LinkedBlocking report
 * the queue is saved as this object which then has its own custom application Event 
 * and process of work
 * 
 * @author Vahid Hedayati
 *
 */
class LinkedBlockingReportsQueue extends ReportsQueue {
	
	
	String getQueueLabel() {
		return LINKEDBLOCKING
	}
	
}
