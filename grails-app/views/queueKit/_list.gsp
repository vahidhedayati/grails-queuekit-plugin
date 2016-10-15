<%@ page import="org.grails.plugin.queuekit.ReportsQueue" %>
<g:set var="entityName" value="${message(code: 'queuekit.reportDownload.label')}" scope="request" />

<g:render template="/queueKit/search" model="${[search:instanceList.search,superUser:superUser]}"/>

<g:set var="dateFormat" value="${message(code: 'queuekit.reportDate.format')}"/>
<div class="content" role="main" id="results">	
<g:if test="${instanceList.reportJobs}">
<section class="jobList container-fluid" id="section2" style="display: none;">
			<g:each in="${instanceList.reportJobs}" var="reportJob">
			<div class="col-sm-12 well">
				<g:message code="reportThreadLimit${reportJob.isAdvanced?'Advanced':''}.${reportJob.queueType}${reportJob.minPreserve?'':'.label'}" 
				args="${[reportJob.maxPoolSize,reportJob.running,reportJob.queued,reportJob.minPreserve,reportJob.priority,reportJob.limitUserBelowPriority,reportJob.limitUserAbovePriority,reportJob.forceFloodControl]}"/>								
				<g:if test="${reportJob.executorCount}">
					<div class="alert alert-success">
						<g:message code="userThreadbreakDown.label" args="${[reportJob.executorCount.userRunningBelow,reportJob.executorCount.userRunningAbove,
							reportJob.executorCount.userBelow,reportJob.executorCount.userAbove,reportJob.priority]}"/>
					</div>
					<div class="alert alert-warning">	
						<g:message code="reportThreadbreakDown.label" args="${[reportJob.executorCount.runningBelow,reportJob.executorCount.runningAbove,
							reportJob.executorCount.queuedBelow,reportJob.executorCount.queuedAbove,reportJob.priority]}"/>
					</div>
				</g:if>
			</div>
			<br/>
			</g:each>
	
	</section>
</g:if>
<div id="message" class="message" role="status" 
	<g:unless test="${flash.message }"> style="display:none" </g:unless>
>${flash.message }</div>
<g:if test="${instanceList.results}">

<table class="table table-list-search">
		<thead>
			<tr>
				<g:sortableColumn property="reportName" titleKey="queuekit.reportName.label" params="${search}" />
				<g:sortableColumn property="created" titleKey="queuekit.created.label" params="${search}" />
				<g:sortableColumn property="startDate" titleKey="queuekit.startDate.label" params="${search}" />
				<g:sortableColumn property="finishDate" titleKey="queuekit.finishDate.label" params="${search}" />
				<g:sortableColumn property="duration" titleKey="queuekit.duration.label" params="${search}" />
				<g:sortableColumn property="userId" titleKey="queuekit.username.label" params="${search}" />
				<g:sortableColumn property="queueType" titleKey="queuekit.queueType.label" params="${search}" />
				<g:sortableColumn property="priority" titleKey="queuekit.reportPriority.label" params="${search}" />
				<g:sortableColumn property="status" titleKey="queuekit.status.label" params="${search}" />
				<th>&nbsp;</th>
			</tr>
		</thead>
		<tbody>
			<g:set var="moreLabel" value="${message(code: 'queuekit.moreoptions.label')}"/>
			<g:set var="downloadLabel" value="${message(code: 'queuekit.download.label')}"/>
			<g:set var="queuedLabel" value="${message(code: 'queuekit.reportType.QU')}"/>
			<g:set var="deleteLabel" value="${message(code: 'queuekit.delete.label')}"/>
			<g:set var="cancelLabel" value="${message(code: 'queuekit.download.label')}"/>
			<g:set var="requeueLabel" value="${message(code: 'queuekit.requeue.label')}"/>
			<g:each in="${instanceList.results}" status="i" var="reportInstance">
				<tr class="${(i % 2) == 0 ? 'even' : 'odd'}">
					<td>${reportInstance.reportName}</td>
					<td><g:formatDate format="${dateFormat}" date="${reportInstance.created}"/></td>
					<td><g:formatDate format="${dateFormat}" date="${reportInstance.startDate}"/></td>
					<td><g:formatDate format="${dateFormat}" date="${reportInstance.finishDate}"/></td>
					<td style="background: ${reportInstance.color?:'transparent'}">${reportInstance.duration?:''}</td>
					<td>${reportInstance.username?:reportInstance.userId}</td>
					<td><g:message code="queuekit.queueType.${reportInstance.queueType}"/></td>
					<td>${reportInstance?.priority?:''}</td>
					<td><g:message code="queuekit.reportType.${reportInstance.status}"/></td>
					<td class="dropdown queuekit">
					<g:if test="${reportInstance.status== ReportsQueue.COMPLETED}">
						<g:link action="download" class="btn btn-default" id="${reportInstance.id}" onclick="setTimeout(function () {reloadPage()}, 100);">${downloadLabel}</g:link>
					</g:if>
					<g:elseif test="${reportInstance.status==ReportsQueue.ERROR||reportInstance.status==ReportsQueue.DOWNLOADED}">
						<g:link action="delRecord" class="btn btn-default"  id="${reportInstance.id}" onclick="setTimeout(function () {reloadPage()}, 100);">${deleteLabel}</g:link>
					</g:elseif>
					<g:else>
					<i  class="btn btn-default" id="${reportInstance.id}">
						<g:message code="queuekit.reportType.${reportInstance.status}"/>
					</i>
					</g:else>					
					<a  class="btn btn-default actionButton" data-toggle="dropdown"
					  data-row-id="${reportInstance.id}" data-queueType="${reportInstance.queueType}" data-row-status="${reportInstance.status}">
					   <span class="arrow-down"></span>						  
					</a>					  
					</td>
				</tr>
			</g:each>
		</tbody>
	</table>
	
		<ul id="contextMenu" class="dropdown-menu" role="menu" >
		<li id="downloadAgain"><a>${downloadLabel}</a></li>
		<li id="queueDisplay"><a><g:message code="queuekit.display.label"/></a></li>
		<li id="queueRequeue"><a>${requeueLabel}</a></li>					
		<li id="queueDelete"> <a>${deleteLabel}</a></li>
		
		<g:if test="${superUser}">
		<li id="qeuePriority"><a><g:message code="queuekit.changePriority.label"/></a></li>			
		<li class="divider"></li>
		<li id="increasePool"><a><g:message code="queuekit.adminButton.PO"/></a></li>
		<li id="increasePreserve"><a><g:message code="queuekit.adminButton.PR"/></a></li>
		</g:if>
		</ul>
	
	<div class="center-block pagination">
		<span class="listTotal"><g:message code="queuekit.count.message" args="${[instanceTotal]}"/></span>
		<g:paginate total="${instanceTotal}" params="${search}" />
	</div>

	<script>
	$(function() {
		toggleBlock('#jobCtrl','.jobList');		
		var showJobControl="${search.jobControl}";
		if (showJobControl=='true') {
			var message="<g:message code="queuekit.jobControl.label" args="${[g.message(code:'queuekit.hide.label')]}"/>";
 			$('#jobCtrl').html(message).fadeIn('slow');
 			$('.jobList').toggle();
		}
		function toggleBlock(caller,called,calltext) {
			$(caller).click(function() {
				if($(called).is(":hidden")) {
					var message="<g:message code="queuekit.jobControl.label" args="${[g.message(code:'queuekit.hide.label')]}"/>";
		 			$(caller).html(message).fadeIn('slow');
		 			$('#jobControl').val(true);
		    	}else{
		    		var message="<g:message code="queuekit.jobControl.label" args="${[g.message(code:'queuekit.show.label')]}"/>";			    	
		        	$(caller).html(message).fadeIn('slow');	
		        	$('#jobControl').val(false);
		    	}
		 		$(called).slideToggle("slow");
		  	});
		  }
		$dropdown = $("#contextMenu");
		$(".actionButton").click(function() {
			var id = $(this).attr('data-row-id');
			var status = $(this).attr('data-row-status');
			var queuetype = $(this).attr('data-queuetype');
			$(this).after($dropdown);	
			$dropdown.find("#queueDelete").attr("onclick", "javascript:doDelete("+id+");");
			$dropdown.find("#downloadAgain").attr("onclick", "javascript:doDownload("+id+");");
			$dropdown.find("#queueDisplay").attr("onclick", "javascript:doDisplay('"+id+"');");	
			$dropdown.find("#queueRequeue").attr("onclick", "javascript:doRequeue('"+id+"');");
			$dropdown.find("#qeuePriority").attr("onclick", "javascript:doPriority('"+id+"');");
			$dropdown.find("#increasePool").attr("onclick", "javascript:doConfigChange('"+id+"','PO');");
			$dropdown.find("#increasePreserve").attr("onclick", "javascript:doConfigChange('"+id+"','PR');");				
			$(this).dropdown();				
			var running= status=='${ReportsQueue.RUNNING}';
			var deleted= status=='${ReportsQueue.DELETED}';
			var queued= status=='${ReportsQueue.QUEUED}';
			var downloaded= status=='${ReportsQueue.DOWNLOADED}';
			var enhancedQueue = queuetype=='${ReportsQueue.ENHANCEDPRIORITYBLOCKING}';
			var priorityQueue = queuetype=='${ReportsQueue.PRIORITYBLOCKING}';
			$('#queueDelete')[(!deleted && enhancedQueue)||(!enhancedQueue && !running) ?'show':'hide']();
			$('#qeuePriority')[(queued && enhancedQueue) ?'show':'hide']();
			var arrayQueue = queuetype=='${ReportsQueue.ARRAYBLOCKING}';
			var issue = (status=='${ReportsQueue.QUEUED}' || status=='${ReportsQueue.ERROR}');
			$('#queueRequeue')[arrayQueue && issue ?'show':'hide']();
			$('#increasePreserve')[(enhancedQueue||priorityQueue) ?'show':'hide']();
			$('#increasePool')['show']();
			$('#downloadAgain')[downloaded ? 'show':'hide']();
										
		});
	});
	</script>
</g:if>
</div>