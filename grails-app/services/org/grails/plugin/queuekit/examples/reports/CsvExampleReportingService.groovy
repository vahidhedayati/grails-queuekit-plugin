package org.grails.plugin.queuekit.examples.reports

import grails.web.databinding.DataBindingUtils
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.examples.Report3Bean
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService

class CsvExampleReportingService extends QueuekitBaseReportsService {

	def tsvService


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

	def actionInternal(out,bean, queryResults,Locale locale) {
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
		out << bean?.fromDate
		out << '\rtoDate,'
		out << bean?.toDate
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
