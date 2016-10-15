package org.grails.plugin.queuekit.event

import org.springframework.context.ApplicationEvent

class PriorityBlockingQueuedEvent  extends ApplicationEvent {
	
	PriorityBlockingQueuedEvent(source) {
		super(source)
	}
}
