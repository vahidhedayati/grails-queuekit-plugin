package org.grails.plugin.queuekit.examples

import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.Priority
import org.grails.plugin.queuekit.validation.ReportsQueueBean
import org.springframework.web.servlet.support.RequestContextUtils

class ReportDemoController {

	def queueReportService
	def queuekitUserService
	
	def beforeInterceptor = {
		[action:this.&checkEnabled()]
	}
	
	def checkEnabled() {
		if (config.disableExamples) {
			redirect(action: 'notFound')			
			return
		}
	}
	def notFound() {
		render status:response.SC_NOT_FOUND
		return
	}

	//used to show how it would be typically
	def tsvService

	private String VIEW='/reportDemo/index'

	def index() {
		render view:VIEW
	}
	
	/**
	 * Using params as the input source with the plugin
	 * 
	 * Review ParamsExampleReportingService to pick up where
	 * the buildReport pumps into.
	 * 
	 * 'paramsExample1' = ParamsExample1ReportingService  
	 * Create whatever the name is a service for it:
	 * 
	 * @return
	 */
	def basicDemo() {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'paramsExample'

		/*
		 * these are your own params really
		 */
		params.report='Params examples'
		params.sample='Some sample text'
		
		//No queue defined - by default Priority
		def queue = queueReportService.buildReport(reportName,userId , locale, params)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:params]
	}

	/*
	 * This is the first action demonstrating Priority Blocking 
	 * tsvExample1 is configued as Priority.HIGH in Config.groovy
	 * This will top any slow requests - you need to fill default queue
	 * limit with 3 slow reports
	 * 
	 * so in effect 6 slow then 5 of these you should see 
	 * after 3 of slow these get picked up then back
	 * to last 3 slow ones
	 * 
	 */
	def index1(Report1Bean bean) {
		if (bean.hasErrors()) {
			render view:'/some/Path/to/Display/ReportSelection',model:[bean:bean]
			return
		}

		def locale = RequestContextUtils.getLocale(request)

		//Dummy test we are always admin
		def userId = queuekitUserService.currentuser

		/** 
		 * Important Whatever is set as the report name the reportService 
		 * then must match name +Reporting
		 * in this example report is 'tsvExample1' 
		 * So we must have available TsvExample1ReportingService 
		 * which follows the provided example and extends ReportsService
		 * 
		 * This gets wired in through an events triggered in 
		 * update(ReportsQueueBean bean, ReportsQueue queue)
		 * in QueueReportService
		 * 
		 */
		String reportName = 'tsvExample1'
		// by extending ReportsService
		def queue
		if (bean.priority) {
			/*
			 *  This is for EnhancedPriority TSV Priority as per configuration example
			 *  It will attempt to load up priority as per configuration or if not by default LOW 
			 */
			
			queue =queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),bean.reportType)
		} else {
			/*
			 * This is all other index1 example calls, all have been set to be high priority calls so we
			 * override actual Config.groovy value with this Priority block addition
			 * 
			 */
		 	queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),Priority.HIGH,bean.reportType)
		}

		
		/*
		 * This primary example demonstrates the most complex scenario
		 * We are imitating a real download through this process.
		 * 
		 * Only if main ThreadExecutor called is down and is not ArrayBlocking
		 * Only if config of useEmergencyExecutor is set to false or emergencyExecutor somehow not being launched
		 * 
		 * Then it would hit here where it would look at the state of queue if it has manualDownload set in DB.
		 * It means something i.e. above gone wrong. 
		 * 
		 * The override of manualDownloadEnabled would have told the plugin to actually go ahead and produce report
		 * as if the user had clicked on controller and was waiting
		 * 
		 * The for loop will imitate their delay.
		 * 
		 * You can test by 
		 * 	stopping ThreadExecutor
		 * 	queuekit.useEmergencyExecutor=false  // in Config.groovy
		 * 
		 * Then running this index followed by index2 - whilst both will be 
		 * under same circumstance this segment of code will imitate download where as index2 will just say running
		 * the end user will be not aware there is issues and will still collect file as they would from the queueKit controller listing
		 * 
		 */
		if (config.manualDownloadEnabled==true) {
			sleep(1000)
			ReportsQueue.withNewSession {
				ReportsQueue queue1 = ReportsQueue.get(queue.id as Long)
				if (queue1.manualDownload && queue1.manualDownload==1) {
					boolean haveFile=false
					for( int i = 0 ; (config.manualWaitTime ?: 1600) && !haveFile ; i++ ) {
						ReportsQueue.withNewSession {
							queue1 = ReportsQueue.get(queue.id)
							if (queue1.finished) {
								haveFile=true
							}
						}
						sleep(60)
					  }
					  
					if (haveFile) {						
						redirect(controller:'queueKit', action:'download', id:queue.id)
						return
					}
					flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
					render view:VIEW, model:[bean:bean]
					return
				}
				flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
				render view:VIEW, model:[bean:bean]
			}
		}

		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}


	def indexBean(Report1Bean bean) {
		if (bean.hasErrors()) {
			render view:'/some/Path/to/Display/ReportSelection',model:[bean:bean]
			return
		}
		// If you wanted to pass in controller and action to buildReport you can use ReportsQueueBean

		ReportsQueueBean inputBean = new  ReportsQueueBean()
		inputBean.fromController=controllerName
		inputBean.fromAction=actionName
		inputBean.reportName= 'tsvExample1'
		inputBean.locale=RequestContextUtils.getLocale(request)
		inputBean.userId=  queuekitUserService.currentuser
		inputBean.paramsMap=bean.loadValues()
		inputBean.priority=Priority.HIGHEST
		inputBean.reportType=bean.reportType


		def queue = queueReportService.buildReport(inputBean)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [inputBean.reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}

	/*
	 * Typically when building reports the controller handles this
	 * so this is to show what index would be as a standard grails app
	 * where as now index takes params
	 * passes it through the steps to process and push out this exact same report/output
	 * but through a service
	 */

	def downloadByBrowser(params) {
		

		/*
		 * In the method above the below content has been copied directly into 
		 * corresponding service. 
		 * The only difference is that the corresponding service generates out 
		 * dynamically and so this part should not be copied
		 */
		response.setHeader 'Content-type','text/plain; charset=utf-8'
		response.setHeader "Content-disposition", "attachment; filename=index.tsv"
		def out = response.outputStream

		/*
		 * From here on to end of out.close gets copied to
		 * corresponding service
		 */

		def queryResults=tsvService.runParams(params)
		out << 'name\t'
		out << "${params.report?:'testing'}"
		out << '\rtext\t'
		out << "${params.sample?:'testing text'}"
		out << '\r'
		queryResults?.each{field->
			out << field.id << '\t'
			out << field.text << '\t'
			out << '\r'
		}
		out.flush()
		out.close()
	}

	def testNoResults(Report2Bean bean) {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'tsvNoResults'
		//No queue defined - by default Priority
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(), Priority.LOWEST,bean.reportType)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}

	/*
	 * This is xlsExample1 which is configured as Priority.SLOW
	 * Execute about 6 of these in a row then 6 of index1 and watch queue
	 * sort by startDate
	 * 
	 */
	def index2(Report2Bean bean) {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'xlsExample1'
		//No queue defined - by default Priority
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(), Priority.LOWEST,bean.reportType)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}


	/*
	 * This demonstrates LINKEBLOCKING queue
	 * TSV file
	 */
	def index3(Report1Bean bean) {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'tsvExample1'
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),ReportsQueue.LINKEDBLOCKING)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}

	/*
	 * This demonstrates LINKEDBLOCKING queue 
	 * XLS
	 * 
	 * If you launch 6 of these and 6 of index3 
	 * LinkedBlocking will not prioritise the reports so 
	 * the 6 of these will run then 6 of index3
	 * 
	 * where as in index2 -> index case  
	 * 	-> 3 of index2   --> 6 of index 
	 *  then --> last 3 of index2 
	 *  would be executed prioritising queue 
	 */
	def index4(Report1Bean bean) {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'xlsExample1'
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),ReportsQueue.LINKEDBLOCKING)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}

	/*
	 * Demonstrates ArrayBlocking
	 * Queueing mechanism is self controlled through DB lookups
	 * ArrayBlocking does not manage queue for you
	 * tsv file example 
	 */
	def index5(Report1Bean bean) {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'tsvExample1'
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),ReportsQueue.ARRAYBLOCKING)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}

	/*
	 * Demonstrates ArrayBlocking
	 * Queueing mechanism is self controlled through DB lookups
	 * ArrayBlocking does not manage queue for you
	 * xls file example
	 */
	def index6(Report1Bean bean) {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'xlsExample1'
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),ReportsQueue.ARRAYBLOCKING)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}

	/*
	 * Demonstrates Priority Queue with custom defined Priority
	 * Priority level must exist in Priority class
	 * Whether report has not been configured in Config.groovy
	 * or you wish to override it in special cases you can call the override
	 * method of buildReport as below:
	 * 
	 */
	def index7(Report1Bean bean) {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'xlsExample1'
		// Using Override method of BuildReport whereby Priority is directly provided by the call method
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),Priority.REALLYSLOW, bean.reportType)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}

	/*
	 * This demonstrates file name override
	 * set the report to be test1 or test2
	 * or anything else and file produced will have that fileName set
	 */
	def index8(Report3Bean bean) {
		render view:'/reportDemo/test', model:[bean:bean]
	}
	/*
	 * finishing action for index8
	 */
	def  customName(Report3Bean bean) {
		if (!bean.validate()) {
			log.debug "Errors: ${bean.errors}"
		}
		def locale = RequestContextUtils.getLocale(request)
		def userId = bean.userId ?: queuekitUserService.currentuser
		String reportName = 'csvExample'
		/*
		 *  Using Override method of BuildReport whereby Priority is directly provided by the call method
		 *  But this comes with a twist if the user selects a 1 day date range and low priority
		 *  it will default it high - 
		 *  
		 *  check queueUserService to find out more on how you can configure a more customised approach
		 *  for your reports.
		 */
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),Priority.REALLYSLOW)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:'/reportDemo/test', model:[bean:bean]
	}

	/**
	 * slow HQL test using priorityBlocking
	 * @param bean
	 * @return
	 */
	def index9(Report2Bean bean) {
		def locale = RequestContextUtils.getLocale(request)
		def userId = queuekitUserService.currentuser
		String reportName = 'tsvExample2'
		//No queue defined - by default Priority
		def queue = queueReportService.buildReport(reportName,userId , locale, bean.loadValues(),Priority.REALLYSLOW, ReportsQueue.ENHANCEDPRIORITYBLOCKING)
		flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
		render view:VIEW, model:[bean:bean]
	}

	ConfigObject getConfig() {
		return grailsApplication.config?.queuekit ?: ''
	}
}
