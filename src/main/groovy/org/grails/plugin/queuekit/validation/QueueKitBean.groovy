package org.grails.plugin.queuekit.validation

import grails.util.Holders
import grails.validation.Validateable

import org.grails.plugin.queuekit.ReportsQueue


class QueueKitBean implements Validateable {

	def queuekitUserService = Holders.grailsApplication.mainContext.getBean('queuekitUserService')

	static final String USER='US'
	static final String REPORTNAME='RN'
	static final List SEARCH_TYPES=[USER,REPORTNAME]

	static final String DELALL='AL'
	static final List deleteList = ReportsQueue.REPORT_STATUS_ALL-[ReportsQueue.DELETED,ReportsQueue.RUNNING]+[DELALL]

	Long userId

	String searchBy
	String searchFor
	Long userSearchId

	String status

	Integer max=Math.min(10, 50)
	Integer offset
	String sort
	String position
	String order='desc'

	String deleteBy
	boolean safeDel=false

	boolean superUser=false
	boolean jobControl=false

	static constraints = {
		status (inList:ReportsQueue.REPORT_STATUS_ALL)
		userSearchId(nullable:true)
		searchFor(nullable:true)
		searchBy(inList:SEARCH_TYPES)
		deleteBy(nullable:true, inList:deleteList)
	}

	/**
	 * Work out if userId is superUser
	 * Rather expensive but paying the price
	 * for userBase separation
	 * @return
	 */
	boolean getSuperUser() {
		return queuekitUserService.isSuperUser(userId)
	}

	/**
	 * returns correct search select listing 
	 * based on privileges
	 * @return
	 */
	List getSearchList() {
		return (getSuperUser() ? SEARCH_TYPES: SEARCH_TYPES-[USER])
	}

	/**
	 * return correct status based on user privileges
	 * @return
	 */
	List getStatuses() {
		return (getSuperUser() ? ReportsQueue.REPORT_STATUS_ALL : ReportsQueue.REPORT_STATUS)
	}
	
	List getAdminButtons() {
		return (getSuperUser() ? ChangeConfigBean.CHANGE_TYPES : [])
	}
	/**
	 * When the user searches for a username on the front end listing
	 * Since this plugin has been designed without the awareness of how you run your userbase.
	 * It calls on queuekitUserService.getRealUserId(searchBy) which will return a userId bound
	 * to search username. 
	 * 
	 * At the moment provided service returns 1 the same as currentuser which is also 1
	 * 
	 * The only thing you need to override is queuekitUserService and make your own then using 
	 * resources.groovy bind it back to be actually queuekitUserService so plugin uses your version instead
	 * to return a real userid
	 * 
	 * @return
	 */
	Long getUserSearchId() {
		if (searchFor==USER && superUser) {
			return queuekitUserService.getRealUserId(searchBy)
		}
		return null
	}
	
	/**
	 * Search map used by search page
	 * @return
	 */
	Map getSearch() {
		def search=[
			searchBy:searchBy,
			searchFor:searchFor,
			userSearchId:userSearchId,
			status:status,
			sort:sort,
			offset:offset,
			position:position,
			order:order,
			jobControl:jobControl,
			max:max
		]
		return search
	}
	
	/**
	 * form order
	 * @param o
	 */
	void setOrder(String o) {
		order = o in ['asc', 'desc'] ? o :''
	}

	/**
	 * form max  
	 * @param o
	 */
	void setMax(Integer o) {
		max= Math.min(o ? o : 10, 50)
	}

}