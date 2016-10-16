package org.grails.plugin.queuekit

import org.grails.plugin.queuekit.priority.Priority


/**
 * This extends reportQueue and also has additional priority
 * if user runs buildReport and provides a priority
 * it is saved and overrides Config.groovy defined values
 * for a given report or reports that may not have configuration 
 * definition
 * 
 * @author Vahid Hedayati
 *
 */
class EnhancedPriorityBlockingReportsQueue extends ReportsQueue {
	
	Priority priority
	
	static constraints={
		priority(nullable:true)
	}
	
	String getQueueLabel() {
		return ENHANCEDPRIORITYBLOCKING
	}

	Boolean hasPriority() {
		return true
	}
	
	static mapping = {
		priority(enumType:'string',sqlType:'char(20)')
	}
	
	Boolean isEnhancedPriority() {
		return true
	}
}
