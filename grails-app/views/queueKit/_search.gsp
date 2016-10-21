<%@ page import="org.grails.plugin.queuekit.validation.QueuekitLists;org.grails.plugin.queuekit.validation.ChangeConfigBean;org.grails.plugin.queuekit.ReportsQueue;org.grails.plugin.queuekit.validation.QueueKitBean" %>
<g:form name='search' controller='queueKit' class="navbar-trans" action='listQueue' method='post' >
	<section class="container-fluid" id="section1">
		<div id="reportResults" class="form-inline nav navbar-trans col-sm-6">			
		<ul>
			<li><a class="home homeButton" href="${createLink(uri: '/')}"><g:message code="default.home.label" /></a></li>
			<li><g:select class="form-control small" name="deleteOption" from="${deleteList}"
						  noSelection="${['':"${g.message(code:'queuekit.chooseDeleteType.label')}"]}"
						  valueMessagePrefix="queuekit.deleteType"/>
			</li>
			<g:if test="${superUser}">
				<li><g:select class="form-control small" name="adminButtons" from="${adminButtons}"
							  noSelection="${['':"${g.message(code:'queuekit.chooseAdminAction.label')}"]}"
							  valueMessagePrefix="queuekit.adminButton"/></li>
			</g:if>
			<g:if test="${superUser||instanceList.reportJobs}">
			<li>
				<g:if test="${superUser}">
					<g:checkBox name="hideUsers" value="${search.hideUsers}"/> <g:message code="queuekit.hideOtherUsers.label"/>
				</g:if>
				<g:if test="${instanceList.reportJobs}">
					<a class="jobButton" id="jobCtrl"><g:message code="queuekit.jobControl.label" args="${[g.message(code:'queuekit.show.label')]}"/></a>
				</g:if>
			</li>
			</g:if>
		</ul>
	</div>	
	<div class="form-inline nav navbar-trans col-sm-6">
		<ul>
			<li>
			<g:textField name="searchFor" class="form-control small" size="20" maxlength="50"
			value="${search?.searchFor}" placeholder="${g.message(code:'queuekit.searchFor.label')}" />
			<g:hiddenField name="userSearchId" value="${search?.userSearchId}" />
		</li>
		<li>
		<g:select name="searchBy" class="form-control small" noSelection="${['':"${g.message(code:'queuekit.searchBy.label')}"]}"
		from="${searchList}"  value="${search?.searchBy}" valueMessagePrefix="queuekit.searchType" />
		</li>
		<li>
		<g:select name="status" class="form-control small" from="${statuses}" noSelection="${['':"${g.message(code:'queuekit.chooseStatus.label')}"]}"
		value="${search?.status}" onChange="reloadPage();" valueMessagePrefix="queuekit.reportType" />
		</li>
		<li>
			<g:hiddenField name="jobControl" value="${search?.jobControl}"/>
			<button type="submit" class="submitButton" name="submit" >${message(code: 'queuekit.search.label')}</button>	
		</li>
		</ul>
	</div>

	</section>
	
 	<g:hiddenField name="sort" value="${search?.sort}"/>
 	<g:hiddenField name="order" value="${search?.order}"/>
</g:form>
<script>
$(function() {
	$('#searchFor').on('change', function() {		
		var value = $('#searchFor').val();
		var searchBy=$('#searchBy').val();
		if (value!='' && searchBy=='') {
			$('#searchBy').attr('required',true);
		} else {
			$('#searchBy').attr('required',false);
		}
	});
	$('#deleteOption').on('change', function() {
		var value = $('#deleteOption').val();
		if (value=='${QueuekitLists.DELALL}') {
			if ( confirm('${message(code: 'queuekit.DeleteAllConfirm.message')}')) { 
				postAction(value);
			}
		} else {
			postAction(value);
		}
	});
	var adminMessages = {"${QueuekitLists.POOL}":"${g.message(code:'queuekit.adminButton.PO')}",
            "${QueuekitLists.PRESERVE}":"${g.message(code:'queuekit.adminButton.PR')}",
            "${QueuekitLists.CHECKQUEUE}":"${g.message(code:'queuekit.adminButton.CQ')}",
            "${QueuekitLists.STOPEXECUTOR}":"${g.message(code:'queuekit.adminButton.ST')}",
            "${QueuekitLists.LIMITUSERABOVE}":"${g.message(code:'queuekit.adminButton.LA')}",
            "${QueuekitLists.LIMITUSERBELOW}":"${g.message(code:'queuekit.adminButton.LB')}",
            "${QueuekitLists.FLOODCONTROL}":"${g.message(code:'queuekit.adminButton.FC')}",
            "${QueuekitLists.DEFAULTCOMPARATOR}":"${g.message(code:'queuekit.adminButton.DC')}"
            
	}
	$('#adminButtons').on('change', function() {
		var value = $('#adminButtons').val();
		if (value!='') {
			var params=$.param({changeType:value});			
			return showDialog('${createLink(controller:'queueKit',action:'changeConfig')}?'+params,adminMessages[value]);
		}
	});
})

</script>
