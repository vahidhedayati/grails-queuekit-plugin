<%@ page import="org.grails.plugin.queuekit.priority.Priority;org.grails.plugin.queuekit.ReportsQueue;org.grails.plugin.queuekit.validation.ChangeConfigBean" %>
<div id="error">
</div>
<form id="changeConfigForm">
	<g:hiddenField name="changeType" value="${instance.changeType}"/>
	<g:if test="${instance.queue}">
		<g:hiddenField name="queue.id" value="${instance.queue.id}"/> 
	</g:if>	
	<h4>
		<g:if test="${instance.queueType}">
		<g:message code="queuekit.queueType.${instance.queueType}"/> :
		</g:if>
		<g:message code="queuekit.changeType.${instance.changeType}.label" args="${[g.message(code:'queuekit.configure.label')]}"/>
	</h4>
	<div class="form-group">	
		<g:if test="${!instance.queue}">
			<g:select name="queueType" noSelection="${['':'']}" required="required" 
			from="${instance.queueList}"
			 valueMessagePrefix="queuekit.queueType" onChange="configureFields(this.value)"/>	
		</g:if>
	</div>	
	<span id="priorityGroup">
		<span class="form-group">
			<label>
			<g:message code="queuekit.changePriority.${instance.changeType}.label" args="${[g.message(code:'queuekit.modify.label')]}"/>		
			</label>	
			<g:select name="priority" class="form-control" from="${Priority.values()}" value="${instance.priority}"/>
		</span>
	</span>
	<span id="changeValueGroup">
		<span class="form-group">
			<label>
				<g:message code="queuekit.changeType.${instance.changeType}.label" args="${[g.message(code:'queuekit.modify.label')]}"/>		
			</label>	
			<g:textField name="changeValue" required="" value="${instance.changeValue?:instance.currentValue?:0}"/>
		</span>
	</span>
	
	<span id="floodControlGroup">
		<span class="form-group">
			<label>
				<g:message code="queuekit.changeType.${instance.changeType}.label" args="${[g.message(code:'queuekit.enable.label')]}"/>		
			</label>
			<g:select name="floodControl" class="form-control" from="${[0,1,2]}" value="${instance.floodControl}" valueMessagePrefix="queuekit.floodControl"/>
		</span>
	</span>
	
	<span id="defaultComparatorGroup">
		<span class="form-group">
			<label>
				<g:message code="queuekit.changeType.${instance.changeType}.label" args="${[g.message(code:'queuekit.enable.label')]}"/>		
			</label>
			<g:select name="defaultComparator" class="form-control" from="${[true,false]}" value="${instance.defaultComparator}" valueMessagePrefix="queuekit.status"/>
		</span>
	</span>
	<div class="form-group">
		<span id="buttons">	
			<a class="btn btn-warning" onclick="closeModal()"><g:message code="queuekit.cancel.label"/></a>
			<g:submitButton name="submit" value="${g.message(code:'submit.label') }" class="btn btn-success"/>	
		</span>
		<a class="btn btn-danger" style="display:none" id="closeForm" onclick="closeModal()"><g:message code="queuekit.close.label"/></a>
	</div>
<form>
<g:if test="${instance.changeType==ChangeConfigBean.MAXQUEUE}">
<g:message code="queuekit.maxQueue1.message"/>
</g:if>
<g:if test="${instance.changeType==ChangeConfigBean.POOL}">
<g:message code="queuekit.maxPool1.message"/>
</g:if>
<g:if test="${instance.changeType==ChangeConfigBean.CHECKQUEUE}">
<g:message code="queuekit.checkQueue1.message"/>
<g:message code="queuekit.checkQueue2.message"/>
<g:message code="queuekit.checkQueue3.message"/>
<g:message code="queuekit.checkQueue4.message"/>
<g:message code="queuekit.checkQueue5.message"/>
<g:message code="queuekit.checkQueue6.message"/><br/>
<g:message code="queuekit.checkQueue7.message"/>
<g:message code="queuekit.checkQueue8.message"/>
</g:if>

<g:if test="${instance.changeType==ChangeConfigBean.STOPEXECUTOR}">
<g:message code="queuekit.stopExecutor1.message"/>
<g:message code="queuekit.stopExecutor2.message"/>
<g:message code="queuekit.stopExecutor3.message"/><br/>
<g:message code="queuekit.stopExecutor4.message"/>
<g:message code="queuekit.stopExecutor5.message"/>
<g:message code="queuekit.stopExecutor6.message"/>
</g:if>
<g:if test="${instance.changeType==ChangeConfigBean.LIMITUSERABOVE||instance.changeType==ChangeConfigBean.LIMITUSERBELOW}">
<g:message code="queuekit.limitUser1.message"/>
</g:if>
<g:if test="${instance.changeType==ChangeConfigBean.FLOODCONTROL}">
<g:message code="queuekit.floodControl1.message"/>
</g:if>
<g:if test="${instance.changeType==ChangeConfigBean.DEFAULTCOMPARATOR}">
<g:message code="queuekit.defaultComparator1.message"/>
</g:if>

<script>
$("#changeConfigForm").submit(function( event ) {
	var data = $("#changeConfigForm").serialize();
	 $.ajax({
         type: 'post',
         url: '${createLink(controller:'queueKit',action:'modifyConfig')}',
         data: data,
         success: function (response) {
        	 closeModal();
        	 $('#results').html(data);
         },
         error: function(xhr,status,error){
             $('#error').html(status);             
         }    
     });
});
$(function() {
	hideField('priority');
	hideField('changeValue');
	hideField('floodControl');
	hideField('defaultComparator');
})
function configureFields(queueType) {
	var changeType="${instance.changeType}";
	var isPool = changeType=='${ChangeConfigBean.POOL}';
	var isQueue = changeType=='${ChangeConfigBean.MAXQUEUE}';
	var isPreserve = changeType=='${ChangeConfigBean.PRESERVE}';
	var isConfig = changeType=='${ChangeConfigBean.PRESERVE}';
	var isLimit = changeType=='${ChangeConfigBean.LIMITUSERABOVE}'||changeType=='${ChangeConfigBean.LIMITUSERBELOW}';
	var isQueueCheck = changeType=='${ChangeConfigBean.CHECKQUEUE}';
	var isFlood = changeType=='${ChangeConfigBean.FLOODCONTROL}';
	var isDefaultComparator = changeType=='${ChangeConfigBean.DEFAULTCOMPARATOR}';
	if (isConfig||isLimit||isPool||isQueue) {
		if ((queueType=='${ReportsQueue.LINKEDBLOCKING}'||queueType=='${ReportsQueue.ARRAYBLOCKING}') && (isConfig||isLimit)) {			 
			hideButtons();
			hideField('priority');
		} else {
			showButtons(changeType,queueType,isPreserve);
		}
	} else if (isFlood) {
		showType(changeType,queueType,'floodControl');
	} else if (isDefaultComparator) {
		showType(changeType,queueType,'defaultComparator');	
	} else {
		if (isConfig) {
			showButtons(changeType,queueType,isPreserve);
			hideField('priority');
		}
	}
}
function hideButtons() {
	$('#closeForm').show();
	$('#buttons').hide();	
	hideField('priority');
	hideField('changeValue');
	hideField('floodControl');
	hideField('defaultComparator');
}
function hideField(called) {
	$('#'+called+'Group').hide();
	$('#'+called).attr('prop','disabled',true).attr('required',false);
}
function showField(called) {
	$('#'+called+'Group').show();
	$('#'+called).attr('prop','disabled',false).attr('required',true);
}

function showType(changeType,queueType,field) {
	$('#closeForm').hide();
	$('#buttons').show();
	hideField('changeValue');
	hideField('priority');
	showField(field);
	postButton(changeType,queueType);
}

function showButtons(changeType,queueType,isPreserve) {
	$('#closeForm').hide();
	$('#buttons').show();
	showField('changeValue');
	if (isPreserve) {
		showField('priority');		
	} else {
		hideField('priority');
	}
	postButton(changeType,queueType);
}
function postButton(changeType,queueType) {
	$.ajax({
        type: 'post',
        url: '${createLink(controller:'queueKit',action:'loadConfig')}',
        data: {changeType:changeType,queueType:queueType},
        success: function (data) {
            var jsonValue = JSON.parse(data.value);
            if (jsonValue) {
                $('#changeValue').val(jsonValue);
            }
            if (data.priority) {
                $('#priority option[value="'+data.priority.name+'"]').prop('selected', true);
            }
            var jsonFloodControl = JSON.parse(data.floodControl);
            if (jsonFloodControl) {
            	$('#floodControl option[value="'+jsonFloodControl+'"]').prop('selected', true);
            }
            var jsonDefaultComparator = JSON.parse(data.defaultComparator);
            if (jsonDefaultComparator) {
            	$('#defaultComparator option[value="'+jsonDefaultComparator+'"]').prop('selected', true);
            }           
        }
    });
}
</script>