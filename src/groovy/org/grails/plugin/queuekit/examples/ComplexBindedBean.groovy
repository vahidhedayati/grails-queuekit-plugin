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
	
	//User user
	
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
		 *  This is actually a bad example sine I am binding a src/groovy class which usually doesn't have an id and these classes 
		 *  are not the problem.
		 *  
		 *  
		 *  This would for when you declare/bind to a real domain classes which does have a real id
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
		
		/**
		 * Real example is here
		 * but since User domainClass does not exist 
		 * in the plugin it has been left as a sample 
		 *  
		 * Not really tested  but above example is binding a bean and I don't 
		 * think it is required it was available and used as an example
		 * 
		 * When you define <g:textField name="user.id" valued="${instance.user.id}"/>
		 * Then user.id is sent in params in this format 
		 * as shown below  [user.id: 1, user:[id: 1]]
		 * 
		 * and we are converting our bean to be exactly the same manner as params 
		 * would be if posted. This is picked up by the given end service.
		 * Take a look at CsvExampleReportingService as an example, it happens here:
		 *  DataBindingUtils.bindObjectToInstance(bean, params) 
		 */
		//map."user.id"=user.id
		//map.user=[id:user.id]
		
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
