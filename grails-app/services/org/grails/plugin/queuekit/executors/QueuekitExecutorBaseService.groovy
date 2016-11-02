package org.grails.plugin.queuekit.executors

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware
import grails.util.Holders
import org.grails.plugin.queuekit.ReportsQueue

/**
 * QueuekitExecutorBaseService is the main class that holds global actions used by
 * -> ArrayBlockingReportsQueueService which extends queuekitExecutorBaseService
 * -> LinkedBlockingReportsQueueService which extends queuekitExecutorBaseService
 * -> PriorityBlockingReportsQueueService which extends queuekitExecutorBaseService
 * 
 * 1. Default checkQueue action provided used by :
 * 	-> LinkedBlockingReportsQueueService
 * 	-> PriorityBlockingReportsQueueService
 * 
 *  ArrayBlockingReportsQueueService overrides checkQueue
 *  with a custom way required to manually manage queue
 *  
 * 2. cancel - actually called in by end user form action
 *  
 * 3. rescheduleRequeue - used by BootStrap call
 * 		It sets running status back to queued - then checkQueue picks up requests. 
 * 		and if configured will re-trigger checkQueue - only required for all types
 *      if app stop/start with records still awaiting - to initiate queue processing *      
 *      
 *    To use in your app - simply change below:
 *    
 *      def queuekitExecutorBaseService
 *      queuekitExecutorBaseService.rescheduleRequeue() in your application BootStrap.groovy
 *      
 *      ensure you have enabled configuration value in Config.groovy:
 *      queuekit.checkQueueOnStart=true
 * 
 * @author Vahid Hedayati
 *
 */
class QueuekitExecutorBaseService implements GrailsApplicationAware {

	def config
	GrailsApplication grailsApplication

	void checkQueue(Long id=null) {
		def inputParams=[:]
		String addon=''
		if (id) {
			addon='and rq.id!=:id'
			inputParams.id=id
		}
		processCheckQueue(addon,inputParams)
	}
	void checkQueue(Class clazz) {
		String addon=''
		def inputParams=[:]
		if (clazz) {
			addon="and rq.class = :className"
		}
		inputParams.className=clazz.name
		processCheckQueue(addon,inputParams)
	}

	void processCheckQueue(String addon,Map inputParams=[:]) {
		inputParams.status=ReportsQueue.QUEUED
		def query="""select new map(rq.id as id,
(case 
				when rq.class=ReportsQueue then '${ReportsQueue.ENHANCEDPRIORITYBLOCKING}'
				when rq.class=EnhancedPriorityBlockingReportsQueue then '${ReportsQueue.ENHANCEDPRIORITYBLOCKING}'
				when rq.class=PriorityBlockingReportsQueue then '${ReportsQueue.PRIORITYBLOCKING}'
				when rq.class=LinkedBlockingReportsQueue then '${ReportsQueue.LINKEDBLOCKING}'
				when rq.class=ArrayBlockingReportsQueue then '${ReportsQueue.ARRAYBLOCKING}'
			end) as queueType
) from ReportsQueue rq where rq.status=:status
						 $addon order by rq.queuePosition asc, id asc
				"""
		def metaParams=[readOnly:true,timeout:15,max:-1,cache: false]
		def waiting
		ReportsQueue.withNewTransaction {
			waiting=ReportsQueue.executeQuery(query,inputParams,metaParams)
		}
		log.debug "waiting reports ${waiting.size()}"
		waiting?.each{queue ->

			new Thread({
				sleep(500)
				switch (queue?.queueType) {
					case ReportsQueue.LINKEDBLOCKING:
						notify( "method.linkedBlocking",queue.id)
						break
					case ReportsQueue.ARRAYBLOCKING:
						notify( "method.arrayBlocking",queue.id)
						break
					case ReportsQueue.PRIORITYBLOCKING:
						notify( "method.priorityBlocking",queue.id)
						break
					case ReportsQueue.ENHANCEDPRIORITYBLOCKING:
						notify( "method.enhancedPriorityBlocking",queue.id)
						break
				}

			} as Runnable ).start()
		}
	}


	/**
	 * Shared functionality used by: 
	 * Linked/Priority/EnhancedPriority
	 * 
	 * If ThreadExecutor is down + backup not enabled or also down
	 * will run this to actually process report as a physical thread 
	 * exactly how user would normally be using the site.
	 * 
	 * @param queue
	 * @return
	 */
	def executeManualReport(ReportsQueue queue){
		new Thread({
			String name = queue.reportName+queue.serviceLabel
			try {
				def currentService =  Holders.grailsApplication.mainContext.getBean(name)
				currentService.executeReport(queue)
			} catch (InterruptedException e) {
				e.printStackTrace()
			}
		} as Runnable ).start()
	}
	/**
	 * This is executed before above in each of the related services
	 * Sets the current job to be of manualDownload
	 * @param queueId
	 * @return
	 */
	def setManualStatus(Long queueId) {
		ReportsQueue.withNewTransaction {
			ReportsQueue queue = ReportsQueue.get(queueId)
			queue.manualDownload=1
			queue.save(flush:true)
		}
	}
	/**
	 * 
	 * Attempt to manually run report and present output back to user 
	 * as a direct download.
	 * 
	 * @param queue
	 * @return
	 def manuallyRunReport(queue) {
	 String name = queue.reportName+queue.serviceLabel
	 try {
	 def currentService =  grailsApplication.mainContext.getBean(name)
	 currentService.executeReport(queue)
	 } catch (InterruptedException e) {
	 e.printStackTrace()
	 }
	 }
	 */
	/**
	 * This is called by Bootstrap to ensure no tasks are left running from last restart
	 * @return
	 */
	def rescheduleRequeue() {
		ReportsQueue.withNewTransaction{
			def running = ReportsQueue.where{status==ReportsQueue.RUNNING}.findAll()
			running.each { ReportsQueue queue ->
				log.debug "Job ${queue.id} had been running. Setting status to queued"
				queue.status=ReportsQueue.QUEUED
				queue.save(flush:true)
			}
		}
		if (config.checkQueueOnStart) {
			checkQueue()
		}
	}

	void setGrailsApplication(GrailsApplication ga) {
		config = ga.config.queuekit
	}
}
