package org.grails.plugin.queuekit.validation

import grails.converters.JSON
import grails.validation.Validateable

import org.grails.plugin.queuekit.ArrayBlockingExecutor
import org.grails.plugin.queuekit.LinkedBlockingExecutor
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.EnhancedPriorityBlockingExecutor
import org.grails.plugin.queuekit.priority.Priority
import org.grails.plugin.queuekit.priority.PriorityBlockingExecutor

/**
 * ChangeConfigBean used for front end user interaction
 * to accept input from the change configuration 
 * 
 * @author Vahid Hedayati
 *
 */
@Validateable
class ChangeConfigBean extends ChangePriorityBean {

	static final String POOL='PO'
	static final String PRESERVE='PR'
	static final String CHECKQUEUE='CQ'
	static final String STOPEXECUTOR='ST'
	static final String FLOODCONTROL='FC'
	static final String LIMITUSERABOVE='LA'
	static final String LIMITUSERBELOW='LB'
	static final String DEFAULTCOMPARATOR='DC'
	static final String MAXQUEUE='MQ'
	static final List CHANGE_TYPES=[POOL,MAXQUEUE,PRESERVE,DEFAULTCOMPARATOR,FLOODCONTROL,LIMITUSERABOVE,LIMITUSERBELOW,CHECKQUEUE,STOPEXECUTOR]


	int changeValue
	String changeType
	String queueType
	int currentValue
	int floodControl
	boolean defaultComparator

	Priority currentPriority

	static constraints = {
		queue(nullable:true, validator: checkQueueType)
		changeType(inList:CHANGE_TYPES)
		changeValue(validator: checkValue ) //nullable:true,blank:true,
		queueType(nullable:true,inList:ReportsQueue.REPORT_TYPES)
		floodControl(nullable:true)
		currentPriority(nullable:true)
		defaultComparator(nullable:true)
	}

	String getQueueType() {
		if (!queueType && queue) {
			return queue.queueLabel
		} else {
			return queueType
		}
	}

	List getQueueList() {
		switch (changeType) {
			case STOPEXECUTOR:
				ReportsQueue.REPORT_TYPES-[ReportsQueue.ARRAYBLOCKING]
				break
			case MAXQUEUE:
				ReportsQueue.REPORT_TYPES
				break
			case POOL:
				ReportsQueue.REPORT_TYPES
				break
			case CHECKQUEUE:
				ReportsQueue.REPORT_TYPES
				break
			case PRESERVE:
			case LIMITUSERABOVE:
			case LIMITUSERBELOW:
			case DEFAULTCOMPARATOR:
			case FLOODCONTROL:
			default:
				ReportsQueue.REPORT_TYPES-[ReportsQueue.ARRAYBLOCKING,ReportsQueue.LINKEDBLOCKING]
				break
		}
	}
	protected def formatBean() {
		def map = loadDefaultValues(queue?.queueLabel)
		if (!currentValue) {
			currentValue= map?.value
		}
		floodControl=map?.floodControl
		if (!currentPriority) {
			currentPriority=map?.priority	?: Priority.MEDIUM
		}
		defaultComparator=map?.defaultComparator
	}

	JSON loadConfig() {
		def results = loadDefaultValues(queueType)
		return [value: results.value, priority:results.priority, floodControl:results.floodControl, defaultComparator:results.defaultComparator] as JSON
	}

	Map loadDefaultValues(String queueLabel) {
		Map results=[:]
		results.priority=Priority.MEDIUM
		results.value = 0
		results.floodControl=0
		results.defaultComparator=false
		def clazz
		switch (queueLabel) {
			case ReportsQueue.LINKEDBLOCKING:
				results=formatAdvanced(changeType,results,LinkedBlockingExecutor)
				break
			case ReportsQueue.ARRAYBLOCKING:				
				results=formatAdvanced(changeType,results,ArrayBlockingExecutor)				
				break
			case ReportsQueue.PRIORITYBLOCKING:				
				results=formatAdvanced(changeType,results,PriorityBlockingExecutor)
				break
			case ReportsQueue.ENHANCEDPRIORITYBLOCKING:				
				results=formatAdvanced(changeType,results,EnhancedPriorityBlockingExecutor)
				break
		}
		return results
	}

	private Map formatAdvanced(String changeType,Map results, Class executor) {
		switch (changeType) {
			case POOL:
				results.value = executor?.maximumPoolSize
				break
			case MAXQUEUE:
				results.value = executor?.maxQueue
				break
			case LIMITUSERABOVE:
				results.value = executor?.limitUserAbovePriority
				break
			case LIMITUSERBELOW:
				results.value = executor?.limitUserBelowPriority
				break
			case FLOODCONTROL:
				results.floodControl= executor?.forceFloodControl
			case DEFAULTCOMPARATOR:
				results.defaultComparator= executor?.defaultComparator
				break
			default:
				results.value = executor?.minPreserve
				results.priority=executor?.definedPriority
				break
		}
		return results
	}

	static def checkValue= {val, obj, errors ->
		if (val && val<0 && obj.changeType != CHECKQUEUE) {
			errors.rejectValue(propertyName, "queuekit.invalidConfigValue.error")
		}
	}

}
