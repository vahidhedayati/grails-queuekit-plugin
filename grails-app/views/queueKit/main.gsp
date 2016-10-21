<%@ page import="org.grails.plugin.queuekit.validation.ChangeConfigBean;org.grails.plugin.queuekit.ReportsQueue;org.grails.plugin.queuekit.validation.QueueKitBean" %>
<!doctype html>
<html>
	<head>
		<meta name="layout" content="main" />
		<g:set var="entityName" value="${message(code: 'queuekit.reportDownload.label')}" scope="request" />
		<title><g:message code="queuekit.list.label" args="[entityName]" /></title>
		<style type="text/css">
		html,body {
			max-width: 100% !important;
		}
		.circle:before {
    		content: ' \25CF';
    		font-size: 10px;
		}
		.arrow-down {
			width: 0; 
			height: 0; 
			border-left: 5px solid transparent;
			border-right: 5px solid transparent;
			border-top: 5px solid #FF0000;
			font-size: 0;
			line-height: 0;
		}
		section {
			padding-top:5px;  
    		background-color: #fffbf8;
			-webkit-box-sizing:border-box;
			-moz-box-sizing:border-box;
			box-sizing:border-box;
			border-radius:6px;
			-webkit-box-shadow:
				0 2px 4px 0 rgba(72, 72, 72, 0.83),
				0 10px 15px 0 rgba(126, 126, 126, 0.12),
				0 -2px 6px 1px rgba(199, 199, 199, 0.55) inset,
				0 2px 4px 2px rgba(255, 255, 255, 0.83) inset;
			-moz-box-shadow:
				0 2px 4px 0 rgba(72, 72, 72, 0.83),
				0 10px 15px 0 rgba(126, 126, 126, 0.12),
				0 -2px 6px 1px rgba(199, 199, 199, 0.55) inset,
				0 2px 4px 2px rgba(255, 255, 255, 0.83) inset;
			box-shadow:
				0 2px 4px 0 rgba(72, 72, 72, 0.83),
				0 10px 15px 0 rgba(126, 126, 126, 0.12),
				0 -2px 6px 1px rgba(199, 199, 199, 0.55) inset,
				0 2px 4px 2px rgba(255, 255, 255, 0.83) inset;
		}
		#section1 {
			background-color: rgba(0, 0, 0, 0.67);
    		color:#38afff;
    		border-style: none;	  
		}
		.small { 
			max-width: 10em; 
		}
		.small:focus {
  		 	max-width: 25em;
		}
		#section2 {
    		color:#FFF;
    		font-weight:bold;
    		top: 0px;
    		margin-top:-5px;    		
		}
		#search a {
			margin-top: 3px;
		}
		#search a:hover{
			text-decoration: none;
			background-color: #24a8f6;
		}
		#search select,#search select option {     
 			color:white;
 			font-weight:bold;
    		background-color:#24a8f6; 
    	}
    	#search :required{
    		background: #fff3f3;
    		border-color: #ffaaaa;
    		color: #cc0000;
		}
    	
		.nav a.homeButton,.jobButton,.submitButton{			
			background-color:transparent;
			-webkit-background-size:20px 20px;
			background-size:20px 20px;
			border:none;
			cursor:pointer;
			color:#fff;
			padding-top:5px;
		}
		.jobButton {
			padding-top:0px;
			margin-left:-8px;
		}
		.submitButton:hover {
			background: #24a8f6;
			padding-top:2px;
			padding-bottom:2px;
			border-radius: 5px;
			box-shadow: inset 3px 3px 10px 0 rgba(0, 0, 0, 0.67);
    		-moz-box-shadow:    inset 0 0 5px rgba(0, 0, 0, 0.67);
    		-webkit-box-shadow: inset 0 0 5px rgba(0, 0, 0, 0.67);
    		box-shadow:         inset 0 0 5px rgba(0, 0, 0, 0.67);			
		}
		.submitButton {
			text-indent: 25px;
			background-image:url(../assets/skin/database_table.png);
			background-position: 0.7em center;
			background-repeat: no-repeat;
			margin-top:5px;
			padding-top:2px;
			padding-bottom:2px;
		}
		.navbar-trans {
			background-color:transparent;
    		color:#fefefe;    		
		}
		.nav {
			box-shadow:none;
		}
		.navbar-trans a{
    		color:#fefefe;
		}
		.alert {
			min-width: 20em;
			max-width: 45em;
    		margin: -3px 0 0 2px;
    		padding: 0 0 0 2px;
    		background:transparent;
    		border:none;
    		box-shadow:none;
		}
		.alert-success {
			color: #4cff00;
		}
		.alert-warning {
			color: #fff200;
		}
		.well {
			padding: 2px 2px 2px 3px;
			margin: 0 0 5px 0px;
			background-color: #168ccc;
		}
		

</style>
</head>
<body>
	<div id="results">
		<g:render template="list" />
	</div>	
	
	<div id="modalcontainer" style="display:none;">
		<g:render template="/modalDialog"/>
	</div>

<script>
	function reloadPage() {
		var url="${createLink(controller: 'queueKit',action:'listQueue')}";
		$.ajax({
			cache:false,			
			timeout:1000,
			type: 'POST',
			url: url,
			data: $('#search').serialize(),
			success: function(data){
				$('#results').html(data);
			}
		});
	}
	function doDelete(id) {
		if (confirm('${message(code: 'queuekit.deleteWarning.message')}')) {
		 	ajaxCall('D',id)
		}
	}
	function doSafeDelete(id) {		
		ajaxCall('S',id)		
	}
	function doRequeue(id) {
		 ajaxCall('R',id)
	}
	function doDownload(id) {
		window.location.href="${createLink(action: "download", absolute: true)}/"+id;
	}
	
	function doDisplay(id,queueType) {
		var params=$.param({id:id,queueType:queueType});
		var title = "${g.message(code: 'queuekit.showRecord.message')}";
		return showDialog('${createLink(controller:'queueKit',action:'display')}?'+params,title);
	}
	function doPriority(id) {
		var params=$.param({'queue.id':id});
		var title = "${g.message(code: 'queuekit.changePriority.label')}";
		return showDialog('${createLink(controller:'queueKit',action:'changePriority')}?'+params,title);
	}
	function doConfigChange(id,type) {
		var params=$.param({'queue.id':id, changeType:type});
		var title = "${g.message(code: 'queuekit.changeConfig.label')}";
		return showDialog('${createLink(controller:'queueKit',action:'changeConfig')}?'+params,title);
	}
	
	function showDialog(url,title) {
		$('#modalCase').modal('show');
		$.get(url,function(data){
			$('#modalContent').hide().html(data).fadeIn('slow');
			$('#modalTitle').html(title);
		});
		$("#modalcontainer").show()
	}
	function ajaxCall(method,id) {
		var url
		if (method=='D') {
			url="${createLink(action: "delRecord")}/"+id;
		} else if (method == 'S') {
			url="${createLink(action: "delRecord")}/?safeDel=${true}&id="+id;	
		} else if (method == 'R') {
			url="${createLink(action: "requeue")}/"+id;
		}
		$.ajax({
			type: 'POST',
			url: url,
			data: $('#search').serialize(),
			success: function(data){
				$('#results').html(data);
			}
		});
	}
	function postAction(method) {
		var url="${createLink(controller: 'queueKit',action:'deleteAll')}/?deleteBy="+method;			
		$.ajax({
			type: 'POST',
			url: url,
			data: $('#search').serialize(),
			success: function(data){
				$('#results').html(data);
			}
		});
	}
</script>
</body>
</html>