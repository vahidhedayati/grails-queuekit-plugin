package org.grails.plugin.queuekit.examples.reports

import grails.web.databinding.DataBindingUtils
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.examples.Report1Bean
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService

class TsvExample1ReportingService extends QueuekitBaseReportsService {

	def tsvService

	def runReport(ReportsQueue queue,Map params) {
		/*
		 * This is the service bound to index action of Report1Controller
		 * The action is bound to report1Bean which is now bound back to params received
		 * through the running job
		 * 
		 */
		Report1Bean bean = new Report1Bean()

		/*
		 * Bind params back to real bean
		 * 
		 */
		DataBindingUtils.bindObjectToInstance(bean, params)

		/*
		 * tsvService would generate an instanceList for runReport
		 */
		def queryResults = tsvService.runReport(bean)

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
	private void actionReport1Report(out,Report1Bean bean,queryResults) {
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
