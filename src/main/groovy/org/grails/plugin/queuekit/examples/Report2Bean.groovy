package org.grails.plugin.queuekit.examples

import grails.validation.Validateable

// This is your validation bean for user selecting a report front end
// not really provided here but results are returned as a map to frontend caller 

class Report2Bean  implements Validateable {
	
	String report='report2'
	String sample='Report 2 will produce xls'
	String reportType
	
	Map loadValues() {
		def map = [:]
		map.report=report
		map.sample=sample
		map.reportType=reportType
		return map
	}
	
}
