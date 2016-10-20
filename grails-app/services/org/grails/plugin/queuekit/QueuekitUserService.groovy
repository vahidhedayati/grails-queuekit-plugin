package org.grails.plugin.queuekit

import grails.util.Holders

import java.text.SimpleDateFormat

import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.grails.plugin.queuekit.priority.Priority

/**
 * ! - IMPORTANT NOTICE - !
 * 
 * The only thing that you should need to extend / 
 * modify behaviour of is all in this class.
 * 
 * So In grails 2 create a new Service i.e :
 *  test/MyUserService
 *  
 *  Open conf/spring/resources.groovy and add following 
 import test.MyUserService
 beans = {	
 queuekitUserService(MyUserService)
 }
 * 
 * Your MyUserService will be similar to:
 * 
 * 
 * 	package test
 import grails.plugin.queuekit.QueuekitUserService
 import grails.plugin.queuekit.ReportsQueue
 import grails.plugin.queuekit.priority.Priority
 class MyUserService extends QueuekitUserService {
 -> Now copy each or all segments below over to it 
 -> Override the methods it would seem at least : 
 currentUser/superUser/username 
 would need to be configured to pass information
 back from within your application.
 The segments further down relate to a rule check done by update feature of 
 queueReportService. And allow you to fine tune priorities beyond configured 
 values
 * 
 *  
 * 
 * 
 * @author Vahid Hedayati
 *
 */

class QueuekitUserService {

	def g = Holders.grailsApplication.mainContext.getBean(ApplicationTagLib)

	/*
	 * Override this service and method to return your real user
	 * must return their userId 
	 */
	Long getCurrentuser() {
		return 1L
	}

	/*
	 * Overrider this method to then ensure superUser
	 * Privileges are only given to superUser's as per your definition
	 * if it is a security group or some user role.
	 */
	boolean isSuperUser(Long userId) {
		return userId==1L
		
	}

	/*
	 * Override this to get the real users UserName
	 * Return String username bound to userId (long digit)
	 */
	String getUsername(Long userId) {
		return ''
	}
	
	Long getRealUserId(String searchBy) {
		/*
		 * 
		 * We are looking for the real user Id that is bound 
		 * to given search username - you may need to UsernameLike 
		 * or something that matches how you want the search to work
		 * best
		 * 
		 * User = User.findByUsername(searchBy)
		 * if (user)
		 * return user.id
		 * 
		 */
		return 1L
	} 

	/*
	 * Override this to return a locale for your actual user
	 * when running reports if you have save their locale on the DB
	 * you can override here it will be defaulted to null and set to 
	 * predfined plugin value in this case
	 * 
	 */
	Locale  getUserLocale(Long userId) {
		return null
	}

	/**
	 * !! ATTENTION !! 
	 * You are not required to override  :
	 * reportPriority or checkReportPriority
	 * 
	 * If you have not enabled standardRunnable or 
	 * set standardRunnable = false
	 * then this aspect does not need to be touched.
	 * 
	 * If you decide to use standardRunnable=true
	 * 
	 * Then override this method in your local extended
	 * version of this class. 
	 *  
	 * This will be your last chance for this scenario to capture
	 * and override the report status.
	 * 
	 * This is an alternative method than using the provided default.
	 * If you wish to configure reports in this manner centralised.
	 * 
	 * You will still need to declare 
	 * Priority getQueuePriority in your extends reportService. 
	 * 
	 * But take a look at ParamsExampleReportingService since 
	 * you can keep it plain
	 * 
	 * -------------------------------------------------------------
	 * How it works
	 * 
	 * Whilst you can configure a report to have LOW priority
	 * It could be that it needs to be LOW for long term date range
	 * but HIGH for a short 1 day lookup
	 * 
	 * This is a final stage before actual priority is selected
	 * which if not found here will be actual report default
	 * as defined in configuration if not by plugin default choice LOW
	 */
	Priority reportPriority(ReportsQueue queue, Priority givenPriority, params) {
		Priority priority

		if (queue.hasPriority()) {

			priority = queue.priority ?: queue.defaultPriority

			if (givenPriority < priority) {
				priority = givenPriority
			}

			//if (priority > Priority.HIGHEST) {
			switch (queue.reportName) {
				//case 'tsvExample2':
				//priority = checkReportPriority(priority,params)
				//	break
				case 'csvExample':
					priority = checkReportPriority(priority,params)
					break
				//case 'xlsExample1':
				//priority = checkReportPriority(priority,params)
				//	break
			}
			//}
		}
		return priority
	}

	/*
	 * refer to above 
	 * A demo of how to try to override a report's priority
	 * in this case based on from/to Dates 
	 * 
	 * It maybe you have more refined range periods and a rule that 
	 * anything beyond a certain level regardless of current position
	 * 
	 * This is really a scribble but maybe a good starting point 
	 * 
	 */
	Priority checkReportPriority(Priority priority,params) {
		if (params.fromDate && params.toDate) {
			Date toDate = parseDate(params.toDate)
			Date fromDate = parseDate(params.fromDate)
			int difference = toDate && fromDate ? (toDate - fromDate) : null
			if (difference||difference==0) {
				if (difference <= 1) {
					// 1 day everything becomes HIGH priority
					priority = Priority.HIGH
				} else if  (difference >= 1 && difference <= 8) {
					if (priority == Priority.HIGHEST) {
						priority = Priority.HIGH
					} else if (priority >= Priority.MEDIUM) {
						priority = priority.value.previous()
					}
				} else if  (difference >= 8 && difference <= 31) {
					if (priority <= Priority.HIGH) {
						priority = Priority.MEDIUM
					} else if (priority >= Priority.LOW) {
						priority = priority.next()
					}
				} else if  (difference >= 31 && difference <= 186) {
					if (priority >= Priority.MEDIUM && priority <= Priority.HIGHEST) {
						priority = priority.next()
					} else if (priority >= Priority.LOW) {
						priority = priority.previous()
					}
				} else if  (difference >= 186) {
					if (priority <= Priority.HIGH) {
						priority = priority.next()

					} else if (priority >= Priority.LOW) {
						priority = priority.next()
					}
				}
			}
			log.debug "priority is now ${priority} was previously ${priority} difference of date : ${difference}"
		}		
		return priority
	}

	Date parseDate(Date t,String format=null) {
		return t
	}

	Date parseDate(String t, String format=null) {
		if (!format) {
			format=g.message(code:'queuekit.defaultDate.format', default: 'dd MMM yyyy')
		}
		SimpleDateFormat sf = new SimpleDateFormat(format)
		sf.setLenient(false)
		if (sf)	return sf.parse(t)
	}
}
