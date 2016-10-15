package org.grails.plugin.queuekit.event

import org.springframework.context.ApplicationEvent

class LinkedBlockingQueuedEvent  extends ApplicationEvent {
	
	LinkedBlockingQueuedEvent(source) {
		super(source)
	}
}
