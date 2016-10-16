package org.grails.plugin.queuekit.priority

import groovy.transform.CompileStatic

@CompileStatic
public enum Priority {

	HIGHEST(0),
	HIGH(1),
	MEDIUM(2),
	LOW(3),
	LOWEST(4),
	REALLYSLOW(5)

	int value

	Priority(int val) {
		this.value = val
	}

	public int getValue(){
		return value
	}
	static Priority byValue(int val) {
		values().find { it.value == val }
	}
}