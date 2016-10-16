package org.grails.plugin.queuekit.validation

import groovy.transform.CompileStatic
import org.grails.plugin.queuekit.ReportsQueue

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
}
