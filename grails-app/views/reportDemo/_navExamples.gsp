<%@ page import="org.grails.plugin.queuekit.ReportsQueue" %>

<div class="alert alert-danger" >

These are various examples when clicked will trigger a report of the defined priority/queue type.
If you do click reports, a message will flash to say job has been queued. Check <g:link controller="queueKit" action="listQueue"><g:message code="queuekit.reportDownload.label"/></g:link> to see how the report is getting on

 <div class="nav" role="navigation">
</ul>
	<ul>
			<li><g:link class="btn btn-default" controller="reportDemo" action="index2" params="${[reportType:ReportsQueue.ENHANCEDPRIORITYBLOCKING]}"><g:message code="queuekit.enhancedPrioritytsvTestXls.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="index7" params="${[reportType:ReportsQueue.ENHANCEDPRIORITYBLOCKING]}"><g:message code="queuekit.enhancedDirectPriority.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="index1" params="${[reportType:ReportsQueue.ENHANCEDPRIORITYBLOCKING]}"><g:message code="queuekit.enhancedPrioritysubmitTsv.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="index1" params="${[reportType:ReportsQueue.ENHANCEDPRIORITYBLOCKING, priority:'default']}"><g:message code="queuekit.enhancedPriorityAsPerConfig.label"/></g:link></li>			
			</ul>
		<ul>
		<br/>
		<ul>

			<li><g:link class="btn btn-default" controller="reportDemo" action="index2" params="${[reportType:ReportsQueue.PRIORITYBLOCKING]}"><g:message code="queuekit.prioritytsvTestXls.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="index7" params="${[reportType:ReportsQueue.PRIORITYBLOCKING]}"><g:message code="queuekit.directPriority.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="index1" params="${[reportType:ReportsQueue.PRIORITYBLOCKING]}"><g:message code="queuekit.prioritysubmitTsv.label"/></g:link></li>			
		</ul>
		<br/>
		<ul>
						
			<li><g:link class="btn btn-default" controller="reportDemo" action="index3"><g:message code="queuekit.linkedtsvTestTsv.label"/></g:link></li>			
			<li><g:link class="btn btn-default" controller="reportDemo" action="index4"><g:message code="queuekit.linkedxlsTestTsv.label"/></g:link></li>
		</ul>
		<br/>
		<ul>
			
			<li><g:link class="btn btn-default" controller="reportDemo" action="index5"><g:message code="queuekit.arrayBlockingtsvTestTsv.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="index6"><g:message code="queuekit.arrayBlockingxlsTestTsv.label"/></g:link></li>					
		</ul>
		<br/>
		<ul>
		<li><g:link class="btn btn-default" controller="reportDemo" action="index8"><g:message code="queuekit.customReportName.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="reportDemo" action="indexBean" params="${[reportType:ReportsQueue.ENHANCEDPRIORITYBLOCKING]}"><g:message code="queuekit.beanReportCall.label"/></g:link></li>
		</ul>
		<br/>
		<ul>
		<li><g:link class="btn btn-default" controller="reportDemo" action="index9" params="${[reportType:ReportsQueue.PRIORITYBLOCKING]}"><g:message code="queuekit.hqlTestPriority.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="reportDemo" action="index9" params="${[reportType:ReportsQueue.ENHANCEDPRIORITYBLOCKING]}"><g:message code="queuekit.hqlTestEnhanced.label"/></g:link></li>
		</ul>
	</ul>
</div>
</div>	