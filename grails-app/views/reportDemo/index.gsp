<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main">
		<g:set var="entityName" value="${message(code: 'example.label', default: 'example')}" />
		<title><g:message code="default.admin.menu.label" args="[entityName]" default="Welcome to ${entityName}" /></title>
		<asset:javascript src="bootstrap.js" />
		<asset:stylesheet href="bootstrap.css" />
	
	</head>
	<body>
	<g:render template="nav"/>	
		<g:if test="${flash.message}">
			<div class="message" role="status">${flash.message}</div>
		</g:if>
		<div class="container">
		<div class="content">
		<g:render template="navExamples"/>
		</div>
		</div>	
		<g:render template="howto"/>
		</body>
		</html>
			