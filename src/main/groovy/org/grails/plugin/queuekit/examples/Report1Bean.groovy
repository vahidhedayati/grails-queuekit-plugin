package org.grails.plugin.queuekit.examples

import grails.validation.Validateable

// This is your validation bean for user selecting a report front end
// not really provided here but results are returned as a map to frontend caller 

class Report1Bean  implements Validateable {
	// this is the front end params captured
	// this would be what they have selected
	String report='report1'
	String sample='This will produce tsv file'
	String reportType
	String priority
	
	//This returns the params as a map 
	// a bit like params itself but obviously it has gone through the bean
	// and may have had validation in real controller etc
	Map loadValues() {
		def map = [:]
		map.report=report
		map.sample=sample
		map.reportType=reportType
		return map
	}
	static constraints = {
		priority(nullable:true)
	}

}
