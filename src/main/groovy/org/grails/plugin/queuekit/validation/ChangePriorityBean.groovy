package org.grails.plugin.queuekit.validation

import grails.validation.Validateable

import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.Priority


class ChangePriorityBean implements Validateable {

	Priority priority
	ReportsQueue queue

	static constraints = {
		priority(nullable: true)
		queue(validator: checkQueueType)
	}

	static def checkQueueType= {val, obj, errors ->
		if (val && !val.isEnhancedPriority()) {
			errors.rejectValue(propertyName, "queuekit.invalidQueue.error")
		}
	}

}
