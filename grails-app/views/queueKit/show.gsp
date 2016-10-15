<!doctype html>
<html>
	<head>
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'queuekit.reportDownload.label')}" scope="request" />
		<title><g:message code="default.list.label" args="[entityName]" /></title>
	</head>
	<body>
	<div class="nav" role="navigation">
		<ul>
			<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label" /></a></li>
		</ul>
	</div>
		
		<div id="reportResults">
			<g:render template="showContent" />
		</div>

	</body>
</html>