package org.grails.plugin.queuekit

import grails.util.Holders

class ReportRunnable implements Runnable {


	private ReportsQueue queue

	public ReportRunnable(ReportsQueue queue){
		this.queue=queue
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
		try {
			def currentService =  Holders.grailsApplication.mainContext.getBean(name)
			currentService.executeReport(queue)
		} catch (InterruptedException e) {
			e.printStackTrace()
		}
	}
}