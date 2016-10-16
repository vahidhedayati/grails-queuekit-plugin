

package org.grails.plugin.queuekit.reports

import groovy.time.TimeCategory
import groovy.time.TimeDuration

import java.util.concurrent.RunnableFuture

import org.grails.plugin.queuekit.ArrayBlockingExecutor
import org.grails.plugin.queuekit.ArrayBlockingReportsQueue
import org.grails.plugin.queuekit.ComparableRunnable
import org.grails.plugin.queuekit.EnhancedPriorityBlockingReportsQueue
import org.grails.plugin.queuekit.LinkedBlockingExecutor
import org.grails.plugin.queuekit.LinkedBlockingReportsQueue
import org.grails.plugin.queuekit.PriorityBlockingReportsQueue
import org.grails.plugin.queuekit.QueuekitHelper
import org.grails.plugin.queuekit.ReportRunnable
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.event.ArrayBlockingQueuedEvent
import org.grails.plugin.queuekit.event.EnhancedPriorityBlockingQueuedEvent
import org.grails.plugin.queuekit.event.LinkedBlockingQueuedEvent
import org.grails.plugin.queuekit.event.PriorityBlockingQueuedEvent
import org.grails.plugin.queuekit.priority.ComparableFutureTask
import org.grails.plugin.queuekit.priority.EnhancedPriorityBlockingExecutor
import org.grails.plugin.queuekit.priority.Priority
import org.grails.plugin.queuekit.priority.PriorityBlockingExecutor
import org.grails.plugin.queuekit.validation.ChangeConfigBean
import org.grails.plugin.queuekit.validation.QueueKitBean
import org.grails.plugin.queuekit.validation.QueuekitLists
import org.grails.plugin.queuekit.validation.ReportsQueueBean

/**
 * QueueReportService is the main service that interacts with 
 * QueueKitController -
 * It carries out user UI selection i.e. Queue listing.
 * 
 * It also is the service called by your report calls 
 * buildReport  -> Two ways of being called  
 *
 */
class QueueReportService {

	def grailsApplication
	def queuekitExecutorBaseService

	def enhancedPriorityBlockingExecutor
	def priorityBlockingExecutor
	def linkedBlockingExecutor
	def arrayBlockingExecutor

	def queuekitUserService



	/**
	 * Attempts to delete actual file as well as DB entry for ReportsQueue
	 * @param id
	 * @param user
	 * @param authorized
	 * @return
	 */
	def delete(Long id,Long userId, boolean authorized,boolean safeDel) {
		ReportsQueue c=ReportsQueue.get(id)
		if (!c) return null
		boolean deleted=false
		if ((c.userId==userId||authorized) && (!safeDel||(safeDel && c.status==ReportsQueue.DOWNLOADED))) {
			if (c.fileName) {
				File file = new File(c.fileName)
				if (file) {
					file.delete()
				}
			}

			deleted=true

			/**
			 * EnhancedPriorityBlockingReportsQueue can stop running tasks
			 * This block attempts to kill the underlying thread launched
			 * with the main call. Thus stopping a live task from execution
			 * 
			 */
			if (c.isEnhancedPriority()) {
				boolean cancelled=false
				/*
				 * Check to see if is a running task ?
				 */
				ComparableFutureTask task = EnhancedPriorityBlockingExecutor?.runningJobs?.find{it.queueId == c.id}
				if (task) {
					statusDeleted(c)
					task.cancel(true)
					cancelled=true
				} else {
					/*
					 *  Confirm task is not sitting on queue ?
					 */
					ComparableFutureTask fTask = enhancedPriorityBlockingExecutor?.getQueue()?.find{it.queueId == c.id}
					if (fTask) {
						/*
						 *  It was found on queue cancel task
						 */
						statusDeleted(c)
						fTask.cancel(true)
						cancelled=true
					}
				}

				/*
				 *  Refresh queue elements
				 */
				if (cancelled) {
					enhancedPriorityBlockingExecutor.purge()
				}
			} else {
				/*
				 * All other ThreadExecutors 
				 */
				switch (c.queueLabel) {
					case ReportsQueue.LINKEDBLOCKING:
						linkedBlockingExecutor?.getQueue()?.find{it.queueId == c.id}?.cancel(true)
						linkedBlockingExecutor.purge()
						break
					case ReportsQueue.ARRAYBLOCKING:
						arrayBlockingExecutor?.getQueue()?.find{it.queueId == c.id}?.cancel(true)
						arrayBlockingExecutor.purge()
						break
					case ReportsQueue.PRIORITYBLOCKING:
						priorityBlockingExecutor?.getQueue()?.find{it.queueId == c.id}?.cancel(true)
						priorityBlockingExecutor.purge()
						break
				}
				statusDeleted(c)
			}
		}
		return deleted
	}

	private void statusDeleted(ReportsQueue c) {
		ReportsQueue.withTransaction {
			c.status=ReportsQueue.DELETED
			c.save()
		}
	}

	/**
	 * Cancels a request from the queue by setting status to error
	 * @param queueId
	 * @param params
	 * @return
	 */
	public boolean cancel(Long queueId,Map params) {
		boolean result=false
		ReportsQueue queue=ReportsQueue.get(queueId)
		if (queue?.status==ReportsQueue.RUNNING) {
			queue.status=ReportsQueue.ERROR
			queue.save()
			result=true
		}
		return result
	}



	/**
	 *  Simply update queue with status Downloaded
	 */
	def markDownloaded(Long queueId) {
		ReportsQueue queue = ReportsQueue.get(queueId)
		if (queue) {
			queue.status=ReportsQueue.DOWNLOADED
			queue.save()
		}

	}

	/**
	 * modify configuration types of given TaskExecutor mode
	 * modifies max thread / preserve values + preserve groups
	 * also controls shutdown and manual re-queue 
	 * @param queue
	 * @param changeValue
	 * @param changeType
	 * @param priority
	 * @return
	 */
	def modifyConfiguration(String queueLabel,int changeValue, String changeType, Priority priority=Priority.MEDIUM, int floodControl, boolean defaultComparator) {
		switch (queueLabel) {
			case ReportsQueue.LINKEDBLOCKING:
				LinkedBlockingExecutor ex = new LinkedBlockingExecutor()
				if (changeType == QueuekitLists.POOL) {
					ex.maximumPoolSize=changeValue
				} else if (changeType == QueuekitLists.MAXQUEUE) {
					ex.maxQueue=changeValue
				} else if (changeType == QueuekitLists.CHECKQUEUE) {
					queuekitExecutorBaseService.checkQueue(LinkedBlockingReportsQueue.class)
				} else if (changeType == ChangeConfigBean.STOPEXECUTOR) {
					linkedBlockingExecutor.shutdown()
				}
				break
			case ReportsQueue.ARRAYBLOCKING:
				ArrayBlockingExecutor ex = new ArrayBlockingExecutor()
				if (changeType == QueuekitLists.POOL) {
					ex.maximumPoolSize=changeValue
				} else if (changeType == QueuekitLists.MAXQUEUE) {
					ex.maxQueue=changeValue
				} else if (changeType == QueuekitLists.CHECKQUEUE) {
					queuekitExecutorBaseService.checkQueue(ArrayBlockingReportsQueue.class)
				}
				break
			case ReportsQueue.PRIORITYBLOCKING:
				PriorityBlockingExecutor ex = new PriorityBlockingExecutor()
				actionModifyType(changeType,changeValue,priority,floodControl,ex,priorityBlockingExecutor, defaultComparator)
				break
			case ReportsQueue.ENHANCEDPRIORITYBLOCKING:
				EnhancedPriorityBlockingExecutor ex = new EnhancedPriorityBlockingExecutor()
				actionModifyType(changeType,changeValue,priority,floodControl,ex,enhancedPriorityBlockingExecutor, defaultComparator)
				break
		}
	}

	private void actionModifyType(String changeType,int changeValue,Priority priority=Priority.MEDIUM,int floodControl,ex,executor,boolean defaultComparator) {
		switch (changeType) {
			case QueuekitLists.POOL:
				ex.maximumPoolSize=changeValue
				break
			case QueuekitLists.MAXQUEUE:
				ex.maxQueue=changeValue
				break
			case QueuekitLists.PRESERVE:
				if (changeValue < ex.maximumPoolSize) {
					ex.minPreserve=changeValue
					ex.definedPriority=priority
				}
				break
			case QueuekitLists.CHECKQUEUE:
				if (changeType == ChangeConfigBean.CHECKQUEUE) {
					queuekitExecutorBaseService.checkQueue(ex.class)
				}
				break
			case QueuekitLists.DEFAULTCOMPARATOR:
				ex.defaultComparator=defaultComparator
				break
			case QueuekitLists.STOPEXECUTOR:
				executor.shutdown()
				break
			case QueuekitLists.LIMITUSERABOVE:
				ex.limitUserAbovePriority=changeValue
				break
			case QueuekitLists.LIMITUSERBELOW:
				ex.limitUserBelowPriority=changeValue
				break
			case QueuekitLists.FLOODCONTROL:
				ex.forceFloodControl=(floodControl)
				break
		}
	}
	/**
	 * As per name when user selects
	 * modify priority on a given queue item
	 * it in turn runs this which - re-shuffles the priority 
	 * if it was low task and now high priority maybe it will run next
	 * @param queue
	 * @param priority
	 * @return
	 */
	def changeQueuePriority(queue, Priority priority) {
		if (queue?.isEnhancedPriority() && priority) {
			try {

				/*
				 *  To shuffle a queue Position it must therefore be in the queue and in QUEUED state
				 *  Confirm queueId exists in the queue collection
				 *  
				 *  if found cancel the task then reschedule it all over again this time with the updated priority
				 *   
				 */

				ComparableFutureTask fTask = enhancedPriorityBlockingExecutor?.getQueue()?.find{it.queueId == queue.id}
				if (fTask) {
					fTask.cancel(true)

					new Thread({
						def currentTask
						if (config.standardRunnable) {
							currentTask = new ReportRunnable(queue)
						} else {
							currentTask = new ComparableRunnable(queue)
						}
						RunnableFuture task = enhancedPriorityBlockingExecutor.execute(currentTask,priority.value)
						task?.get()
					} as Runnable ).start()

					if (queue.priority && queue.priority != priority) {
						queue.priority=priority
						queue.save()
					}

					enhancedPriorityBlockingExecutor.purge()
				}

			}catch(e) {
			}
		}

	}

	/**
	 * binds with front end user call to delete all or downloaded reports
	 * @param bean
	 * @param downloadedOnly optional if provided will only delete downloaded
	 * @return
	 */
	boolean clearUserReports(QueueKitBean bean, String deleteType) {
		def query=" from ReportsQueue rq where rq.userId =:currentUser and "
		def whereParams=[:]
		if (deleteType==QueueKitBean.DELALL) {
			query +="rq.status != :running and rq.status!=:deleted"
			whereParams.running=ReportsQueue.RUNNING
			whereParams.deleted=ReportsQueue.DELETED
		} else {
			query +="rq.status = :requestType"
			whereParams.requestType=deleteType
		}
		def metaParams=[readOnly:true,timeout:15,max:-1]
		whereParams.currentUser=bean.userId
		def results=ReportsQueue.executeQuery(query,whereParams,metaParams)
		def found = results?.size()
		results?.each {ReportsQueue queue ->

			if (queue.fileName) {
				try {
					File file = new File(queue.fileName)
					if (file) {
						file.delete()
					}
				}catch(Exception e) {
				}
			}

			ReportsQueue.withTransaction{
				queue.status=ReportsQueue.DELETED
				queue.save()
			}

			/*
			 * Iterate through  each getQueue = listing of ComparableFutureTask (s)
			 * if current queue.id matches queueId of  ComparableFutureTask.queueId collection then remove it
			 * finally purge executor - resetting queue
			 *
			 */
			switch (queue?.queueLabel) {
				case ReportsQueue.LINKEDBLOCKING:
					linkedBlockingExecutor?.getQueue()?.find{it.queueId == queue.id}?.cancel(true)
					linkedBlockingExecutor.purge()
					break
				case ReportsQueue.ARRAYBLOCKING:
					arrayBlockingExecutor?.getQueue()?.find{it.queueId == queue.id}?.cancel(true)
					arrayBlockingExecutor.purge()
					break
				case ReportsQueue.PRIORITYBLOCKING:
					priorityBlockingExecutor?.getQueue()?.find{it.queueId == queue.id}?.cancel(true)
					priorityBlockingExecutor.purge()
					break
				case ReportsQueue.ENHANCEDPRIORITYBLOCKING:
					enhancedPriorityBlockingExecutor?.getQueue()?.find{it.queueId == queue.id}?.cancel(true)
					enhancedPriorityBlockingExecutor.purge()
					break
			}
		}
		return (found>0)
	}

	/**
	 * Quartz Schedule: this task
	 * @return
	 */
	def deleteReportFiles() {
		def whereParams=[:]
		def query="""from ReportsQueue rq where (
				(rq.created <=:delDate and rq.status!=:downloaded) 
				or (rq.created <=:downloadedDate and rq.status=:downloaded)
			)
			"""
		int removalDay = config.removalDay ?: 5
		int removalDownloaded = config.removalDownloadedDay ?: 1
		whereParams.delDate=new Date()-removalDay
		whereParams.downloadedDate=new Date() -removalDownloaded
		whereParams.downloaded=ReportsQueue.DOWNLOADED
		def metaParams=[readOnly:true,timeout:15,max:-1]
		def results=ReportsQueue.executeQuery(query,whereParams,metaParams)
		results?.fileName?.each { f->
			if (f) {
				try {
					File file = new File(f)
					if (file) {
						file.delete()
					}
				}catch(Exception e) {
				}
			}
		}
		if (config.deleteEntryOnDelete) {
			results*.delete()
		}
	}


	/**
	 * Retrieve file attempts to load up a file if reportsQueue has a fileName
	 * Returns file object to controller: queueKit  action: download
	 * @param id
	 * @param user
	 * @param authorized
	 * @return
	 */
	def retrieveFile(Long id,Long userId, boolean authorized) {
		File file
		ReportsQueue c=ReportsQueue.get(id)
		if (!c) return null
		def recId=c.id
		String record=c.toString()
		if ((c.userId==userId||authorized) && c.fileName) {
			file = new File(c.fileName)
		}
		return file
	}

	/**
	 * Generates listing view for controller: queueKit, action: listQueue
	 * @param bean
	 * @return
	 */
	def list(QueueKitBean bean) {
		def start=System.currentTimeMillis()
		def query
		def where=''
		def whereParams=[:]
		def sorts=['reportName', 'created', 'startDate', 'finishDate' ,'user', 'status', 'priority','queueType','duration','userId']
		def sorts2=['rq.reportName', 'rq.created', 'rq.start','rq.finished','rq.userId','rq.status', 'rq.priority','rq.class','rq.finished-rq.start','rq.userId']
		def sortChoice=sorts.findIndexOf{it==bean.sort}

		// FROM_UNIXTIME(UNIX_TIMESTAMP(coalesce(rq.finished,rq.start)) -  UNIX_TIMESTAMP(rq.start))

		query="""select new map(rq.id as id, rq.retries as retries, rq.paramsMap as paramsMap,
		coalesce(rq.displayName,rq.reportName) as reportName, 
		rq.userId as userId,
		rq.reportName as realReport,
			(case 
				when rq.class=ReportsQueue then '${ReportsQueue.ENHANCEDPRIORITYBLOCKING}'
				when rq.class=EnhancedPriorityBlockingReportsQueue then '${ReportsQueue.ENHANCEDPRIORITYBLOCKING}'
				when rq.class=PriorityBlockingReportsQueue then '${ReportsQueue.PRIORITYBLOCKING}'
				when rq.class=LinkedBlockingReportsQueue then '${ReportsQueue.LINKEDBLOCKING}'
				when rq.class=ArrayBlockingReportsQueue then '${ReportsQueue.ARRAYBLOCKING}'
			end) as queueType,
			(case 
				when rq.class=PriorityBlockingReportsQueue then rq.priority
				when rq.class=EnhancedPriorityBlockingReportsQueue then rq.priority					
			end) as priority,
		rq.fileName as fileName, rq.start as startDate, rq.created as created, 
		rq.finished as finishDate, rq.status as status
	)
	from ReportsQueue rq  """

		if (!bean.superUser || (bean.superUser && (bean.status != ReportsQueue.OTHERUSERS||bean.searchBy && bean.searchBy!=QueuekitLists.USER))) {
			where=addClause(where,'rq.userId=:userId')
			whereParams.userId=bean.userId
		}
		if (!bean.superUser && bean.status!=ReportsQueue.DOWNLOADED ||bean.superUser && (bean.status!=ReportsQueue.DELETED||bean.status!=ReportsQueue.DOWNLOADED)) {
			where=addClause(where,'rq.status not in (:statuses) ')
			def statuses=[]

			if (!bean.superUser||bean.superUser && bean.status != ReportsQueue.DOWNLOADED) {
				statuses << ReportsQueue.DOWNLOADED
			}
			if (!bean.superUser||bean.superUser && bean.status != ReportsQueue.DELETED) {
				statuses << ReportsQueue.DELETED
			}
			whereParams.statuses=statuses
		}

		if (bean.status && bean.status != ReportsQueue.OTHERUSERS) {
			where=addClause(where,'rq.status=:status')
			whereParams.status=bean.status
		}

		if (bean.searchBy) {
			if (bean.searchBy==QueuekitLists.REPORTNAME) {
				where=addClause(where,'rq.reportName like :reportSearch')
				whereParams.reportSearch='%'+bean.searchFor+'%'
			} else if (bean.searchBy==QueuekitLists.USER && bean.superUser) {
				Long userId = queuekitUserService.getRealUserId(bean.searchFor)
				if (userId) {
					where=addClause(where,'rq.userId=:userId')
					whereParams.userId=userId
				}
			}

		}
		
		query+=where
		def metaParams=[readOnly:true,timeout:15,offset:bean.offset?:0,max:bean.max?:-1]
		if (sortChoice>0) {
			query+=" order by ${sorts2[sortChoice]} $bean.order"
		} else {
			query+=" order by rq.created $bean.order"
		}
		def results=ReportsQueue.executeQuery(query,whereParams,metaParams)
		int total=results.size()
		if (total>=metaParams.max) {
			total=ReportsQueue.executeQuery("select count(*) from ReportsQueue rq "+where,whereParams,[readOnly:true,timeout:15,max:1])[0]
		} else {
			total+=metaParams.offset as Long
		}
		/*
		 * load up Config.groovy durationThreshHold
		 * recollect setting any unset values to 0
		 */
		def durationThreshHold=config.durationThreshHold
		def threshHold = durationThreshHold?.collect{[hours: it.hours?:0,minutes:it.minutes?:0,seconds:it.seconds?:0,color:it.color?:'']}

		results=results?.each { instance ->
			/*
			 * If it is complete
			 */
			if (instance.finishDate) {
				/*
				 * Compare start/end
				 */
				TimeDuration duration = TimeCategory.minus(instance.finishDate, instance.startDate)
				if (duration && threshHold) {
					instance.duration=duration
					/*
					 * Find the nearest match from durationThreshHold
					 */
					def match = threshHold.findAll{ k-> k.hours <= duration.hours && \
						k.minutes <= duration.minutes && k.seconds <= duration.seconds}\
					.sort{a,b-> a.hours<=>b.hours ?: a?.minutes<=>b?.minutes?: a?.seconds<=>b?.seconds  }
					if (match) {
						instance.color=match.last().color
					}
				}
			}
			if (instance.queueType==ReportsQueue.PRIORITYBLOCKING||instance.queueType==ReportsQueue.ENHANCEDPRIORITYBLOCKING) {
				instance.priority=instance.priority?:QueuekitHelper.sortPriority(instance.realReport)
			}

			instance.username = queuekitUserService.getUsername(instance.userId)
		}
		def instanceList=[results:results]

		def runTypes =results?.collect{it.queueType}.sort()?.unique()
		def queueTypes=[]
		int defaultPriority = (config.queuekit?.preservePriority ?: Priority.MEDIUM).value
		runTypes?.each {queueType->
			queueTypes << getJobsAvailable(queueType,defaultPriority,bean.userId)
		}
		instanceList << [reportJobs:queueTypes]

		return [instanceList:instanceList, instanceTotal:total, superUser:bean.superUser, statuses:bean.statuses,
				searchList  :bean.searchList, deleteList:QueuekitLists.deleteList, adminButtons:bean.adminButtons]
	}

	/**
	 * Works out and returns a map of total jobs queued / running and available limit
	 */
	Map getJobsAvailable(String queueType=null,int defaultPriority=null,Long userId=null) {
		int queued,running,maxPoolSize,minPreserve,limitUserBelowPriority,limitUserAbovePriority,forceFloodControl,maxQueue
		boolean isAdvanced
		Priority priority
		def executorCount=[:]
		switch (queueType) {
			case ReportsQueue.LINKEDBLOCKING:
				queued=linkedBlockingExecutor.getQueue().size()
				running=linkedBlockingExecutor.getActiveCount()
				maxPoolSize=LinkedBlockingExecutor.maximumPoolSize
				maxQueue=LinkedBlockingExecutor.maxQueue
				break
			case ReportsQueue.ARRAYBLOCKING:
				queued=arrayBlockingExecutor.getQueue().size()
				running=arrayBlockingExecutor.getActiveCount()
				maxPoolSize=ArrayBlockingExecutor.maximumPoolSize
				maxQueue=ArrayBlockingExecutor.maxQueue
				break
			case ReportsQueue.PRIORITYBLOCKING:
				executorCount = QueuekitHelper.executorCount(EnhancedPriorityBlockingExecutor.runningJobs,enhancedPriorityBlockingExecutor.getQueue(),defaultPriority,userId)
				queued=priorityBlockingExecutor.getQueue().size()
				running=priorityBlockingExecutor.getActiveCount()
				maxPoolSize=PriorityBlockingExecutor.maximumPoolSize
				maxQueue=PriorityBlockingExecutor.maxQueue
				minPreserve=PriorityBlockingExecutor.minPreserve
				limitUserBelowPriority=PriorityBlockingExecutor.limitUserBelowPriority
				limitUserAbovePriority=PriorityBlockingExecutor.limitUserAbovePriority
				isAdvanced=true
				forceFloodControl=PriorityBlockingExecutor.forceFloodControl
				if (minPreserve>0) {
					priority=PriorityBlockingExecutor.definedPriority
				}

				break
			case ReportsQueue.ENHANCEDPRIORITYBLOCKING:
				executorCount = QueuekitHelper.executorCount(EnhancedPriorityBlockingExecutor.runningJobs,enhancedPriorityBlockingExecutor.getQueue(),defaultPriority,userId)
				queued=enhancedPriorityBlockingExecutor.getQueue().size()
				running=enhancedPriorityBlockingExecutor.getActiveCount()
				maxPoolSize=EnhancedPriorityBlockingExecutor.maximumPoolSize
				maxQueue=EnhancedPriorityBlockingExecutor.maxQueue
				minPreserve=EnhancedPriorityBlockingExecutor.minPreserve
				limitUserBelowPriority=EnhancedPriorityBlockingExecutor.limitUserBelowPriority
				limitUserAbovePriority=EnhancedPriorityBlockingExecutor.limitUserAbovePriority
				forceFloodControl=EnhancedPriorityBlockingExecutor.forceFloodControl
				isAdvanced=true
				if (minPreserve>0) {
					priority=EnhancedPriorityBlockingExecutor.definedPriority
				}
				break
		}

		return [queueType:queueType ?: ReportsQueue.ENHANCEDPRIORITYBLOCKING  , maxPoolSize:maxPoolSize,
			limitUserBelowPriority:limitUserBelowPriority,limitUserAbovePriority:limitUserAbovePriority,
			forceFloodControl:forceFloodControl,isAdvanced:isAdvanced,maxQueue:maxQueue,
			running:running,queued:queued,minPreserve:minPreserve,priority:priority,executorCount:executorCount]
	}

	/**
	 * Main point of buildReport used by binding to ReportsQueueBean
	 * refer to ReportsDemoController / indexBean example
	 * 
	 * Allows fromController / fromAction fields to be collected
	 * which gives extended functionality on display screen
	 * 
	 * Will then be able to generate report re-generation/re-run 
	 * 
	 * @param bean
	 * @return
	 */
	def buildReport(ReportsQueueBean bean) {
		functionReport(bean.bindReport())
	}


	/**
	 * BuildReport is a wrapper for prepartion of saving a new report
	 * calls save method further down once it has built a map
	 * 
	 * @param reportName  --Important
	 * 
	 * If reportName is "pdfReport" Then you must create 
	 * 	"PdfReportReportingService" 
	 * 	that extends ReportingService - follow existing examples
	 * 
	 * Important since you must ensure you use short and valid naming convention
	 * If you called it reportName="hereIsAVeryVeryLongNameAndMore"
	 * Then you would have to have
	 * "HereIsAVeryVeryLongNameAndMoreReportingService"
	 * 
	 * Grails has a restriction on service naming convention
	 * 
	 * Important since you should refrain from using space or _ - or anything invalid for that matter in the reportName
	 *  
	 * @param user
	 * @param locale
	 * @param params
	 * 
	 * @param reportType = P Default PriorityBlocking - if not provided
	 * 				 L LinkedBlocking  - has no priority queue managed on it's own
	 * 				 A ArrayBlocking  - has no priority - queue manually managed through DB
	 * @return
	 */
	def buildReport(String reportName, Long userId, Locale locale, params,String reportType=null) {
		def values=[:]
		values.reportName=reportName
		values.userId=userId
		values.locale=locale
		values.paramsMap=params
		values.reportType=reportType
		functionReport(values)
	}



	/**
	 * Same as above but with Priority and reportType overrides 
	 * define a priority to override configuration requirement for a given task
	 * 
	 * @param reportName
	 * @param userId
	 * @param locale
	 * @param params
	 * @param priority Directly define Priority i.e. Priority.HIGHEST or Priority.LOWEST
	 * 
	 * @param reportType optional  = P Default PriorityBlocking - if not provided
	 * 				 L LinkedBlocking  - has no priority queue managed on it's own
	 * 				 A ArrayBlocking  - has no priority - queue manually managed through DB
	 * @return
	 */

	def buildReport(String reportName, Long userId, Locale locale, params,Priority priority,String reportType=null) {
		def values=[:]
		values.reportName=reportName
		values.userId=userId
		values.locale=locale
		values.paramsMap=params
		values.priority=priority
		values.reportType=reportType
		if (values.reportType !=  ReportsQueue.ENHANCEDPRIORITYBLOCKING && values.reportType !=  ReportsQueue.PRIORITYBLOCKING) {
			values.reportType = ReportsQueue.ENHANCEDPRIORITYBLOCKING
		}
		functionReport(values)
	}

	def functionReport(Map values) {
		/*
		 * If no reportType defined default to ReportsQueue.ENHANCEDPRIORITYBLOCKING
		 */
		if (!values.reportType) {
			values.reportType = config.defaultReportsQueue ?: ReportsQueue.ENHANCEDPRIORITYBLOCKING
		}
		def queue
		/*
		 * Decide which reportType it is and set correct queue class type
		 */
		switch (values.reportType) {
			case ReportsQueue.ARRAYBLOCKING :
				queue = queueArrayBlocking(values)
				break
			case ReportsQueue.LINKEDBLOCKING :
				queue = queueLinkBlocking(values)
				break
			case ReportsQueue.PRIORITYBLOCKING :
				queue = queuePriorityBlocking(values)
				break
			default:
			/*
			 *  Default to enhancedPriorityBlockingDomainClass
			 */
				queue = enhancedqueuePriorityBlocking(values)
		}
		save(values,queue)
	}

	/**
	 * return queue class as LinkedBlockingReportsQueue
	 * @param values
	 * @return
	 */
	def queueLinkBlocking(values) {
		LinkedBlockingReportsQueue queue
		if (values.id) {
			queue = LinkedBlockingReportsQueue.get(values.id)
		} else {
			queue = new LinkedBlockingReportsQueue()
		}
		return queue
	}

	/**
	 * return queue class as ArrayBlockingReportsQueue
	 * @param values
	 * @return
	 */
	def queueArrayBlocking(values) {
		ArrayBlockingReportsQueue queue
		if (values.id) {
			queue = ArrayBlockingReportsQueue.get(values.id)
		} else {
			queue = new ArrayBlockingReportsQueue()
		}
		return queue
	}

	/**
	 * Return queue as PriorityBlockingReportsQueue domainClass
	 * @param values
	 * @return
	 */
	def queuePriorityBlocking(values) {
		PriorityBlockingReportsQueue queue
		if (values.id) {
			queue = PriorityBlockingReportsQueue.get(values.id)
		} else {
			queue = new PriorityBlockingReportsQueue()
		}
		return queue
	}

	/**
	 * Return queue as EnhancedPriorityBlockingReportsQueue domainClass
	 * @param values
	 * @return
	 */
	def enhancedqueuePriorityBlocking(values) {
		EnhancedPriorityBlockingReportsQueue queue
		if (values.id) {
			queue = EnhancedPriorityBlockingReportsQueue.get(values.id)
		} else {
			queue = new EnhancedPriorityBlockingReportsQueue()
		}
		return queue
	}

	/**
	 * Generic save method that should be called from any report that requires queueing
	 * @param params
	 * @return
	 */
	def save(values,queue) {
		update(values,queue)
	}


	/**
	 * override method of update passing a map into ReportsQueueBean
	 * @param values
	 * @param queue
	 * @return
	 */
	def update(Map values, queue) {
		ReportsQueueBean bean = new ReportsQueueBean()
		bean.bindBean(values)
		update(bean,queue)
	}

	/**
	 * this is the main update method that actually updates the DB and triggers event
	 * @param bean
	 * @param queue
	 * @return
	 */

	def update(ReportsQueueBean bean, queue) {
		queue.reportName=bean.reportName
		queue.paramsMap=bean.paramsMap
		queue.userId=bean.userId
		queue.locale=bean.locale
		queue.fileName=bean.fileName
		queue.fromController=bean.fromController
		queue.fromAction=bean.fromAction

		if (bean.priority && queue.hasPriority()) {
			if (config.standardRunnable && config.disableUserServicePriorityCheck) {
				queue.priority= queuekitUserService.reportPriority(queue,bean.priority,bean.parameters)
			} else {
				queue.priority=bean.priority
			}
		}

		queue.created=new Date()
		queue.save(flush:true)


		/*
		 * Generation of new records specially if
		 * end user is hammering reports button can 
		 * sometimes cause item to be missed
		 * lets capture it and slow down the pace a little
		 *  
		 */
		new Thread({
			sleep(500)
			switch (queue?.queueLabel) {
				case ReportsQueue.LINKEDBLOCKING:
					publishEvent(new LinkedBlockingQueuedEvent(queue.id))
					break
				case ReportsQueue.ARRAYBLOCKING:
					publishEvent(new ArrayBlockingQueuedEvent(queue.id))
					break
				case ReportsQueue.PRIORITYBLOCKING:
					publishEvent(new PriorityBlockingQueuedEvent(queue.id))
					break
				case ReportsQueue.ENHANCEDPRIORITYBLOCKING:
					publishEvent(new EnhancedPriorityBlockingQueuedEvent(queue.id))
					break
			}
		} as Runnable ).start()
		return queue
	}

	private String addClause(String where,String clause) {
		return (where ? where + ' and ' : 'where ') + clause
	}

	ConfigObject getConfig() {
		return grailsApplication.config.queuekit ?: ''
	}
}
