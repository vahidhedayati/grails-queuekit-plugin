package org.grails.plugin.queuekit.reports

import grails.converters.JSON
import grails.util.Holders

import java.text.SimpleDateFormat
import java.util.concurrent.RunnableFuture
import java.util.concurrent.ScheduledThreadPoolExecutor

import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.codehaus.groovy.grails.web.context.ServletContextHolder
import org.grails.plugin.queuekit.ArrayBlockingReportsQueue
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.AttachedRunnable
import org.grails.plugin.queuekit.priority.EnhancedPriorityBlockingExecutor
import org.grails.plugin.queuekit.priority.Priority
import org.hibernate.LazyInitializationException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.support.WebApplicationContextUtils
/**
 * 
 * Abstract QueuekitBaseReportsService class is the core component
 * of the reporting process of this plugin.
 * All generated reportName must have a matching reportNameReportingService 
 * class which extends this QueuekitBaseReportsService - follow existing examples
 * 
 *  
 * This class actions the core global functionalities of the extended classes
 * it provides g. to all extended services
 * defines:
 *  	out -> depending on reportType
 *  	fileName -> default value set here - can be overridden by extended classes
 *      actual executeReport functionality
 *      reportName -> generated according to input - can be overridden by extended classes
 *      reportExension -> default set as 'tsv' - can be overridden by extended classes
 *      actionReport -> final report segment which defines values of (for DB):
 *      	fileName
 *          reportName
 *          displayName
 * 
 * 	Your extended class will bounce back and forth with these generic actions
 * 
 * @author Vahid Hedayati
 *
 */
abstract class QueuekitBaseReportsService  {

	/*
	 * define g tag which loads up ApplicationTagLib
	 * this now allows your services to emulate g.message tags 
	 * that you would do typically to generate reports 
	 */

	def grailsApplication = Holders.grailsApplication
	def g = grailsApplication.mainContext.getBean(ApplicationTagLib)
	def queuekitUserService= grailsApplication.mainContext.getBean('queuekitUserService')



	/**
	 * Abstract method that must exist in all report services that extend this ReportsService class
	 * It is the initiating point of this service that is executed on the defined service that matches report
	 * @param queue
	 * @param params
	 * @return
	 */
	abstract def runReport(ReportsQueue queue,Map params)

/**
	 * getQueuePriority is by default the queue's configuration priority
	 *
	 *  Take a look at XlsExample where a more specific lookup is done
	 * @param queue
	 * @param params
	 * @return
	 */
	Priority getQueuePriority(ReportsQueue queue, Map params) {
		Priority priority = queue?.priority ?: queue.defaultPriority
		return priority
	}

	/**
	 * Take a look at XlsExample1ReportingService
	 * It uses it to parse the Date fields given by 
	 * end user. It will most likely be in String format 
	 * and call the one below
	 * @param t
	 * @return
	 */
	Date parseDate(Date t,String format=null) {
		return t
	}

	Date parseDate(String t,String format=null) {
		SimpleDateFormat sf
		if (!format) {
			/*
			 * Fix for No thread-bound request found: Are you referring to request attributes
			 */
			def webRequest = RequestContextHolder.getRequestAttributes()
			if(!webRequest) {
				def servletContext  = ServletContextHolder.getServletContext()
				def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
				webRequest = grails.util.GrailsWebUtil.bindMockWebRequest(applicationContext)
			}
			format =g.message(code:'queuekit.defaultDate.format', default: 'dd MMM yyyy')
		}
		sf = new SimpleDateFormat(format)
		sf.setLenient(false)
		if (sf)	return sf.parse(t)
	}
	

	/**
	 * Abstract method must exist in all classes that extend this class
	 * At this point runReport method in your service has produced 
	 * queryResults  
	 * @param out
	 * @param bean
	 * @param queryResults
	 * @param locale
	 * @return
	 */
	abstract def actionInternal(ReportsQueue queue,out,bean, queryResults,Locale locale)

	/**
	 * Override method called by your extended Service runReport method
	 * You must call this method in your run report to carry out
	 * By the time this is called you must have generated queryResults 
	 * and have a report binding bean to pass to it 
	 * generic report actions - 
	 * it generates out depending on report Type
	 * calls abstract actionInternal
	 * @param queue
	 * @param queryResults
	 * @param bean
	 * @return
	 */
	def runReport(ReportsQueue queue,queryResults,bean) {
		if (queryResults) {
			String filename=getFileName(queue,bean)
			def out
			def bufferedWriterTypes = config.bufferedWriterTypes ?: ['TSV','CSV','tsv','csv']

			if (bufferedWriterTypes.contains(reportExension)) {
				out = getOut(filename)
			} else {
				/*
				 *  This will be xls,docx,doc,doc,pdf and so on
				 *  by default it will fall here if it does not match above bufferedWriterTypes
				 */
				out =new FileOutputStream(new File(filename))
			}

			/* abstract method above */
			actionInternal(queue,out,bean, queryResults,queue.locale)

			/*
			 * global method at the very bottom of this class
			 * finalises by setting reportName or displayName 
			 * and fileName fileName is generated by this 
			 * global call of runReport at the very top of this call
			 */
			actionReport(queue,bean,filename)
		} else {
			errorReport(queue)
		}
	}

	def executeReport(ReportsQueue queue) {
		boolean validStatus=verifyStatusBeforeStart(queue.id)

		if (validStatus && !threadInterrupted) {
			/*
			 * Fix for No thread-bound request found: Are you referring to request attributes
			 */
			def webRequest = RequestContextHolder.getRequestAttributes()
			if(!webRequest) {
				def servletContext  = ServletContextHolder.getServletContext()
				def applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext)
				webRequest = grails.util.GrailsWebUtil.bindMockWebRequest(applicationContext)
			}
			boolean hasException=false
			try {
				/*
				 *  Override current request Locale with user's locale.
				 *  g.message will now pick up correct locale
				 *  
				 */
				def request = RequestContextHolder.currentRequestAttributes().request
				Locale locale= queue.locale ?:  queuekitUserService.getUserLocale(queue.userId) ?: Locale.UK.toString()
				request.addPreferredLocale(locale)

				/*
				 * Convert params from JSON back to map
				 */
				def paramsMap=JSON.parse(queue.paramsMap)

				/*
				 * Due to complications:
				 * 
				 * 	If queue is of EnhancedPriorityBlockingReportsQueue type
				 * 	then Add it as a HookTask and run it as another thread 
				 * 	within this thread
				 * 
				 * Other report types carry on as per normal 
				 * 
				 */				
				if (queue.isEnhancedPriority()) {

					def manualDownloadEnabled = (config.manualDownloadEnabled &&  config.manualDownloadEnabled == true)

					if (manualDownloadEnabled && isManual(queue.id)) {
						runReport(queue,paramsMap)
					} else {
						/*
						 * Actual request that is from a proper queueing mechanism
						 * and isEnhancedPriority
						 *  
						 */			
						def task = EnhancedPriorityBlockingExecutor?.runningJobs?.find{it?.queueId == queue.id}
						if (task) {
							RunnableFuture rFuture
							ScheduledThreadPoolExecutor timeoutExecutor = task.timeoutExecutor
							AttachedRunnable attachedRunnable= new AttachedRunnable(queue,paramsMap)
							try {
								rFuture = timeoutExecutor.submit(attachedRunnable) ///, EnhancedPriorityBlockingExecutor.keepAliveTime, EnhancedPriorityBlockingExecutor.timeoutUnit)
								EnhancedPriorityBlockingExecutor.addScheduledTask(queue.id,attachedRunnable,rFuture)
								rFuture?.get()
							}catch (e) {
								attachedRunnable.shutdown()
								timeoutExecutor.shutdownNow()
								timeoutExecutor.shutdown()
								/*
								 * It is a running live task
								 * Cancel back-end thread
								 *
								 */
								EnhancedPriorityBlockingExecutor.endRunningTask(queue.id,timeoutExecutor)

								rFuture?.cancel(true)

								/*
								 * Remove this element from runningJobs
								 */
								EnhancedPriorityBlockingExecutor?.runningJobs.remove(task)
							} finally {
								log.debug " Finished: Open Tasks: ${timeoutExecutor?.shutdownNow()?.size()}"
							}
						}
					}
				} else {
					/*
					 * Normal reports just do this
					 * enhancedPriority goes through further steps to 
					 * bind a further thread to it's own in order to be 
					 * able to cancel live running task if user requests it
					 * 
					 */
					runReport(queue,paramsMap)
				}
			} catch(LazyInitializationException ee) {
			} catch (Exception e) {
				hasException=true
				log.error(e,e)
			}

			setCompletedState(queue.id,hasException,queue.status,threadInterrupted)
		}
		/*
		 * ArrayBlocking has no method of automatically running queue
		 * for queue elements bound to ArrayBlocking - trigger
		 * check queue which will re-trigger events / jobs 
		 */
		if (queue.instanceOf(ArrayBlockingReportsQueue)) {
			def arrayBlockingReportsQueueService = grailsApplication.mainContext.getBean('arrayBlockingReportsQueueService')
			arrayBlockingReportsQueueService.checkQueue(queue.id)
		}
	}


	/**
	 * a final check done to ensure job is running correctly
	 * only required for enhancedExecutor since manual tasks 
	 * vs it's own mechanism to fire off threads ends up putting report into status
	 * finished but not actually doing task correctly. Since the task was actually within
	 * another schedule.
	 * 
	 * @param queueId
	 * @return
	 */
	boolean isManual(Long queueId) {
		boolean manual=false
		ReportsQueue.withNewTransaction {
			ReportsQueue queue3=ReportsQueue.get(queueId)
			if (queue3.manualDownload && queue3.manualDownload==1) {
				manual=true
			}
		}
		return manual
	}


	/**
	 * Ensure we are checking the latest version of this
	 * queue Element against DB - just in-case something
	 * changed status during transition
	 * @param queueId
	 */
	static boolean verifyStatusBeforeStart(Long queueId) {
		boolean validStatus=false
		ReportsQueue.withNewTransaction {
			ReportsQueue queue3=ReportsQueue.get(queueId)
			if (queue3.status==ReportsQueue.QUEUED||queue3.status==ReportsQueue.ERROR) {
				validStatus=true
				if (queue3.status==ReportsQueue.ERROR) {
					queue3.retries=queue3.retries ? queue3.retries+1 : 1
				}
				queue3.status=ReportsQueue.RUNNING
				queue3.start=new Date()
				queue3.save(flush:true)
			}
		}
		return validStatus
	}
	/**
	 * Whilst task is running properly the status should be ReportsQueue.RUNNING
	 *  Scenarios such as a user deleting / cancelling a request will change record status
	 *  This will not be visible using queue.status so instead lets get the object over again
	 *  and test real status - updating existing record if all is as expected.
	 *  otherwise interrupt the thread which should stop any further updates to this record 
	 * @param queueId
	 */
	static void setCompletedState(Long queueId, boolean hasException,String status,boolean threadInterrupted=false) {
		ReportsQueue.withNewTransaction {
			ReportsQueue queue2=ReportsQueue.get(queueId)
			if (queue2 && queue2.status == ReportsQueue.RUNNING) {
				if (!hasException && !threadInterrupted) {
					queue2.status=ReportsQueue.COMPLETED
				} else {
					queue2.status=ReportsQueue.ERROR
				}
				queue2.finished=new Date()
				queue2.save(flush:true)
			} else {
				log.debug "Queue ${queueId} has real status of ${queue2.status} had been ${status}. Task will be interrupted"
				Thread.currentThread().interrupt()
			}
		}
	}
	/**
	 * By default all extended classes use this
	 * @param queue
	 * @param bean
	 * @return
	 */
	String getReportName(ReportsQueue queue,bean) {
		return "${queue.reportName}-${queue.id}.${reportExension}"
	}

	/**
	 * By default reports do not have displayName
	 * special circumstances such as tolerance report
	 * @param bean
	 * @return
	 */
	String updateDisplayName(bean) {
		return null
	}

	/**
	 * default report set to tsv, xls reports override this
	 */
	String getReportExension() {
		return 'tsv'
	}

	/**
	 * When future.cancel(true) the thread is set to interrupted
	 *  lets double check thread status during certain updates
	 *  typically this has reached our timeout limit
	 * @return
	 */
	boolean getThreadInterrupted() {
		return Thread.currentThread().isInterrupted()
	}

	/**
	 * Sets the file path / name according to report
	 * @param queue
	 * @param bean
	 * @return
	 */
	String getFileName(ReportsQueue queue, bean) {
		boolean isLinux = config.osType != 'windows'
		String basePath=config.reportDownloadPath ?: (isLinux? '/tmp' : 'c:\\\\temp')
		File f
		String userPath=basePath+(isLinux ? '/' : '\\\\')+queue.userId
		String dateFormat=new Date().format('dd-MM-yyyy-HH-mm-ss')
		f = new File(userPath)
		if (!f.exists()) {
			f.mkdir()
		}
		return userPath+(isLinux ?  '/' :'\\\\')+getReportName(queue,bean)
	}

	/**
	 * Returns out writer to a file as UTF8
	 * used by CSV/TSV file types
	 * @param fileName
	 * @return
	 */
	Writer getOut(String fileName) {
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF8"))
	}

	/**
	 * This will be called when your service has produced
	 * no results 
	 * @param queue
	 * @param bean
	 */
	private void errorReport(ReportsQueue queue) {
		// Override reportName for special report types that share same service call/functionality
		ReportsQueue.withNewTransaction {
			ReportsQueue queue2=ReportsQueue.get(queue.id)
			if (queue2 && queue2.status == ReportsQueue.RUNNING && !threadInterrupted) {
				queue2.status=ReportsQueue.NORESULTS
				queue2.save(flush:true)
			}
		}
	}

	/**
	 * Additional db fields that require extra information to produce
	 * called by all reports
	 * @param queue
	 * @param bean
	 */
	private void actionReport(ReportsQueue queue,bean,String fileName) {
		// Override reportName for special report types that share same service call/functionality
		ReportsQueue.withNewTransaction {
			ReportsQueue queue2=ReportsQueue.get(queue.id)
			if (queue2 && queue2.status == ReportsQueue.RUNNING && !threadInterrupted) {
				queue2.fileName=fileName
				queue2.displayName=updateDisplayName(bean)
				queue2.save(flush:true)
			}
		}
	}

	ConfigObject getConfig() {
		return grailsApplication.config.queuekit ?: ''
	}
}