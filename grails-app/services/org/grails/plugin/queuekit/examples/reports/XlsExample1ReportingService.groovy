package org.grails.plugin.queuekit.examples.reports

import grails.web.databinding.DataBindingUtils
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.examples.Report2Bean
import org.grails.plugin.queuekit.priority.Priority
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService

/**
 * This is not a real XLS demonstration and will produce tsv file 
 * Please refer to provided demo site for a real XLS test.
 * Used for demonstration of priorityExecutor call
 * 
 * @author Vahid Hedayati
 *
 */
class XlsExample1ReportingService extends QueuekitBaseReportsService {

	def tsvService

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
					if (priority <= Priority.HIGH) {
						priority = priority.next()

					} else if (priority >= Priority.LOW) {
						priority = priority.next()
					}
				}
			}
		}
		return priority
	}

	def runReport(ReportsQueue queue,Map params) {

		/*
		 *  This is the service bound to index2 action of Report2Controller
		 *  The action is bound to report2Bean which is now bound back to params received
		 *  through the running job
		 */
		Report2Bean bean = new Report2Bean()

		/*
		 * Bind params back to real bean
		 *
		 */
		DataBindingUtils.bindObjectToInstance(bean, params)

		/*
		 * tsvService would generate an instanceList for runReport
		 */
		def queryResults = tsvService.runReport2(bean)

		/*
		 *  A call in the real ReportsService which binds back to
		 *  actionInternal below
		 *   
		 */
		runReport(queue,queryResults,bean)
	}

	def actionInternal(out,bean, queryResults,Locale locale) {
		/*
		 * This can be your own call method as generated below
		 * 
		 */
		actionReport2Report(out,bean,queryResults)
	}


	/*
	 * You would need to add this for real XLS files
	 * 
	 */
	// String getReportExension() {
	//	return 'xls'
	// }

	/*
	 * It goes through a variety of in/out of QueuekitBaseReportsService finally it has real results
	 * here and the real bean which it displays results for
	 */
	private void actionReport2Report(out,Report2Bean bean,queryResults) {

		/*
		 *  Please REFER TO DEMO SITE - test-queue-plugin
		 *  The demo site has apache-poi libraries and fully demonstrates call
		 *      
		 */

		out << 'name\t'
		out << bean.report
		out << '\rtext\t'
		out << bean.sample
		out << '\r'
		queryResults?.each{field->
			out << field.id << '\t'
			out << field.text << '\t'
			out << '\r'
		}
		out.flush()
		out.close()
	}
}
