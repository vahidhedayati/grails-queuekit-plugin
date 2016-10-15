<%@ page import="org.grails.plugin.queuekit.validation.ChangeConfigBean;org.grails.plugin.queuekit.ReportsQueue;org.grails.plugin.queuekit.validation.QueueKitBean" %>
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
			<g:if test="${instanceList.reportJobs}">
				<li><a class="jobButton" id="jobCtrl"><g:message code="queuekit.jobControl.label" args="${[g.message(code:'queuekit.show.label')]}"/></a></li>
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
		from="${searchList}" value="${search?.searchBy}" valueMessagePrefix="queuekit.searchType" />
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
		if (value!='') {
			$('#searchBy').attr('required',true);
		} else {
			$('#searchBy').attr('required',false);
		}
	});
	$('#deleteOption').on('change', function() {
		var value = $('#deleteOption').val();
		if (value=='${QueueKitBean.DELALL}') {
			if ( confirm('${message(code: 'queuekit.DeleteAllConfirm.message')}')) { 
				postAction(value);
			}
		} else {
			postAction(value);
		}
	});
	var adminMessages = {"${ChangeConfigBean.POOL}":"${g.message(code:'queuekit.adminButton.PO')}",
            "${ChangeConfigBean.PRESERVE}":"${g.message(code:'queuekit.adminButton.PR')}",
            "${ChangeConfigBean.CHECKQUEUE}":"${g.message(code:'queuekit.adminButton.CQ')}",
            "${ChangeConfigBean.STOPEXECUTOR}":"${g.message(code:'queuekit.adminButton.ST')}",
            "${ChangeConfigBean.LIMITUSERABOVE}":"${g.message(code:'queuekit.adminButton.LA')}",
            "${ChangeConfigBean.LIMITUSERBELOW}":"${g.message(code:'queuekit.adminButton.LB')}",
            "${ChangeConfigBean.FLOODCONTROL}":"${g.message(code:'queuekit.adminButton.FC')}",
            "${ChangeConfigBean.DEFAULTCOMPARATOR}":"${g.message(code:'queuekit.adminButton.DC')}"
            
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