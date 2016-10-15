package queuekit

import grails.plugins.Plugin
import org.grails.plugin.queuekit.ArrayBlockingExecutor
import org.grails.plugin.queuekit.LinkedBlockingExecutor
import org.grails.plugin.queuekit.priority.EnhancedPriorityBlockingExecutor
import org.grails.plugin.queuekit.priority.PriorityBlockingExecutor

class QueuekitGrailsPlugin extends Plugin {
	def version = "1.0"
	def grailsVersion = "2.4 > *"
	def title = "queuekit plugin"
	def description = """Queuekit plugin incorporates TaskExecutor ArrayBlocking / LinkBlocking and PriorityBlocking. 
	It also enhances on PriorityBlocking with a new custom method EnhancedPriorityBlocking. Define queue limit
	which in turn limits concurrent usage of all users. Typical request on demand report system will change to background queued 
	reports system. Choose the best blocking method for your queues. Both Priority and EnhancedPriority allow
	queue items to have a default or on the fly priority. EnhancedPriority has additional code that runs requested task within a sub thread. 
	When master task or live running task is cancelled. The underlying thread is cancelled. 
	This gives you the feature to cancel live background threaded tasks."""
	def documentation = "https://github.com/vahidhedayati/grails-queuekit-plugin"
	def license = "APACHE"
	def developers = [name: 'Vahid Hedayati', email: 'badvad@gmail.com']
	def issueManagement = [system: 'GITHUB', url: 'https://github.com/vahidhedayati/grails-queuekit-plugin/issues']
	def scm = [url: 'https://github.com/vahidhedayati/grails-queuekit-plugin']
	Closure doWithSpring() {
		{->
			arrayBlockingExecutor(ArrayBlockingExecutor)
			linkedBlockingExecutor(LinkedBlockingExecutor)
			priorityBlockingExecutor(PriorityBlockingExecutor)
			enhancedPriorityBlockingExecutor(EnhancedPriorityBlockingExecutor)
			priorityBlockingExecutor(PriorityBlockingExecutor)
		}
	}
}
