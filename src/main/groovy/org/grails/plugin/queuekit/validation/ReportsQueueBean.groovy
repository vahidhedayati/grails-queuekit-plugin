package org.grails.plugin.queuekit.validation

import grails.converters.JSON
import grails.util.Holders
import grails.validation.Validateable

import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.Priority


class ReportsQueueBean implements Validateable {

	def queuekitUserService = Holders.grailsApplication.mainContext.getBean('queuekitUserService')
	def grailsLinkGenerator = Holders.grailsApplication.mainContext.getBean('grailsLinkGenerator')

	def id
	String reportName
	String displayName
	String paramsMap
	Long userId
	Locale locale
	String fileName
	Date start				// null means never send
	Date created
	Date requeued
	Integer retries
	Date finished
	Priority priority
	String username
	String queueType
	byte manualDownload=0

	String fromController
	String fromAction

	String reportType		// Required only for binding to buildReport request

	String status=ReportsQueue.QUEUED

	private static final long serialVersionUID = 1L


	static constraints={
		id(nullable:true,bindable:true)
		status(maxSize:1,inList:ReportsQueue.REPORT_STATUS)
		requeued(nullable:true)
		start(nullable:true)
		finished(nullable:true)
		fileName(nullable:true)
		userId(nullable:true)
		priority(nullable:true, inList:Priority.values())
		username(nullable:true)
		queueType(nullable:true)
		reportType(nullable:true)
		fromController(nullable:true)
		fromAction(nullable:true)
	}

	/*
	 * sets up the bean according to a DB entry 
	 */
	def formatBean(queue) {
		id=queue.id
		reportName=queue.reportName
		displayName=queue.displayName
		paramsMap=queue.paramsMap
		userId=queue.userId
		locale=queue.locale
		fileName=queue.fileName
		start=queue.start
		status=queue.status
		queueType=queue.queueLabel
		retries=queue.retries
		
		if (queue.manualDownload) {
			manualDownload=queue.manualDownload
		}
		
		if (queue.hasPriority()) {
                    priority = queue?.priority ?:queue?.defaultPriority
		}
		
		username=queuekitUserService.getUsername(userId)
		fromAction=queue?.fromAction
		fromController=queue?.fromController
		return this
	}

	/*
	 * When you use the buildReport(bean) method
	 * you first create a new instance of this bean and add
	 * your values to it.
	 * When it buildsReport it calls this bindReport
	 * which gives back the required values as map to 
	 * ongoing function
	 * 
	 */
	protected Map bindReport() {
		def values=[:]
		values.with {
			reportName=this.reportName
			userId=this.userId
			locale=this.locale
			priority=this.priority
			reportType=this.reportType
			fromController=this.fromController
			fromAction=this.fromAction
		}
		values.paramsMap=JSON.parse(this.paramsMap)
		return values
	}

	/*
	 * Binds bean to a given params
	 * Converts a real params map to a JSON string
	 * which is then compatible with object type
	 */
	protected def bindBean(Map values) {
		if (values.id) {
			id=values.id
		}
		reportName=values.reportName
		paramsMap=(values.paramsMap as JSON).toString()
		userId=values.userId
		locale=values.locale
		fileName=values.fileName
		start=values.start
		if (values.priority) {
			priority=values.priority
		}
		//manualDownload=values.manualDownload
		status=ReportsQueue.QUEUED
		fromAction=values.fromAction
		fromController=values.fromController
		return this
	}

	/*
	 * Sets a map to string JSON
	 */
	void setParamsMap(Map t) {
		paramsMap=(t as JSON).toString()
	}

	/* 
	 * Returns JSON object that was stored as string back as 
	 * a map
	 */
	Map getParameters() {
		return JSON.parse(paramsMap)
	}

	/*
	 * Used by _showContent.gsp
	 * Converts fromController/action params 
	 * into a URL GET line
	 */
	String getFormAsUrl() {
		if (fromController && fromAction && paramsMap) {
			def url = grailsLinkGenerator.link(controller: fromController, action: fromAction, params: parameters, absolute: 'true')
			return """<a href="${url}" target="_newWindow">HTTP-GET ${reportName}</a>"""
		}
	}
	/*
	 * Used by _showContent.gsp
	 * Converts fromController/action params
	 * into a post form
	 */
	String getFormAsForm() {
		if (fromController && fromAction && paramsMap) {
			def url = grailsLinkGenerator.link(controller: fromController, action: fromAction)
			StringBuilder form = new StringBuilder()
			form.append("""<form method="post" action="${url}" target="_newWindow">""")
			parameters?.each{k,v->
				form.append("""<input type="hidden" name="${k}" value="${v}">""")
			}
			form.append("""<input type="submit" value="HTTP-POST ${reportName}"></form>""")
			return form.toString()
		}
	}
}