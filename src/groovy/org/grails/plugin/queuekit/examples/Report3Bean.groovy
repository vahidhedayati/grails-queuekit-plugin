package org.grails.plugin.queuekit.examples

import grails.util.Holders
import grails.validation.Validateable

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.Priority

// This is your validation bean for user selecting a report front end
// not really provided here but results are returned as a map to frontend caller
@Validateable
class Report3Bean {
	 
	def g = Holders.grailsApplication.mainContext.getBean(ApplicationTagLib)
	
	String report='report3'
	String sample='Report 3 will produce xls'
	Long userId=1
	Date fromDate=new Date()
	Date toDate=new Date()+15
	Priority priority = Priority.HIGHEST
	String reportType = ReportsQueue.ENHANCEDPRIORITYBLOCKING
	
	String countrySelected

	static List countries = [
		[name: 'US', value:'United States'],
		[name: 'FR', value:'France'],
		[name: 'NL', value:'Netherlands']
	]
	
	String getFromDate() {
		return fromDate.format(message)
	}
	
	String getToDate() {
		return toDate.format(message)
	}
	
	String getFromDateRaw() {
		return fromDate
	}
	
	String getToDateRaw() {
		return toDate
	}
	
	void setFromDate(String t) {
		 SimpleDateFormat sf = new SimpleDateFormat(message)
		fromDate= sf.parse(t)
	}
	
	void setToDate(String t) {
		 SimpleDateFormat sf = new SimpleDateFormat(message)
		toDate= sf.parse(t)
	}
	
	String getMessage() {
		return g.message(code:'queuekit.defaultDate.format', default: 'dd MMM yyyy')
	}
	
	static constraints = {
		countrySelected(nullable:true, inList:countries.name)
		reportType(inList:ReportsQueue.REPORT_TYPES)
	}
	Map loadValues() {
		def map = [:]
		map.report=report
		map.sample=sample
		map.countrySelected=countrySelected
		map.fromDate=fromDate //getFromDate()
		map.toDate=toDate //getToDate()
		
		map.reportType=reportType
		map.priority=priority
		//map."priority.value"=priority.value
		//map.priority=[value:priority.value]
		return map
	}


}