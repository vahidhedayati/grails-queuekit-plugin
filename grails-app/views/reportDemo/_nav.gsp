<style type="text/css">
html,body {
	max-width: 100% !important;
}
.codebox {
  		border:1px solid black;
  		background-color:#090909;
  		width: 60em;
  		overflow:auto;    
  		padding:20px;
  		margin-left:10px;
  		
}
.codebox code {
	font-family: Arial, Helvetica, sans-serif;
  		font-size:1em;
  		white-space: pre;
  		background-color:transparent;
  		color:#FFF;
  
}
.alert {
	min-width: 20em;
	max-width: 61em;
	color:#000;
	font-weight:bold;
}
</style>
<div class="nav" role="navigation">
	<ul>
		<li><a class="home" href="${createLink(uri: '/')}"><g:message code="default.home.label" /></a></li>
		<li><g:link class="btn btn-default" controller="queueKit" action="listQueue"><g:message code="queuekit.reportDownload.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="reportDemo" action="downloadByBrowser"><g:message code="queuekit.tsvTestBefore.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="reportDemo" action="basicDemo"><g:message code="queuekit.paramsDemo.label"/></g:link></li>
		<li><g:link class="btn btn-default" controller="reportDemo" action="testNoResults"><g:message code="queuekit.noResults.label"/></g:link></li>							
	</ul>
</div>
