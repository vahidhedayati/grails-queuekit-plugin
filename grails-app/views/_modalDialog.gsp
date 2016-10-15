<style>
.queueKitModal .modal-content {
    -webkit-border-radius: 0;
    -webkit-background-clip: padding-box;
    -moz-border-radius: 0;
    -moz-background-clip: padding;
    border-radius: 6px;
    background-clip: padding-box;
    -webkit-box-shadow: 0 0 40px rgba(0,0,0,.5);
    -moz-box-shadow: 0 0 40px rgba(0,0,0,.5);
    box-shadow: 0 0 40px rgba(0,0,0,.5);
    color: #000;
    background-color: #fff;
    border: rgba(0,0,0,0);
}
.queueKitModal .modal-message .modal-dialog {
    width: 300px;
}
.queueKitModal .modal-message .modal-body, .modal-message .modal-footer, .modal-message .modal-header, .modal-message .modal-title {
    background: 0 0;
    border: none;
    margin: 0;
    padding: 0 20px;
    text-align: center!important;
}

.queueKitModal .modal-message .modal-title {
    font-size: 17px;
    color: #737373;
    margin-bottom: 3px;
}

.queueKitModal .modal-message .modal-body {
    color: #737373;
}

.queueKitModal .modal-message .modal-header {
    color: #fff;
    margin-bottom: 10px;
    padding: 15px 0 8px;
}
.queueKitModal .modal-message .modal-header .fa, 
.queueKitModal .modal-message .modal-header 
.glyphicon, .queueKitModal .modal-message 
.modal-header .typcn,.queueKitModal  .modal-message .modal-header .wi {
    font-size: 30px;
}

.queueKitModal .modal-message .modal-footer {
    margin: 25px 0 20px;
    padding-bottom: 10px;
}

.queueKitModal .modal-backdrop.in {
    zoom: 1;
    filter: alpha(opacity=75);
    -webkit-opacity: .75;
    -moz-opacity: .75;
    opacity: .75;
}
.queueKitModal .modal-backdrop {
    background-color: #fff;
}
.queueKitModal .modal-message.modal-success .modal-header {
    color: #53a93f;
    border-bottom: 3px solid #a0d468;
}

.queueKitModal .modal-message.modal-info .modal-header {
    color: #57b5e3;
    border-bottom: 3px solid #57b5e3;
}

.queueKitModal .modal-message.modal-danger .modal-header {
    color: #d73d32;
    border-bottom: 3px solid #e46f61;
}

.queueKitModal .modal-message.modal-warning .modal-header {
    color: #f4b400;
    border-bottom: 3px solid #ffce55;
}
</style>
<div class="modal fade queueKitModal" id="modalCase" role="dialog">
	<div class="modal-dialog">
		<div class="modal-content">
		
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">x</button>
				<h3>
				<span id="modalTitle">
				
				</span>
				</h3>
			</div>
			<div class="modal-body">
			<div id="modalContent">
			</div>
			</div>
			<div class="modal-footer">
			</div>
			
		</div>
	</div>
</div>
<script>
	function closeModal() {
		$('#modalCase').modal('hide');
		$('body').removeClass('modal-open');
		$('.modal-backdrop').remove();
	}
</script>