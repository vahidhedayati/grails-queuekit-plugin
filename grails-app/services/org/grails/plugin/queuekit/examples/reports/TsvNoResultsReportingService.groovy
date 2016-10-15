package org.grails.plugin.queuekit.examples.reports

import java.util.Map;

import org.codehaus.groovy.grails.web.binding.DataBindingUtils
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.examples.Report2Bean
import org.grails.plugin.queuekit.priority.Priority;
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService


class TsvNoResultsReportingService extends QueuekitBaseReportsService {

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
				}
			}
		}
		return priority
	}
	def runReport(ReportsQueue queue,Map params) {
		/*
		 * This is the service bound to index action of Report1Controller
		 * The action is bound to report1Bean which is now bound back to params received
		 * through the running job
		 * 
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
		def queryResults = tsvService.runNoReport(bean)

		/*
		 *  A call in the real ReportsService which binds back to
		 *  actionInternal below
		 *   
		 */
		runReport(queue,queryResults,bean)
	}

	def actionInternal(out,bean, queryResults,Locale locale) {
		actionReport1Report(out,bean,queryResults)
	}

	/*
	 * It goes through a variety of in/out of QueuekitBaseReportsService finally it has real results
	 * here and the real bean which it displays results for
	 * 
	 */
	private void actionReport1Report(out,Report2Bean bean,queryResults) {
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
