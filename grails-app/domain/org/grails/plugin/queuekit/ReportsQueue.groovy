package org.grails.plugin.queuekit

import grails.converters.JSON

import org.grails.plugin.queuekit.priority.Priority

/**
 * When a request is made to save or run something on the reportsQueue
 * it is saved here. The main object extends over 3 different calls and classes 
 * found in this folder
 * 
 * @author Vahid Hedayati
 *
 */
class ReportsQueue {

	String reportName		//typical report will use reportName
	String displayName		// shared reports can set displayName
	String paramsMap
	Long userId				//could be bound to spring security userId or apacheShiro
	Locale locale
	String fileName
	Date start				// null means never send
	Date created
	Date requeued
	Integer retries			//keep a tab on user clicking requeue
	Integer queuePosition
	
	String fromController
	String fromAction
	
	Byte manualDownload

	Date finished

	String status=QUEUED


	private static final long serialVersionUID = 1L

	static final String ENHANCEDPRIORITYBLOCKING='E'
	static final String PRIORITYBLOCKING='P'
	static final String LINKEDBLOCKING='L'
	static final String ARRAYBLOCKING='A'
	static final List REPORT_TYPES=[ENHANCEDPRIORITYBLOCKING,PRIORITYBLOCKING,LINKEDBLOCKING,ARRAYBLOCKING]
	
	
	static final String QUEUED='QU'
	static final String ERROR='ER'
	static final String RUNNING='RU'
	static final String CANCELLED='CA'
	static final String COMPLETED='CO'
	static final String DELETED='DE'
	static final String DOWNLOADED='DO'
	static final String NORESULTS='NR'
	static final String OTHERUSERS='OU'
	
	
	static final List REPORT_STATUS_ALL=[QUEUED,DOWNLOADED,RUNNING,COMPLETED,DELETED,ERROR,CANCELLED,NORESULTS,OTHERUSERS]
	static final List REPORT_STATUS=REPORT_STATUS_ALL-[DELETED,OTHERUSERS]
	
	static constraints={
		status(maxSize:2,inList:REPORT_STATUS_ALL)
		requeued(nullable:true)
		start(nullable:true)
		finished(nullable:true)
		fileName(nullable:true)
		displayName(nullable:true)
		retries(nullable:true)
		queuePosition(nullable:true)
		userId(nullable:true)
		fromController(nullable:true)
		fromAction(nullable:true)
		manualDownload(nullable:true)
	}

	String getQueueLabel() {
		return null
	}
	
	Priority getDefaultPriority() {
		return QueuekitHelper.sortPriority(reportName) ?: Priority.LOW
	}
	
	/**
	 * defaults to 'ReportingService'
	 * can be overriden by classes that will use different convention
	 * i.e. EmailService
	 * @return
	 */
	String getServiceLabel() {
		return 'ReportingService'
	}
	
	static mapping={
		status(sqlType:'char(2)')
		//manualDownload(sqlType:'bit(1)')
		start(index:'reports_queue_start_idx')
	}
	
	String toString() {
		return "${reportName}-${created}"
	}
	
	Boolean hasPriority() {
		return false
	}
	
	Boolean isEnhancedPriority() {
		return false
	}
	
	Map getParamsAsMap() {
		return JSON.parse(paramsMap)
	}
}
