<%@ page import="org.grails.plugin.queuekit.priority.Priority" %>
<form id="changePriorityForm">
<div class="form-group">
	<g:hiddenField name="queue.id" value="${instance.queue?.id}"/>	
	<label>New Priority for ${instance.queue.reportName}: </label>
	<g:select name="priority" class="form-control" from="${Priority.values()}" value="${instance.priority}"/>
	</div>
	<div class="form-group">
	<a class="btn btn-warning" onclick="closeModal()"><g:message code="queuekit.cancel.label"/></a>
	<g:submitButton name="submit" value="${g.message(code:'submit.label') }" class="btn btn-success"/>	
	</div>
<form>

<script>
$("#changePriorityForm").submit(function( event ) {
	var data = $("#changePriorityForm").serialize();
	 $.ajax({
         type: 'post',
         url: '${createLink(controller:'queueKit',action:'modifyPriority')}',
         data: data,
         success: function (response) {
             $('#modalCase').modal('hide');
             $('body').removeClass('modal-open');
             $('.modal-backdrop').remove();
        	 $('#results').html(data);
         }
     });
			  
});
</script>