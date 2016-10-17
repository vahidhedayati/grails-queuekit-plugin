package org.grails.plugin.queuekit.examples.reports

import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.Priority
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService


class ParamsExampleReportingService extends QueuekitBaseReportsService {

	def tsvService

	def runReport(ReportsQueue queue,Map params) {
		def queryResults = tsvService.runParams(params)
		runReport(queue,queryResults,params)
	}

	def actionInternal(out,bean, queryResults,Locale locale) {
		actionReport1Report(out,bean,queryResults)
	}

	private void actionReport1Report(out,bean,queryResults) {
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
	
	/*
	 * 
	 * Overriding how QueuekitBaseReportsService names it here
	 */
	String getReportName(ReportsQueue queue,bean) {
		return "MyLovelyReport-${queue.id}.${reportExension}"
	}

}
