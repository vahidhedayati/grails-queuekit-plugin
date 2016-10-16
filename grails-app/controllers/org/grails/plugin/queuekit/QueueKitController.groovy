package org.grails.plugin.queuekit

import org.grails.plugin.queuekit.validation.ChangeConfigBean
import org.grails.plugin.queuekit.validation.ChangePriorityBean
import org.grails.plugin.queuekit.validation.QueueKitBean
import org.grails.plugin.queuekit.validation.QueuekitLists
import org.grails.plugin.queuekit.validation.ReportsQueueBean
import org.springframework.dao.DataIntegrityViolationException

/**
 * Main controller that provides a visible front end
 * to your reports queues.
 * 
 * Control report output, extend QueuekitUserService 
 * and define actual userId, userName, user.locale  
 * + authority -> 
 * 		superUser can view all user reports + deleted records 
 *  	normalUser -> only interacts with own reports + no deleted record history
 *  
 *  With correct output from overridden QueuekitUserService 
 *  the results of this report then matches returned service calls
 *  i.e : bean.userId = queuekitUserService.currentuser
 *  
 * @author Vahid Hedayati
 *
 */

class QueueKitController {
	
	static defaultAction = 'listQueue'
	
	static allowedMethods = [save: 'POST']
	
	def queueReportService
	def arrayBlockingReportsQueueService
	def queuekitUserService

	/**
	 * Display a given report Queue record
	 * @return
	 */
	def display() {
		if (params.id) {
			def queue
			switch (params?.queueType) {
				case ReportsQueue.LINKEDBLOCKING:
					queue = LinkedBlockingReportsQueue.load(params.long('id'))
					break
				case ReportsQueue.ARRAYBLOCKING:
					queue = ArrayBlockingReportsQueue.load(params.long('id'))
					break
				case ReportsQueue.PRIORITYBLOCKING:
					queue = PriorityBlockingReportsQueue.load(params.long('id'))
					break
				case ReportsQueue.ENHANCEDPRIORITYBLOCKING:
					queue = EnhancedPriorityBlockingReportsQueue.load(params.long('id'))
					break
				default:
					queue = ReportsQueue.load(params.long('id'))
			}
			ReportsQueueBean bean = new ReportsQueueBean().formatBean(queue)
			if (request.xhr) {
				render template:'showContent',model:[instance:bean]
			} else {
				render view:'show',model:[instance:bean]
			}
			return
		}
		render status:response.SC_NOT_FOUND
	}

	/*
	 * Default action returns reports produced
	 * for current userId
	 */
	def listQueue(QueueKitBean bean) {
		bean.userId = queuekitUserService.currentuser
		if (bean.userId) {
			def results=queueReportService.list(bean)
			def search = bean.search
			results.search=search
			//session.queueKitSearch=search
			if (!results.instanceList.results) {
				flash.message = message(code: 'queuekit.noRecordsFound.message')				
			}
			if (request.xhr) {
				render template:'list', model:results, status: response.SC_OK
				return
			}
			render view:'main',model:results
			return			
		}		
		render status:response.SC_NOT_FOUND
	}


	/**
	 * Download button from queueKit listing
	 * @param bean
	 * @return
	 */
	def download(QueueKitBean bean) {
		bean.userId = queuekitUserService.currentuser
		boolean authorizeduser=bean.superUser ? true : false
		File file = queueReportService.retrieveFile(params.id as Long,bean.userId,authorizeduser)
		if (file && file.exists()) {
			queueReportService.markDownloaded(params.id as Long)
			response.setContentType("application/octet-stream")
			response.setHeader("Content-disposition", "filename=${file.name}")
			response.outputStream << file.bytes
			return
		}
		response.status=response.SC_CONFLICT

	}

	
	/**
	 * Deletes a given record ID - front end queueKit listing delete button action
	 * @param bean
	 * @return
	 */
	def delRecord(QueueKitBean bean) {
		try {
			flash.message = message(code: 'deletion.failure.message')
			bean.userId = queuekitUserService.currentuser
			boolean authorizeduser=bean.superUser ? true : false
			def deleted=queueReportService.delete(params.id as Long,bean.userId,authorizeduser,bean.safeDel)
			if (deleted) {
				flash.message = message(code: 'default.deleted.message', args: [message(code: 'queuekit.record.label'), params.id])
				redirect(action: "listQueue",params:bean.search)
				return
			}
		} catch (DataIntegrityViolationException e) {
		} catch (Throwable t) {
			flash.message= t.toString()
		}
		listQueue()
	}

	/**
	 * Re-queue a file only used by ArrayBlocking
	 */
	def requeue() {
		// id is the id of the queue entry
		boolean success=arrayBlockingReportsQueueService.requeue(params.long('id'))
		response.status=success ? response.SC_CREATED : response.SC_CONFLICT
		listQueue()
	}

	
	/**
	 * Cancel action from queueKit listing screen
	 * @return
	 */
	def cancel() {
		// id is the id of the queue entry
		boolean success=queueReportService.cancel(params.long('id'),params)
		response.status=success ? response.SC_CREATED : response.SC_CONFLICT
		listQueue()
	}

	/**
	 * Main queueKit listing deletAll button action
	 * @param bean
	 * @return
	 */
	def deleteAll(QueueKitBean bean) {
		bean.userId = queuekitUserService.currentuser
		boolean success
		if (QueuekitLists.deleteList.contains(bean.deleteBy)) {
			success = queueReportService.clearUserReports(bean,bean.deleteBy)
		}
		response.status=success ? response.SC_CREATED : response.SC_CONFLICT
		listQueue()
	}


	/**
	 * used by ArrayBlocking - don't think it is enabled
	 * @param bean
	 * @return
	 */
	def scheduleAll(QueueKitBean bean) {
		bean.userId = queuekitUserService.currentuser
		boolean success = arrayBlockingReportsQueueService.rescheduleAll(bean)
		response.status=success ? response.SC_CREATED : response.SC_CONFLICT
		listQueue()
	}
	
	
	/**
	 * 
	 * ADMIN / SUPERUSER RELATED TASKS 
	 * 
	 * 
	 * 
	 */
	
	
	
	def changePriority(ChangePriorityBean bean) {
		if (queuekitUserService.isSuperUser(queuekitUserService.currentuser)) {
			if (request.xhr && bean.queue) {
				bean.priority = bean.queue.priority ?: bean.queue.defaultPriority
				render template:'/queueKit/changePriority',model:[instance:bean]
				return
			}
		}
		render status:response.SC_NOT_FOUND
	}

	def modifyPriority(ChangePriorityBean bean) {
		if (queuekitUserService.isSuperUser(queuekitUserService.currentuser)) {
			if (bean.validate()) {
				queueReportService.changeQueuePriority(bean.queue, bean.priority)
				response.status=response.SC_CREATED
				return
			}
			response.status=response.SC_CONFLICT
			listQueue()
			return
		}
		response.status=response.SC_CONFLICT
	}

	def changeConfig(ChangeConfigBean bean) {
		if (queuekitUserService.isSuperUser(queuekitUserService.currentuser) && request.xhr) {
				bean.formatBean()
				render template:'/queueKit/changeConfig',model:[instance:bean]
				return
		}
		flash.message = bean?.errors?.allErrors.collect{message(error : it)}
		render status:response.SC_NOT_FOUND
		return
	}

	def modifyConfig(ChangeConfigBean bean) {
		if (queuekitUserService.isSuperUser(queuekitUserService.currentuser) &&bean.validate()) {
			queueReportService.modifyConfiguration(bean.queueType?:bean.queue,bean.changeValue, bean.changeType,bean.priority,bean.floodControl, bean.defaultComparator)
			response.status=response.SC_CREATED
			listQueue()
			return
		}
		flash.message = bean?.errors?.allErrors.collect{message(error : it)}
		response.status=response.SC_CONFLICT
	}
	
	def loadConfig(ChangeConfigBean bean) {
		if (queuekitUserService.isSuperUser(queuekitUserService.currentuser) &&bean.validate()) {
			render bean.loadConfig()
			return
		}
		response.status=response.SC_CONFLICT
	}

}
