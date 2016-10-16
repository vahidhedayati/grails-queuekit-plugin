queuekit {
	
	/*
	 * keepAliveTime in seconds
	 */
	keepAliveTime=300
	
	/*
	 * corePoolSize this should match maximumPoolSize
	 * 
	 */
	corePoolSize=3
	/*
	 * maxPoolSize
	 *
	 */
	maximumPoolSize=3
	
	/*
	 * Amount of elements that can queue
	 */
	maxQueue=100
	
	
	/*
	 * If you have 3 threads and there are 6 reports launched
	 * 
	 * If after (reportThreads - preserveThreads) =
	 * 3 - 1 = 2
	 * 
	 * After 2 threads all report priorities above or equal
	 * perservePriority Permission  = Priority.MEDIUM
	 * will be left in queued state.
	 * This means all those below Priority.MEDIUM will have a spare slot 
	 * to run within. In short an fast lane left open always for those 
	 * below medium and 2 slow lanes. You should be able to configure 6 report 
	 * Threads and 2 reserveThreads. Configure to suite your needs  
	 */
	preserveThreads = 1
	
	// Explained in preseverThreads
	preservePriority = org.grails.plugin.queuekit.priority.Priority.MEDIUM

	/*
	 * If you are on windows you need to set this to a path like 
	 * reportDownloadPath='c:\\\\temp'
	 * 
	 * By default it will try to put into /tmp
	 * which means it supports Linux file system by default
	 * 
	 */
	reportDownloadPath='/tmp' 
	
	/*
	 * only required and set to windows if on windows only if reportDownloadPath not provided
	 * To save hassle set correct reportDownloadPath and this can be then ignored
	 * osType='windows' 
	 */	
	osType='linux'	
	
	/*
	 * By default Cron schedule removes all requests older than 5 days old
	 */
	removalDay=5
	
	/*
	 *By default same schedule removes All downloaded reports older than 1 day 
	 */
	removalDownloadedDay=1
	
	/*
	 *  bufferedWriterTypes user BufferedWriter to output stream
	 *  typically csv, tsv files generated without requirement of
	 *  FileOutputStream which is the default if files do not match this
	 */
	bufferedWriterTypes=['TSV','CSV','tsv','csv']
	
	
	/*
	 *  Define the order preference when using PriorityExecutor method
	 *  by default it will all not defined are treated as LOW
	 */
	reportPriorities = [
			tsvExample1:org.grails.plugin.queuekit.priority.Priority.REALLYSLOW,
			csvExample1:org.grails.plugin.queuekit.priority.Priority.HIGHEST,
			xlsExample1:org.grails.plugin.queuekit.priority.Priority.REALLYSLOW,
			xlsExample2:org.grails.plugin.queuekit.priority.Priority.LOWEST,
			xlsExample3:org.grails.plugin.queuekit.priority.Priority.MEDIUM
	]
	
	/*
	 * Define the default report type to select if no reportType has been selected as part
	 * of buildReport calls - these are all the types:
	 * 
	 *  org.grails.plugin.queuekit.ReportsQueue.ENHANCEDPRIORITYBLOCKING
	 *  org.grails.plugin.queuekit.ReportsQueue.PRIORITYBLOCKING
	 *  org.grails.plugin.queuekit.ReportsQueue.LINKEDBLOCKING
	 *  org.grails.plugin.queuekit.ReportsQueue.ARRAYBLOCKING
	 */
	defaultReportsQueue=org.grails.plugin.queuekit.ReportsQueue.ENHANCEDPRIORITYBLOCKING
	
		
	/*
	 *  Configure colour display on duration field of listing
	 *  Configure as you require providing hours minutes seconds and color
	 *  various examples provided to ssho it can be either 1 or all cases
	 *  just repeat to cover all your cases
	 *  Will help identify slow reports quickly.
	 */
	durationThreshHold = [
		[hours: 1, minutes: 10, seconds: 2, color: 'blue'],
		[minutes: 1, seconds: 5, color: 'orange'],
		[minutes: 1, seconds: 9, color: '#FF86E3'],
		[minutes: 1, seconds: 10, color: '#C4ABFE'],
		[seconds: 1, color: '#9999CC'],
		[seconds: 12, color: '#FFFFAA'],
		[seconds: 10, color: '#E3E0FA'],
		[seconds: 25, color: '#52FF20'],
		[seconds: 20, color: '#F49AC2'],
		[seconds: 50, color: '#FF4848'],
		[seconds: 35, color: '#9999CC']
	]
	/*
	 * Enhanced Priority task killer
	 * Kill a running task that runs more than ?
	 * in seconds - which gives you a chance to
	 * give more accuracy than just minutes alone
	 * 60 = 1 minute
	 * 600 = 10 minutes
	 * 3600 = 1 hour
	 *
	 * By default this is 0 = off
	 *
	 */
	killLongRunningTasks=300
	
	
	/*
	 * defaultComparator will use out of the box 
	 * comparator method for either of PriorityBlocking
	 * or EnhancedPriorityBlocking
	 * 
	 * If enabled all of the rest of the experimental options below it won't kick in
	 * By default it is off
	 *  
	 */
	defaultComparator=false
	
	/*
	 * StandardComparable is ReportRunnable class
	 * This does not implement comparable and if enabled 
	 * you can still override a report priority thourgh overriding
	 * queuekitUserService and implementing an override at that point 
	 * By default it is off
	 *  
	 */
	standardRunnable=false
	
	/*
	 * If you have enabled standardRunnable and you want to override report comparison 
	 * to go through queueKitUserService (which you would have) extended
	 * and changed the methods to work with your reports. 
	 * 
	 * Then also enable this - this being enabled with standardRunnable being false
	 * makes no difference
	 * 
	 */
	disableUserServicePriorityCheck=false
	
	/*
	 * A backup executor is configured for 
	 * Linked/Priority/EnhancedPriority Blocking mechanisms
	 * If you shut down main executor or it runs into problems
	 * by enabling this config as true it will attempt to bypass
	 * the existing mechanisms and over to an emergency single
	 * executor. 
	 * 
	 * This keeps business flowing but application will need to be restarted 
	 * in order to trigger proper business flow of any of the above used methods
	 * 
	 * Each new request will automatically be executed and be running 10 reports = 10 new threads
	 * 
	 */
	useEmergencyExecutor=true
	
	/*
	 * This is the final step to ensure your end user will get their report regardless
	 * 
	 * The scenario would be:
	 *  
	 * 	---> The main executor (of choice) that is being used is down i.e. something seriously gone wrong
	 *  	 or that you have shut it down from the main menu
	 *  
	 *  ---> The above useEmergencyExecutor is set to false and or useEmergencyExecutor is set to true
	 *       and even that appears to be shutDown or something seriously wrong
	 *  
	 *  ---> If all of above has somehow happened then by setting manualDownloadEnabled = true
	 *  	 will run the report as it would have been doing through the usual process of how it
	 *  	would have run originally. 
	 *  
	 *  Like above it will be as it was originally 10 user requests = 10 separate threads.
	 *  You can still view these files in the report menu and will still be downloadable the same way.
	 *   
	 * 
	 * Refer to ReportDemoController main index action to see how you can make this all imitate the real thing
	 * 
	 */
	manualDownloadEnabled=true
	
	/*
	 * By default if not set this will be false
	 * If you wish to remove the actual database record
	 * when a user requests to delete report set this to true
	 *
	 */
	deleteEntryOnDelete=false
	
	

	
	/*
	 * This is fluidity control
	 * If no others are using the queueing mechanism
	 * If no tasks outstanding below defined priority
	 *
	 * Scenario:
	 * 9 high tasks with no one else using and no MEDIUM/LOW tasks
	 * are awaiting
	 *
	 * All 9 should use all 3 base slots at a time.
	 *
	 * If a medium task gets added then it will attempt to stop using all three
	 *
	 * i.e. hogging all channel with HIGH priority tasks only.
	 * 
	 * You have states 0 = off
	 * states 1 = only floodControl those above defined Priority group 
	 * so if MEDIUM then setting to 1 means all above MEDIUM i.e. HIGH HIGHEST
	 * will be above to use all channels
	 * 
	 * state 2 = do this for both HIGH and LOW jobs if no one is using system
	 * override emergency channel for usage for both
	 * 
	 * In which case why not just increase pool size ?
	 * (kind of overlapping)
	 *
	 */
	forceFloodControl=0
	
	
	/*
	 * limitUserBelowPriority & limitUserAbovePriority
	 * by default are both off. You must provide a numeric value
	 * 
	 * This will be used to hard limit any user by that many requests at any one time
	 * 
	 * limitUserBelowPriority = user concurrently sharing with others and having Jobs 
	 * running at the same time below MEDIUM (If medium) is default group.  So MEDIUM/LOW/LOWEST etc
	 * 
	 * 
	 * limitUserAbovePriority = user sharing jobs and running whilst other run SET VALUE of jobs above 
	 * MEDIUM (If medium) is default group.  So HIGH AND HIGHEST
	 * 
	 * This obviously falls into the laws of what is actually available in the first instance and 
	 * only if other users are actively using the queueing mechanism with the given user.
	 * 
	 * If you have a 5 reportThread limit and 1 above/below per user
	 * and two users run 6 low/medium jobs each user will get a 
	 * thread each out of the 5. So in theory shared threading between the user.
	 * 
	 * Meaning all 5 channels will still be used regardless of this. This is to set a hardlimit 
	 * as to how many per user so with 5 concurrent users this should be 1 each.
	 * (hoping)
	 * 
	 * 
	 */
	limitUserBelowPriority=0
	limitUserAbovePriority=0
	
	/*
	 * Configure this value if you have enabled in BootStrap:
	 *
	 * queuekitExecutorBaseService.rescheduleRequeue()
	 *
	 * This will then attempt to re-trigger all outstanding jobs upon an
	 * application restart.
	 *
	 * Running the above task without below enabled will simply set the
	 * status of any jobs of running back to queued. Making them ready to be
	 * processed. They were tasks that had been running whilst application was
	 * interrupted/stopped.
	 *
	 * You could also use the manual checkQueue method provided on the listing UI
	 *
	 */
	checkQueueOnStart=true
	
	
	/*
	 * DisableExamples basically disables the examples controller
	 * so when you have tested and don't wish to allow this controller to be available on your app
	 * then turn it off through this config set it to true. By default it is false
	 */
	disableExamples=false
}