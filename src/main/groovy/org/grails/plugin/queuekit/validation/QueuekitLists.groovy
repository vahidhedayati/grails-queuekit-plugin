package org.grails.plugin.queuekit.validation

import grails.util.Holders
import groovy.transform.CompileStatic
import org.grails.plugin.queuekit.ReportsQueue
import org.grails.plugin.queuekit.priority.Priority

/**
 * Created by Vahid Hedayati on 16/10/16.
 */
@CompileStatic
class QueuekitLists {
    static final String POOL='PO'
    static final String PRESERVE='PR'
    static final String CHECKQUEUE='CQ'
    static final String STOPEXECUTOR='ST'
    static final String FLOODCONTROL='FC'
    static final String LIMITUSERABOVE='LA'
    static final String LIMITUSERBELOW='LB'
    static final String DEFAULTCOMPARATOR='DC'
    static final String MAXQUEUE='MQ'
    static final List CHANGE_TYPES=[POOL,MAXQUEUE,PRESERVE,DEFAULTCOMPARATOR,FLOODCONTROL,LIMITUSERABOVE,LIMITUSERBELOW,CHECKQUEUE,STOPEXECUTOR]

    static final String DELALL='AL'
    static final def deleteList = ReportsQueue.REPORT_STATUS_ALL-[ReportsQueue.DELETED, ReportsQueue.RUNNING, ReportsQueue.OTHERUSERS]+[DELALL]


    static final String USER='US'
    static final String REPORTNAME='RN'
    static final List SEARCH_TYPES=[USER,REPORTNAME]
	
	/**
	 * Retrieve configuration value of reportPriorties
	 * this should be a list containing key value e.g: ReportName:Priority.HIGH
	 *
	 * Assign report priority back if not set to DEFAULT LOW
	 */
	static Priority sortPriority(String reportName) {
		Priority priority = Priority.LOW
		Priority configProp =getConfigPriority(reportName)
		if (configProp) {			
			priority = configProp 
		}
		return priority
	}
	static Priority getConfigPriority(String reportName) {
		return (getConfig('reportPriorities') as Map).find{k,v-> k==reportName}.value  as Priority 		
	}
	static def getConfig(String configProperty) {
		 Holders.grailsApplication.config.queuekit[configProperty] ?: ''
	}
}
