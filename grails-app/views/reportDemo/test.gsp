<%@ page import="org.grails.plugin.queuekit.ReportsQueue; org.grails.plugin.queuekit.priority.Priority" %>
<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'example.label', default: 'example')}" />
		<title><g:message code="queuekit.menu.label" args="[entityName]" default="Welcome to ${entityName}" /></title>
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

			<div class="container" >
				<div class="form-inline">
					
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
								<g:textField name="fromDate"  class="form-control" value="${bean.fromDate}"/>
							<label>toDate: </label>
								<g:textField name="toDate" class="form-control" value="${bean.toDate}"/>
							<g:submitButton name="submit" value="${g.message(code:'submit.label') }" class="btn btn-danger"/>
						</g:form>
					
				</div>
			</div>
</body>
</html>