package org.grails.plugin.queuekit.event

import org.springframework.context.ApplicationEvent

class EnhancedPriorityBlockingQueuedEvent  extends ApplicationEvent {
	
	EnhancedPriorityBlockingQueuedEvent(source) {
		super(source)
	}

}
