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

	int changeValue
	String changeType
	String queueType
	int currentValue
	int floodControl
	boolean defaultComparator

	Priority currentPriority

	static constraints = {
		queue(nullable:true, validator: checkQueueType)
		changeType(inList:QueuekitLists.CHANGE_TYPES)
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
			case QueuekitLists.STOPEXECUTOR:
				ReportsQueue.REPORT_TYPES-[ReportsQueue.ARRAYBLOCKING]
				break
			case QueuekitLists.POOL:
				ReportsQueue.REPORT_TYPES
				break
			case QueuekitLists.MAXQUEUE:
				ReportsQueue.REPORT_TYPES
				break
			case QueuekitLists.CHECKQUEUE:
				ReportsQueue.REPORT_TYPES
				break
			case QueuekitLists.PRESERVE:
			case QueuekitLists.LIMITUSERABOVE:
			case QueuekitLists.LIMITUSERBELOW:
			case QueuekitLists.DEFAULTCOMPARATOR:
			case QueuekitLists.FLOODCONTROL:
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
			case QueuekitLists.POOL:
				results.value = executor?.maximumPoolSize
				break
			case QueuekitLists.MAXQUEUE:
				results.value = executor?.maxQueue
				break
			case QueuekitLists.LIMITUSERABOVE:
				results.value = executor?.limitUserAbovePriority
				break
			case QueuekitLists.LIMITUSERBELOW:
				results.value = executor?.limitUserBelowPriority
				break
			case QueuekitLists.FLOODCONTROL:
				results.floodControl= executor?.forceFloodControl
			case QueuekitLists.DEFAULTCOMPARATOR:
				results.defaultComparator= executor?.defaultComparator
				break
			default:
				results.value = executor?.minPreserve
				results.priority=executor?.definedPriority
				break
		}
		return results
	}

	static def checkValue= { val, obj, errors ->
		if (val && val < 0 && obj.changeType != CHECKQUEUE) {
			errors.rejectValue(propertyName, "queuekit.invalidConfigValue.error")
		}
	}
}
