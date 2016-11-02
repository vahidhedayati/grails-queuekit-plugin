package org.grails.plugin.queuekit.examples.reports

import grails.web.databinding.DataBindingUtils
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.examples.Report3Bean
import org.grails.plugin.queuekit.priority.Priority
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService

class CsvExampleReportingService extends QueuekitBaseReportsService {

	def tsvService

	/**
	 * This overrides the default priority of the report set by
	 * QueuekitBaseReportsService
	 *
	 * By default it is either as per configuration or if not by default
	 * LOW priority.
	 *
	 * At this point you can parse through your params and decide if in this example
	 * that the given range fromDate/toDate provided is within a day make report
	 * HIGHEST
	 * if within a week HIGH and so on
	 *
	 * This priority check takes place if you are using
	 * standard standardRunnable = false if your report default type is
	 * EnhancedBlocking
	 * if disableUserServicePriorityCheck=false and standardRunnable = true
	 * then it should use the priority method very similar to this in
	 *
	 * queuekitUserService. This is the service you are supposed to extend
	 * and declare as a bean back as queuekitUserService.
	 *
	 * Then you can control priority through this service call and a more
	 * centralised control can be configured/setup.
	 *
	 */
	Priority getQueuePriority(ReportsQueue queue, Map params) {
		Priority priority = queue.priority ?: queue.defaultPriority
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
					if (priority <= Priority.LOWEST) {
						priority = priority.previous()
					} else if (priority >= Priority.LOW) {
						priority = priority.next()
					}
				}
			}
			log.debug "priority is now ${priority} was previously ${priority} difference of date : ${difference}"
		}
		return priority
	}


	/*
	 * We must define the report type file extension
	 * default is tsv this being CSV needs to be defined
	 * 
	 */
	String getReportExension() {
		return 'csv'
	}


	def runReport(ReportsQueue queue,Map params) {
		/*
		 * This is the service bound to index action of Report1Controller
		 * The action is bound to report1Bean which is now bound back to params received
		 * through the running job
		 * 
		 */
		Report3Bean bean = new Report3Bean()

		/*
		 * Bind params back to real bean
		 * 
		 */
		DataBindingUtils.bindObjectToInstance(bean, params)

		/*
		 * tsvService would generate an instanceList for runReport
		 */
		def queryResults = tsvService.runReport3(bean)

		/*
		 *  A call in the real ReportsService which binds back to
		 *  actionInternal below
		 *   
		 */
		runReport(queue,queryResults,bean)
	}

	def actionInternal(ReportsQueue queue,out,bean, queryResults,Locale locale) {
		actionReport1Report(out,bean,queryResults)
	}

	/*
	 * It goes through a variety of in/out of QueuekitBaseReportsService finally it has real results
	 * here and the real bean which it displays results for
	 * 
	 */
	private void actionReport1Report(out,Report3Bean bean,queryResults) {
		out << 'name,'
		out << bean?.report
		out << '\rtext,'
		out << bean?.sample
		out << '\rreportType,'
		out << bean?.reportType
		out << '\rpriority,'
		out << bean?.priority
		out << '\rfromDate,'
		out << bean?.fromDateRaw
		out << '\rtoDate,'
		out << bean?.toDateRaw
		out << '\r'
		queryResults?.each{field->
			out << field.id << ','
			out << field.text << ','
			out << '\r'
		}
		out.flush()
		out.close()
	}


	/**
	 * In-order to override defined file name we have created a new function 
	 * to set the name according to something in Report3Bean.
	 * When user completes index8 if they put in test1 or test2 something predefined
	 * will be generated as fileNames otherwise what ever is in that box will be part of 
	 * generated fileName -  
	 * @param bean
	 * @return
	 */
	String getFileName(bean) {
		String filename
		if (bean.report=='test1') {
			filename='someName'
		} else if (bean.report=='test2') {
			filename='someOtherName'
		} else {
			filename=bean.report
		}
		return filename
	}

	/**
	 * This is overriding main call in QueuekitBaseReportsService and using
	 * above to preset fileName to be something else 
	 * when user clicks to download fileName will be this 
	 * 
	 */
	String getReportName(ReportsQueue queue,bean) {
		return "${getFileName(bean)?:queue.reportName}-${queue.id}.${reportExension}"
	}

}
