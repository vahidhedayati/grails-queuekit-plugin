<g:set var="dateFormat" value="${message(code: 'default.date.format')}"/>

<div class="content">
	<div>
		<label class="bold"><g:message code="queuekit.reportName.label"/></label>
		${instance.reportName}
	</div>
	<g:if test="${instance.displayName}">
		<div>
			<label class="bold"><g:message code="queuekit.displayName.label"/></label>
			${instance.displayName}
		</div>
	</g:if>
	
	<div>
		<label class="bold"><g:message code="queuekit.locale.label"/></label>
		${instance.locale}
	</div>
	<div>
		<label class="bold"><g:message code="queuekit.params.label"/></label>
		${instance.parameters}
		<g:if test="${instance.formAsUrl}">
			<br/>
			${raw(instance?.formAsUrl)}
			${raw(instance?.formAsForm)}
		</g:if>
	</div>
	<div>
		<label class="bold"><g:message code="queuekit.username.label"/></label>
		${instance.userId}
	</div>
	<g:if test="${instance.fileName}">
		<div>
			<label class="bold"><g:message code="queuekit.fileName.label"/></label>
			${instance.fileName}
		</div>
	</g:if>
	<g:if test="${instance.created}">
	<div>
		<label class="bold"><g:message code="queuekit.created.label"/></label>
		<g:formatDate date="${instance.created}" format="${dateFormat}"/>
	</div>
	</g:if>
	<g:if test="${instance.start}">
		<div>
			<label class="bold"><g:message code="queuekit.startDate.label"/></label>
			<g:formatDate date="${instance.start}" format="${dateFormat}"/>
		</div>
	</g:if>
	<g:if test="${instance.requeued}">
		<div>
			<label class="bold"><g:message code="queuekit.requeued.label"/></label>
			<g:formatDate date="${instance.requeued}" format="${dateFormat}"/>
		</div>
	</g:if>
	<g:if test="${instance.retries}">
		<div>
			<label class="bold"><g:message code="queuekit.retries.label"/></label>
			${intance.retries }
		</div>
	</g:if>
	<g:if test="${instance.finished}">
		<div>
			<label class="bold"><g:message code="queuekit.finished.label"/></label>
			<g:formatDate date="${instance.finished}" format="${dateFormat}"/>
		</div>
	</g:if>
	
	<div>
		<label class="bold"><g:message code="queuekit.status.label"/></label>
		<g:message code="queuekit.reportType.${instance.status}"/>
	</div>
	
	<div>
		<label class="bold"><g:message code="queuekit.queueType.label"/></label>
		<g:message code="queuekit.queueType.${instance.queueType}"/>
	</div>
	
	<div>
		<label class="bold"><g:message code="queuekit.reportPriority.label"/></label>
		${instance?.priority?:''}
	</div>
</div>