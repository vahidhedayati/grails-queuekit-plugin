package org.grails.plugin.queuekit


/**
 * 
 * This class has no additional declarations
 * when buildReport is executed, if report type is ArrayBlocking
 * it is saved in this extended class of ReportsQueue
 * Will then provide additional custom eventListener actions for 
 * ArrayBlocking
 * 
 * @author Vahid Hedayati
 *
 */
class ArrayBlockingReportsQueue extends ReportsQueue {

	String getQueueLabel() {
		return ARRAYBLOCKING
	}
}
