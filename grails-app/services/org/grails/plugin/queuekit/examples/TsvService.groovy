package org.grails.plugin.queuekit.examples

import java.rmi.server.UID
import java.security.SecureRandom

import org.grails.plugin.queuekit.ReportsQueue
/**
 * Demonstration class used to show how plugin works 
 * @author Vahid Hedayati
 * @created 25 Sept 2016
 */
class TsvService {

	static prng = new SecureRandom()
	Random random = new Random()


	def runNoReport(Report2Bean bean) {
		return []
	}

	def runParams(params) {
		return getResults()
	}

	def runReport(Report1Bean bean) {
		return getResults()
	}

	/*
	 * This gets run by XLExample1ReportingService
	 */
	def runReport2(Report2Bean bean) {
		return getResults(true)
	}


	/*
	 * This gets run by XLExample3ReportingService
	 * and CsvExampleReporingService in this plugin
	 * In both cases report output will be much slower
	 * to show priorities working
	 * 
	 */
	def runReport3(Report3Bean bean) {
		return getResults(true)
	}


	/*
	 *  This would be you doing a real query and returning
	 *  a list containing all your elements.
	 *  For this test a simple list containing fields: id, text.
	 *  Slowed down in XLS cases to show priorityExecutor work
	 *   
	 */

	private List getResults(boolean doSleep=false) {
		int i=0
		def results = []
		try {
			while (i < 4500 ) {
				results << [ id: i, text: new UID().toString() + prng.nextLong() +System.currentTimeMillis() ]
				//sleep(random.nextInt(doSleep ? ((i % 2) == 0 ? 40 : 10) : ((i % 2) == 0 ? 20 : 5)))
				sleep(random.nextInt(doSleep ? ((i % 2) == 0 ? 60 : 15) : ((i % 2) == 0 ? 17 : 9)))
				log.info "working on ${i} "
				i++
			}
		}catch (e) {
			log.error "${e.getClass()} ${e.message}"
		}
		return results
	}


	def runReport4(Report2Bean bean) {

		String query="""select new map(rq.id as id, rq.retries as retries, rq.paramsMap as paramsMap,
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
	from ReportsQueue rq
"""
		def inputParams=[:]
		List resultsList
		try {
			int cowsComeHome=100
			while (cowsComeHome > 0) {

				resultsList = ReportsQueue.executeQuery(query,inputParams,[readOnly:true,timeout:15])
				sleep(cowsComeHome*10)
				cowsComeHome--
				// would prefer real data to crunch rather than some loop
			}
		}catch (e) {
			//
		}
		return resultsList
	}

}
