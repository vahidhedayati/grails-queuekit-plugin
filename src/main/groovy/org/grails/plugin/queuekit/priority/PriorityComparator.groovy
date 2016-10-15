package org.grails.plugin.queuekit.priority

import org.grails.plugin.queuekit.QueuekitHelper


class PriorityComparator implements Comparator<ComparableFutureTask>{


	@Override
	public int compare(final ComparableFutureTask lhs, final ComparableFutureTask rhs) {
		if(lhs instanceof ComparableFutureTask && rhs instanceof ComparableFutureTask) {
			int returnCode=0
			if (lhs.priority < rhs.priority) {
				returnCode=-1
			} else if (lhs.priority > rhs.priority) {
				returnCode=1
			}
			if (lhs.priorityBlockingExecutor.defaultComparator) {
				/**
				 * This is the defaultComparator and will return above result
				 * nice and simple
				 */
				return returnCode
			} else {
				/**
				 * This is some complex stuff going on to figure out if the current item should have a slot free
				 * and given a slot if it should
				 *
				 * It has taken me a few days of going round to come up with this logic
				 *
				 * quite likely to have things changed within changeMaxPoolSize in future
				 *
				 * is also used by Actual PriorityBlockingExecutor as request comes in
				 */
				boolean slotsFree = QueuekitHelper.changeMaxPoolSize(lhs.priorityBlockingExecutor,lhs.userId,lhs.maxPoolSize,lhs.minPreserve,lhs.priority,lhs.definedPriority,
						lhs.priorityBlockingExecutor.getActiveCount(),lhs.priorityBlockingExecutor.getCorePoolSize())

				/**
				 * Unsure about this feel free to give input
				 * as to if this should be actual return code or
				 * maybe this hack is working as expected.
				 *
				 */
				if (returnCode>0 && slotsFree) {
					returnCode=-1
				}
				return returnCode

			}
		}
	}
}