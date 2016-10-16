package org.grails.plugin.queuekit.priority

import grails.util.Holders

import org.grails.plugin.queuekit.ReportsQueue


class AttachedRunnable implements Runnable {
	private volatile boolean shutdown

	private ReportsQueue queue
	private Map paramsMap

	public AttachedRunnable(ReportsQueue queue, Map paramsMap){
		this.queue = queue
		this.paramsMap = paramsMap
	}

	/**
	 * Runnable finds queue.reportName
	 * calls .execute on relevant service bound to reportName
	 */
	@Override
	public void run() {
		/* 
		 * This is where the task gets bound to end user generated service
		 * so if reportName was harryPotter
		 * it would generate 
		 * HarryPotter${queue.serviceLabel} where 
		 * 	-> queue.serviceLabel is bound to actual queue class
		 * 
		 * in the case of all reports this will be 
		 *  HarryPotterReportingService 
		 * as outlined in notes
		 */
		String name = queue.reportName+queue.serviceLabel
		Thread t
		try {
			def currentService=Holders.grailsApplication.mainContext.getBean(name)
			/*
			 * OUCH another thread here
			 * 
			 * but this is so that we can issue our unsafe stop 
			 * to then finally kill running task
			 * 
			 */
			t = new Thread({currentService.runReport(queue,paramsMap)} as Runnable)
			int i=0
			while (!t.isInterrupted() || !shutdown) {
				if (i==0) {
					t.start()
					i++
				}
				/**
				 * WARNING - This is deprecated usage of stop and is UNSAFE !.
				 * 
				 * But since we are lookup up read only DB information for reports 
				 * this should hopefully be fine to kill off
				 * 
				 */
				if (t.isInterrupted()||shutdown||!t.isAlive()) {
					break
				}
			}
			if (shutdown) {
				t.interrupt()
				t.stop()
			}
		} catch (InterruptedException e) {
			e.printStackTrace()
			t.interrupt()
			Thread.currentThread().interrupt()
			return
		}
	}

	public void shutdown() {
		shutdown = true
	}
}
