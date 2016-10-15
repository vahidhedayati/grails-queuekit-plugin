package org.grails.plugin.queuekit.event

import org.springframework.context.ApplicationEvent

class ArrayBlockingQueuedEvent  extends ApplicationEvent {

	ArrayBlockingQueuedEvent(source) {
		super(source)
	}
	
}
