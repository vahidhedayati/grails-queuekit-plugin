<%@ page import="org.grails.plugin.queuekit.ReportsQueue; org.grails.plugin.queuekit.priority.Priority" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'example.label', default: 'example')}" />
		<title><g:message code="default.admin.menu.label" args="[entityName]" default="Welcome to ${entityName}" /></title>
		<asset:javascript src="bootstrap.js" />
		<asset:stylesheet href="bootstrap.css" />
		<style type="text/css">
		html,body {
			max-width: 100% !important;
		}
		</style>
	</head>
	<body>
		<g:render template="nav"/>
	<g:if test="${flash.message}">
		<div class="message" role="status">${flash.message}</div>
	</g:if>
	
<div class="container">
		<div class="content">
			<div class="form-inline alert alert-warning">
			<h4>Welcome to the test form page, simply change: with EnhancedPriorityBlocking / PriorityBlocking</h4>
			<h5>userId to another userId to make report be created as them</h5>
			<h5>reportName try name of test1 or test2 compared to other names - fileName should change</h5>
			
			<h5>Then try changing Date range within 1 day = HIGHEST 1 WEEK = HIGH MORE Than 1 WEEK = MEDIUM ...it changes according to date range </h5>
			<h6>Obviously if no jobs are running priority will not change, but if other jobs have got the running tasks all covered. 
			Then it will end up in the queue. If it ends up in the queue then priority will change accordingly. 
			It would make no difference to change the priority if it can run :)</h6>
			
			<g:form action="customName">	
				<label>UserId : </label>
					<g:textField name="userId" class="form-control" value="${bean.userId}"/>
				<label>ReportName : </label>
					<g:textField name="report" class="form-control" value="${bean.report }"/>
				<label>Country: </label>
					<g:select name="countrySelected" class="form-control" from="${bean.countries}" optionKey="name" optionValue="value"/>
				<label>QueueType: </label>
					<g:select name="reportType" class="form-control" from="${ReportsQueue.REPORT_TYPES}" valueMessagePrefix="queuekit.queueType"/>
				<label>Priority: </label>
					<g:select name="priority" class="form-control" from="${Priority.values()}" />
				<label>fromDate: </label>
					<g:textField name="fromDate"  class="form-control" value="${bean?.fromDate}" />
					 
				<label>toDate: </label>
					<g:textField name="toDate" class="form-control" value="${bean?.toDate}" />
					
				<g:submitButton name="submit" value="${g.message(code:'submit.label') }" class="btn btn-danger"/>
			</g:form>					
		</div>
		<g:render template="navExamples"/>
	</div>
</div>
		<br/>
			<g:render template="howto"/>
</body>
</html>