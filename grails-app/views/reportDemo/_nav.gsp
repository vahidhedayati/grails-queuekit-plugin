<%@ page import="org.grails.plugin.queuekit.ReportsQueue" %>
<div class="nav" role="navigation">
		<ul>
			<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label" /></a></li>
			<li><g:link class="btn btn-default" controller="queueKit" action="listQueue"><g:message code="queuekit.reportDownload.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="downloadByBrowser"><g:message code="queuekit.tsvTestBefore.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="basicDemo"><g:message code="queuekit.paramsDemo.label"/></g:link></li>
			<li><g:link class="btn btn-default" controller="reportDemo" action="testNoResults"><g:message code="queuekit.noResults.label"/></g:link></li>

		</ul>
		<br/>
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
	</div>