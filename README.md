queuekit
=========

#### Queuekit is a plugin for grails which uses TaskExecutor with Spring Events for grails 2 and for grails 3 using
default `Reactor` events to manage concurrent submitted reports.You can use it for your own custom reporting needs or
simply combine it with grails export plugin to queue export requests.

##### At work we face a scenario where reports and core application are integrated and even if separated they would
still hit the same database.
##### The reports are typically rendered through response as a file stream and saved to user's desktop.
##### At some periods of the day the application runs slow, we think due to people running reports concurrently.
Without going into further complexity of database, application and reporting system. A cure would be to limit the amount
 of concurrent reports that can run and sort reports based on a priority system allowing (HIGHER: Speedier ones through
 over LOWER: Calculations would suggest it will take long per report basis)
 
##### Queuekit plugin incorporates TaskExecutor ArrayBlocking / LinkBlocking and PriorityBlocking. 
#### It also enhances on PriorityBlocking with a new custom method called EnhancedPriorityBlocking. 
##### Define queue limit which in turn limits concurrent usage of all users. 
##### Typical request on demand report system will change to background queued reports system. Choose the
 best blocking method for your queues.
##### Both Priority and EnhancedPriority allow queue items to have a default or on the fly priority.
 
##### EnhancedPriorityBlocking is more costly and launches addition threads per job but all in aid of actually being
able to kill a live running IO task.
##### When master task or live running task is cancelled. The underlying thread is cancelled. This gives you the
feature to cancel live background threaded tasks.

##### If you run reports in such a manner, read on since with a minor tweak and the help of this plugin you can easily
convert those reports to background queued tasks that you then define limitation over.

## 1. Installation:

### Grails 3: 
```groovy
compile "org.grails.plugins:queuekit:1.11"
```
 
##### [source](https://github.com/vahidhedayati/grails-queuekit-plugin/) |
 [demo](https://github.com/vahidhedayati/test-queuekit3/)

### Grails 2: 
```groovy
compile ":queuekit:1.5"
```

##### [source](https://github.com/vahidhedayati/grails-queuekit-plugin/tree/grails2) |
 [demo](https://github.com/vahidhedayati/test-queuekit)

 
## 2.[Configuration](https://github.com/vahidhedayati/grails-queuekit-plugin/tree/master/grails-app/conf/SampleConfig.groovy)
The configuration provided would be added to Config.groovy on grails 2 and application.groovy in grails 3.


####Videos
##### [Video 1:- grails queuekit plugin part 1 : Why you would use this plugin?](https://www.youtube.com/watch?v=hVC8IOagAwo)
##### [Video 1:- grails queuekit plugin part 1.5 : Combine grails export plugin with queuekit so export requests are queued](https://www.youtube.com/watch?v=QntcB_k3JfI)
##### [Video 2:- grails queuekit plugin part 2 : Configuration walkthrough](https://www.youtube.com/watch?v=JgoSSOG_iRI)
##### [Video 3:- grails queuekit plugin part 3 : Demonstrating cancellation of live threadExecutor Threads](https://www.youtube.com/watch?v=QDZ5-A-4_8o)
##### [Video 4:- grails queuekit plugin part 4 : Binding plugin with your grails application security](https://www.youtube.com/watch?v=6MOzoPp7o1g)
##### [Video 5:- grails queuekit plugin  part 5: Preserving a running queue slot for defined priority or above](https://www.youtube.com/watch?v=ACKo7o6DkkA)
#### [Video Boxset (all of above)](https://www.youtube.com/watch?v=hVC8IOagAwo&list=PLfZr1vB6p8XIHP-d8ta8fyYVCgRfr4Y7h)

## 3.Information
=====

### Provides a variety of methods managing your queue:

> 1. ArrayBlockingQueue  - This is the most basic and provided are database functionality to manage the queue for you
ArrayBlockingQueue has no mechanism to manage incoming threads, it will take on as much as available and beyond that
 reject them. Additional steps have been added to capture / re-queue those that exhaust the queue and to manually check
  the queue upon completion of last task.

> 2. LinkedBlockingQueue - This manages the queue automatically 
LinkedBlockingQueue is the next phase up, Since it manages the queue for you. If you have 3 maximum threads and fire 5.
 2 will wait until the first 2 are done and then their picked up. Queue is processed and limited to items as they arrive.

> 3. PriorityBlockingQueue - This manages the queue automatically and also attempts to run concurrent requests with a priority.
PriorityBlockingQueue by default provides a mechanism to manage queue items according to each items priorities.

> 4. EnhancedPriorityBlockingQueue - This manages the queue automatically and also attempts to run concurrent requests
with a priority. PriorityBlockingQueue by default provides a mechanism to manage queue items according to each items priorities.
It also binds a new thread task to an inner thread. This means you can also cancel a live running thread.
When a cancel request is issued. The main running thread also kills off the inner thread that is the running task.
This required further concurrentHashMaps to track / remove elements.

With both PriorityBlocking and EnhancedPriorityBlocking which was really my own additional work around PriorityBlocking,


The priority itself is defined by you in your Config.groovy depending on the name given to it.
There is also an additional hook that you can add to all/some of your reports that will go off and look at custom
parameters provided by the report and decide if it should lower/higher the default config priority.
An example for this is also provided. The policy as to how it decides is entirely based on what works for you.
You could use the example to expand on your business logic.

##### Must outline the topics covered in point 4 EnhancedPriority are all very experimental,
it's the end ambition/desire to achieve all that without any side effects. I think you may find odd behaviour.
 Your input / enhancements are most welcome.
 
You can configure the following:
```groovy
reportThreads=3
preserveThreads = 1
preservePriority = Priority.MEDIUM
```
	
##### 	If you have 3 threads and 6 LOW Priority reports launched  
####    If after (reportThreads - preserveThreads) = 3 - 1 = 2  (So only 2 would run at most at any time of LOW Priority)
This means all those below Priority.MEDIUM will have a spare slot to run within. In short an fast lane left open always
 for those below medium and 2 slow lanes. You should be able to configure 6 report
	  
	
## With admin rights you can:

#### >  Change a priority of a queued report from the main listing screen.  
But beyond that when a report is queued you can click on report item and choose change priority.  If it was LOW and set
 by Config or override hook you now as the human can set it to be a higher priority which will take effect when next
 thread becomes available.
 
 
#### >  Increase / Decrease (override Config values of) reportThreads / preserveThreads and preservePriority.
This means you can on the fly increase or decrease and change preservePriority group from the main report screen.
 The changes are only for runtime. Meaning upon restart the defaults from Config.groovy or if not defined what
 plugin thinks is best is used.

  
#### >  Shutdown ThreadExecutor
No idea why you want to do this unless you are testing in worst case scenario with intentions of testing
`useEmergencyExecutor=false` or `manualDownloadEnabled=true`

#### > Control maximum running time before killing a running task and canelling it.
configure `killLongRunningTasks=300` where 300 = 5 minutes in seconds. Provide the time in seconds you wish to wait
before the taskExecutor sechedule is killed off. This only works for EnhancedPriorityBlockingExecutor and when it
 attempts to pickup the next thread, it checks running task of existing threads.
If any match your set limit and beyond they are killed and set to status Cancelled
	

## With Configuration you can:

#### >  Configure report time highlight
In this example if report takes:
 
 > over 01:10:02  (1 hour 10 minutes and 2 seconds) it will highlight in blue
 > over 00:01:05  (1 minute and 5 seconds) it will highlight in orange
 > over 00:00:12  (12 seconds) it will highlight in html colour code: #FFFFAA
 
```groovy
   durationThreshHold = [
		[hours: 1, minutes: 10, seconds: 2, color: 'blue'],
		[minutes: 1, seconds: 5, color: 'orange'],
		[seconds: 12, color: '#FFFFAA']
	]
```

#### >  Configure a backup Executor for when / if main Executor is down

When tasks go into rejection it is typically due to Executor having issues. This is all true for all Executor
types provides besides ArrayBlocking.
So this feature is available for all besides ArrayBlocking. Since ArrayBlocking has no queue mechanism and is managed
by plugin DB lookups. It automatically puts a new task in rejection if over limit running. Therefore we capture those
 and re-queue them in this case. For all other cases if you enable
`useEmergencyExecutor=true`  This will tell the plugin to fall over to a single executor and launch a schedule of the
 user request. This is all transparent to the end user and happens in the back-end. It will throw errors in the log.
 All the rules of priorities goes out of the window and there are no limitations as many requests made is as many
 threads launched. It keeps business flowing whilst a fix / resolution is found I guess.


If main Executor is shutdown - due to how it is wired - (behaving like a service) it needs an application restart
for it to reset. Ideas welcome, some comments in EnhancedPriorityBlockingExecutor in regards to this.



#### >  Configure a backup of a backup or disable backup Executor and fall back to manualDownload
Like above if tasks go into rejection, if you have configured backup executor and even that appears to be shutdown -
 which would be strange since the backup is a single executor launched per request. Either way, you can also set this
 to true `manualDownloadEnabled=true` in your Config.groovy.

This again is transparent to the end user, but if all of above has failed it will actually launch a new single
runnable task to execute the task, the report is treated like all of above, it is captured in report listing, shows
that it is running and completed and is also timed. But has totally bypassed all of the threadExecutors and just
launched as a thread within application.

This also behaves like all of above meaning the end user will get a prompt report has been triggered and if trained to
 do all ove above they would go to the normal listing screen and wait for it to complete.

If you wanted to make this fail over behave like a real download, you could refer to `ReportDemoController` `index`
action which has the following:
(It makes the user wait on the screen whilst the manual thread goes through the process of being created. When it has a
 file, it redirects to download page like they would have if they had clicked on a live report download action.





## 4. Additional simplification / explanation 

This plugin is from a concept I put together for work and will convert the process of user report interaction from one of :

Click on a report - or define report criteria and click download 

If the process is to then go off and get data produce a CSV,XLS,TSV,DOC,PDF of some form and you are using your controller request mechanism
to deliver file through a stream.

If above describes your scenario then as you are probably well aware, as database/user-base grows and more reports are
requested. Specially concurrent requests by many users can have an impact on your application performance.

This plugin will be able to change that process and limit to concurrent threads as well as provide a queueing / user
 report interface. Since the jobs are converted to background tasks the files are produced when system has completed
 and user has to check another interface rather than on demand file generated on the fly as they clicked save/download.

The process to convert your existing reports should be really kept to a minimal so long as Controller was used to
 generate file from some result set that came back from some service. The only minor change is where you
  defined `request type` and `out` and `filename`. These segments can be stripped out and rest will be near
  enough what you had :


## Examples



#### Binding queuekit with grails export plugin

Most importantly pay attention to bufferedWriterTypes configuration ensure you have removed CSV and csv from it. If you
are going to use this plugin for working with export plugin. The export plugin uses a different way to export csv compared
 to how you would normally through a controller as described further down under manual reports.

You can use this plugin in conjunction with the export plugin to essentially change the mechanism from files produced
 as requested to user requests for export plugin being quueed through queuekit.

Please feel free to browse through the grails 3 demo site which has all of this in place, I will show  the more
advanced version since it is probably most feasible.

You have installed/been using export plugin, you have configured the required addition in the controller to send
 report to exportService.

I installed the plugin, created a domain class generated controllers and views and then amended the call to
exportService to  :
`queueReportService.buildReport(reportName,userId , locale, params)`

Change that to this

```groovy

    //def exportService
    def myUserService
    def queueReportService

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        String format=params.f  ?: params.extensions ?: 'html'
        if(format && format != "html"){
            def locale = RequestContextUtils.getLocale(request)
            def userId = myUserService.currentuser
            String reportName = 'exportPluginAdvanced'
            /**
             * ! -- IMPORTANT
             * In order to let this dynamic exportPluginAdvancedReportingService pickup the correct domainClass
             * We must send an additional params as part of reports calls and bind in the actual domainClass we would be listing
             * just like shown here
             *
             */
            params.domainClass=TestAddress.class

            log.debug "Sending task as default priority to queueReportService instead of exportService.export"
            //def queue = queueReportService.buildReport(reportName,userId , locale, params,Priority.HIGH,ReportsQueue.PRIORITYBLOCKING)
            def queue = queueReportService.buildReport(reportName,userId , locale, params)

            flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])

            /**
             * How you would normally export using Export plugin
             * Changed to above to go through queuekit plugin and queue request instead
             *
             * Take a look at ExportPluginAdvancedReportingService to see how you can do the same
             *
             */
            // response.contentType = grailsApplication.config.grails.mime.types[format]
            // response.setHeader("Content-disposition", "attachment; filename=books.${params.extension}")
            // exportService.export(format, response.outputStream,TestExport.list(params), [:], [:])
        }
        respond TestAddress.list(params), model:[testAddressCount: TestAddress.count()]
    }
 ```

Now with this in place I will need to create a new service called `ExportPluginAdvancedReportingService`:

```groovy
package test

import grails.util.Holders
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.reports.QueuekitBaseReportsService
import testing.TestAddress
import testing.TestAttributes

class ExportPluginAdvancedReportingService extends QueuekitBaseReportsService {

	def exportService

	def runReport(ReportsQueue queue,Map params) {
		runReport(queue,[something:'aa'],params)
	}
	// This doesn't matter so much so long as it meets the Type that is not of
	// config value of config.bufferedWriterTypes
	// Since it needs to call the other method in QueuekitBaseReportsService
	// Actual fileName extension is overriden right at the very bottom of this
	// class in getReportName by bean.extension (this ensures file is correctly labelled
	String getReportExension() {
		return 'xls'
	}

	def actionInternal(out,bean, queryResults,Locale locale) {
		actionReport1Report(out,bean,queryResults)
	}

	/**
	 *
	 *
	 * @param out -> Where out is provided by plugin
	 * @param bean ->Where bean is your actual user params from the front end screen
	 * @param queryResults -> QueryResults would be what would be produced by your code
	 * 				In the case of this we are setting it to [something:'aa']
	 * 				above. This then will continue working and hit this block
	 * 				which will carry out real export service task at hand.
     */
	private void actionReport1Report(out,bean,queryResults) {
		String format=bean.f ?: 'html'
		if(format && format != "html"){
			log.debug "Params received  ${bean.f} ${bean.extension} "
			def domain= bean.domainClass

			if (domain) {
				println "got Domain ${domain}"
				//	def domainClass = Holders.grailsApplication?.domainClasses?.find { it.clazz.simpleName == uc(domain) }?.clazz
				def domainClass = Holders.grailsApplication.getDomainClass(domain)?.clazz
				if (domainClass) {
					println "we have a real domainClass ${domainClass}"
					domainClass.withTransaction {
						Map formatters=[:]
						Map parameters=[:]
						switch (domain) {
							case 'TestAddress':
								println "custom testAddress stuff here"
								//formatters=[:]
								//parameters=[:]
								//bean.something=SomethingElse
								break
							case 'TestAttribues':
								println "custom testAttributes stuff here"
								//What would you like to do
								//formatters=[:]
								//parameters=[:]
								break
						}
						//Export service is being called here
						exportService.export(format, (OutputStream) out, domainClass.list(bean),formatters,parameters)
					}
				}
			}
		}
	}

	/*
	 *
	 * Overriding how QueuekitBaseReportsService names it here
	 */
	String getReportName(ReportsQueue queue,bean) {
		return "ExportPlugin-${queue.id}.${bean.extension?:reportExension}"
	}

}

```

That's it, the user reports will now be queued through the queuekit plugin,
You can see export feature is called in the `ExportPluginAdvancedReportingService`.
The code in the controller can be copied from controller to controller.
Just pay attention to: (Ensure you are passing in correct domainClass that you will use in the sharedExport service.
```groovy
 /**
             * ! -- IMPORTANT
             * In order to let this dynamic exportPluginAdvancedReportingService pickup the correct domainClass
             * We must send an additional params as part of reports calls and bind in the actual
             * domainClass we would be listing
             * just like shown here
             *
             */
            params.domainClass=TestAddress.class
```

Manual Reports using plugin with your own methods of producing reports
=====

#### Apache-poi XLS files - manual report
Check out [grails queuekit demo site for grails 3](https://github.com/vahidhedayati/test-queuekit3/). Follow the example to see how I got it to work
All very similar to instructions below besides that it is using custom libraries to produce the output. (different file types to standard csv/tsv described below)



#### Examples demonstrated on
[org.grails.plugin.queuekit.examples.ReportDemoController](https://github.com/vahidhedayati/grails-queuekit-plugin/tree/master/grails-app/controllers/org/grails/plugin/queuekit/examples/ReportDemoController.groovy)

``

Assuming you have:

```groovy
response.setHeader 'Content-type','text/plain; charset=utf-8'
response.setHeader "Content-disposition", "attachment; filename=index.tsv"
def out = response.outputStream
def queryResults=tsvService.runParams(params)
	out << 'name\t'
	out << "testing"
	out << '\rtext\t'
	out << "testing text"
	out << '\r'
	queryResults?.each{field->
		out << field.id << '\t'
		out << field.text << '\t'
		out << '\r'
	}
out.flush()
out.close()
```

If you change to be like this:
```groovy
def controllerCall() {
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
```

Then you are half way there, in principal the same thing would be put in to your service that would be in the action
report. The plugin handles out so there will be no need to define response or out variables. So all of above would become:
```groovy
def controllerCall() {
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
```

Then we create a service called [ParamsExampleReportingService](https://github.com/vahidhedayati/grails-queuekit-plugin/tree/master/grails-app/services/org/grails/plugin/queuekit/examples/reports/ParamsExampleReportingService.groovy)

```groovy
package org.grails.plugin.queuekit.examples.reports

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
			//out << "${g.message(code:'some.code')}"
			out << '\r'
		}
		out.flush()
		out.close()
	}
}
```

That now queues the report requests when someone clicks controllerCall and the report can be seen Here 

You can also use this technology for any other type of files you were generating on the fly in a controller so for example apache-poi

```groovy
String filename = 'Report3Example.xls'
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
	}
```	
Would be changed to like per above:
```groovy
def controllerCall() {
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
	//You can provide further options look up ReportDemoController to see more examples
	//def queue = queueReportService.buildReport(reportName,userId , locale, params,Priority.HIGH,ReportsQueue.PRIORITYBLOCKING)
	flash.message = g.message(code: 'queuekit.reportQueued.label', args: [reportName, queue?.id])
}
```

Then we create `XlsExampleReportingService`
```groovy
package org.grails.plugin.queuekit.examples.reports

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
    			log.debug "priority is now ${priority} was previously ${priority} difference of date : ${difference}"
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
		return "MyLovelyReport-${queue.id}.${reportExension}"
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
}
```

### Beans that bind to other objects

Take a look at
[org.grails.plugin.queuekit.examples.ComplexBindedBean](https://github.com/vahidhedayati/grails-queuekit-plugin/tree/master/src/main/groovy/org/grails/plugin/queuekit/examples/ComplexBindedBean.groovy) and read through it to understand how to bypass it



### Other useful information

> ### queuekitUserService `def userId = queuekitUserService.currentuser`

This is a userService that exists within this plugin, you should override this as per example site and feed in your
 real user/userId/userLocale/permission values in from your own site.

> ### reportName `String reportName = 'tsvExample1'`

This is really as important as it gets, ensure you use proper class naming convention so no +_&*^!£$%^ characters no
 space etc just normal alphabet as if you were naming a domain class.
Create a new service called
> ##### ${name}ReportingService [TsvExample1ReportingService](https://github.com/vahidhedayati/grails-queuekit-plugin/tree/master/grails-app/services/org/grails/plugin/queuekit/examples/reports/TsvExample1ReportingService.groovy)

This service must [extend QueuekitBaseReportsService](https://github.com/vahidhedayati/grails-queuekit-plugin/tree/master/grails-app/services/org/grails/plugin/queuekit/reports/QueuekitBaseReportsService.groovy).


Binding application security with the plugin
=====
Under the [grails 3 demo site](https://github.com/vahidhedayati/test-queuekit3/), spring security got installed a new
service called MyUserService which extends QueuekitUserService and overrides the default actions of the plugin to return
userId if user is a super user and so forth.

The service then takes over QueuekitUserService the test site's grails-app/init/test.queuekit3/Application.groovy
```groovy
class Application extends GrailsAutoConfiguration {
    Closure doWithSpring() {
        { ->
            queuekitUserService(test.MyUserService)
        }
    }
    ///....
 ```
 
Quartz scheduling clean up
=====
If you are running quartz, create a task probably running daily that calls

```groovy
def queueReportService
...

queueReportService.deleteReportFiles()
```


Bootstrap task to re-schedule old queued task pre-application shutdown
===== 
In your Bootstrap.groovy declare

```grooy
//Inject the service
def queuekitExecutorBaseService

//Run this 
queuekitExecutorBaseService.rescheduleRequeue()

//Also ensure you have enabled in your Config.groovy/application.groovy
queuekit.checkQueueOnStart=true
```
