<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'example.label', default: 'example')}" />
		<title><g:message code="default.admin.menu.label" args="[entityName]" default="Welcome to ${entityName}" /></title>
		<style type="text/css">
		html,body {
			max-width: 100% !important;
		}
		.codebox {
    		border:1px solid black;
    		background-color:#090909;
    		width: 60em;
    		overflow:auto;    
    		padding:20px;
    		margin-left:10px;
    		
		}
		.codebox code {
			font-family: Arial, Helvetica, sans-serif;
    		font-size:1em;
    		white-space: pre;
    		background-color:transparent;
    		color:#FFF;
    
		}
		.alert {
			min-width: 20em;
			max-width: 61em;
			color:#000;
			font-weight:bold;
		}
		</style>
	</head>
	<body>
	<g:render template="nav"/>
		
		<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
		</g:if>
		<div class="container">
		<div class="content">
		<div class="alert alert-success">
		Welcome to queuekit plugin.<br>
		
		I will try to explain how to use this plugin through a basic example below, assuming you have:<br> 
		</div>
<div class="codebox">
<code>response.setHeader 'Content-type','text/plain; charset=utf-8'
response.setHeader "Content-disposition", "attachment; filename=index.tsv"
def out = response.outputStream
def queryResults=tsvService.runParams(params)
	out << 'name\t'
	out << "\${params.report?:'testing'}"
	out << '\rtext\t'
	out << "\${params.sample?:'testing text'}"
	out << '\r'
	queryResults?.each{field->
		out << field.id << '\t'
		out << field.text << '\t'
		out << '\r'
	}
out.flush()
out.close()
</code>
</div>
	<div class="alert alert-success">
If you change to be like this:
</div>
<div class="codebox">
<code>def controllerCall() {
  response.setHeader 'Content-type','text/plain; charset=utf-8'
  response.setHeader "Content-disposition", "attachment; filename=index.tsv"
  def out = response.outputStream
  def queryResults=tsvService.runParams(params)
  actionReport(out,queryResults,params)
}
private actionReport(out,queryResults,params) {
  out << 'name\t'
  out << params.report
  out << '\rtext\t'
  out << params.sample
  out << '\r'
  queryResults?.each{field->
	out << field.id << '\t'
	out << field.text << '\t'
	out << '\r'
  }
  out.flush()
  out.close()
}
</code>
</div>
	<div class="alert alert-success">
Then you are half way there, in principal the same thing would be put in to your service that would be in the action report. The plugin handles out so there will be no need to define 
response or out variables.
 <br/>
So all of above would become:
</div>
<div class="codebox">
<code>def controllerCall() {
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
}
</code>
</div>
	<div class="alert alert-success">
Then we create
</div>
<div class="codebox">
<code>package org.grails.plugin.queuekit.examples.reports

import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService


class ParamsExampleReportingService extends QueuekitBaseReportsService {

	def tsvService

	
    /*
	 * Must be declared gives you params 
	 * You must run your service to get back the results
	 * Push results params and queue into runReport as show
	 */
	def runReport(ReportsQueue queue,Map params) {
		def queryResults = tsvService.runParams(params)
		runReport(queue,queryResults,params)
	}

    /*
     * You must define this as shown. Plugin will provide you at this point
     * with out. Push out queryResults and bean = your original params back into 
     * your own custom method which like shown above iterates through your list
     * and pushes into out
     */
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
			//This will also work like in your controller
			//out << "&#36;{g.message(code:'some.code')}"
			out << '\r'
		}
		out.flush()
		out.close()
	}
}</code>
</div>
	<div class="alert alert-success">
That now queues the report requests when someone clicks controllerCall and the report can be seen <g:link controller="queueKit" action="listQueue">Here</g:link>



<br/><br/>

You can also use this technology for any other type of files you were generating on the fly in a controller so for example apache-poi
</div>
<div class="codebox">
<code>String filename = 'Report3Example.xls'
	HSSFWorkbook wb = new HSSFWorkbook()
	HSSFSheet sheet = wb.createSheet()
	....
	try {
		
		// When copying your method over to your new Service
		// as already mentioned out is already provided by plugin 
		// the below 4 lines should not be provided in the new service call
		// everything else is identical
		response.setContentType("application/ms-excel")
		response.setHeader("Expires:", "0") // eliminates browser caching
		response.setHeader("Content-Disposition", "attachment; filename=$filename")
		OutputStream out = response.outputStream
		// End of no longer required - when converted to plugin service method 
		wb.write(out)
		out.close()
	} catch (Exception e) {
	}</code>
</div>
<div class="alert alert-success">
Would be changed to like per above:
</div>
<div class="codebox">
<code>def controllerCall() {
	def locale = RequestContextUtils.getLocale(request)
	def userId = queuekitUserService.currentuser
	String reportName = 'xlsExample'

	/*
	 * these are your own params really
	 */
	params.report='Params examples'
	params.sample='Some sample text'
		
	//No queue defined - by default Priority
	def queue = queueReportService.buildReport(reportName,userId , locale, params)
	flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
}
</code>
</div>
<div class="alert alert-success">
Then we create
</div>
<div class="codebox">
<code>package org.grails.plugin.queuekit.examples.reports

import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService


class XlsExampleReportingService extends QueuekitBaseReportsService {


	
	def tsvService
	
	/*
	 * We must define the report type file extension
	 * default is tsv this being XLS needs to be defined
	 * 
	 */
	String getReportExension() {
		return 'xls'
	}
	
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
			log.debug "priority is now &#36;{priority} was previously &#36;{priority} difference of date : &#36;{difference}"
		}		
		return priority
	}
	
	/*
	 * 
	 * Overriding how QueuekitBaseReportsService names it here
	 * Take a look at CsvExampleReportingService where a more 
	 * complex example is provided that defines filename based on 
	 * a value within bean - the report was used
	 * for multiple different reports - each doing something slightly 
	 * different but using same input bean ..
	 */
	String getReportName(ReportsQueue queue,bean) {
		return "MyLovelyReport-&#36;{queue.id}.&#36;{reportExension}"
	}


    
	def runReport(ReportsQueue queue,Map params) {
		def queryResults = tsvService.runParams(params)
		runReport(queue,queryResults,params)
	}

	def actionInternal(out,bean, queryResults,Locale locale) {
		actionReport1Report(out,bean,queryResults)
	}

	private void actionReport1Report(out,bean,queryResults) {
		HSSFWorkbook wb = new HSSFWorkbook()
		HSSFSheet sheet = wb.createSheet()
		//Do your stuff you are doing with out
		HSSFRow row=sheet.createRow(counter)
		Cell cell1 = row.createCell(i)
			cell1.setCellValue("")
			cell1.setCellStyle(headingStyle)
		...
		// finally the above block you had above becomes much simpler
		// like this:
		// out is then taken care of by plugin
		try {
			wb.write(out)
			out.close()
		} catch (Exception e) {
		}
			
		
	}
}</code>
</div>
</div>
</div>
		</body>
		</html>
			