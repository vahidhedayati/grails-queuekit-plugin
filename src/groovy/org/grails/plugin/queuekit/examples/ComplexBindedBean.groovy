package org.grails.plugin.queuekit.examples

import grails.util.Holders
import grails.validation.Validateable

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.Priority


@Validateable
class ComplexBindedBean {
	 
	Long userId
	String report='ComplexBinding'
	int otherResult=0
	Report2Bean otherBean
	
	static constraints = {
		userId(nullable:true)
	}
	
	protected def formatBean() {
		/**
		 * Assuming you have some other function that binds stuff
		 *  into the bean and is then needed for the report
		 *  so for example before it loadsValues it formatsBean but before it formatsBean
		 *  it actually sets userId 
		 *  and otherResults then gets set according to userId
		 *  
		 *  This is just an example to explain that sometimes you need to do something else so you need to then load values 
		 *  from bean into the controller doing the call
		 * 
		 */
		
	}
	
	Map loadValues() {
		def map = [:]
		map.report=report
		/*
		 * This is where otherResult may have been set by formatBean 
		 * hence wasn't actually an originating params
		 */
		map.otherResults=otherResult
		
		/*
		 * Now we need to also set
		 * otherBean
		 * But if we set otherBean by
		 * 
		 * map.otherBean = otherBean
		 * 
		 * This will then cause problems with the plugin converting 
		 * the actual bean 
		 * 
		 * so instead we need to imitate how params does it:
		 */
		
		map."otherBean.id"=otherBean.id
		map.otherBean=[id:otherBean.id]
		
		/*
		 * With above set when it binds back to the bean everything will work as expected
		 * 
		 * Then you call bean.loadValues(): 
		 * 	def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues())			
		 * instead of:
		 *  def queue = queueReportService.buildReport(reportName,userId , locale, params)
		 * 
		 */
		
		return map
	}


}
