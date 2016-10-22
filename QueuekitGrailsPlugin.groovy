import org.grails.plugin.queuekit.ArrayBlockingExecutor
import org.grails.plugin.queuekit.LinkedBlockingExecutor
import org.grails.plugin.queuekit.priority.EnhancedPriorityBlockingExecutor
import org.grails.plugin.queuekit.priority.PriorityBlockingExecutor

class QueuekitGrailsPlugin {
	def version = "1.4"
	def grailsVersion = "2.4 > *"
	def title = "queuekit plugin"
	def description = """Queuekit plugin provides you with various ways of calling on TaskExecutor and specifically it's
 underlying queuing mechanism to control the creation of reports on your grails application. Queuekit can help whether
 you are using the existing grails export plugin or you manually produce csv,tsv or maybe even rely on apache-poi or it's
 likes to produce xls files as per user request. With this plugin you can change the process of files produces as requested
 to files produced when there is an available runable space for it to be executed. In effect saving your application from
 slowing down if there are surges for reports by your userbase."""
	def documentation = "https://github.com/vahidhedayati/grails-queuekit-plugin"
	def license = "APACHE"
	def developers = [name: 'Vahid Hedayati', email: 'badvad@gmail.com']
	def issueManagement = [system: 'GITHUB', url: 'https://github.com/vahidhedayati/grails-queuekit-plugin/issues']
	def scm = [url: 'https://github.com/vahidhedayati/grails-queuekit-plugin']
	def doWithSpring = {
		arrayBlockingExecutor(ArrayBlockingExecutor)
		linkedBlockingExecutor(LinkedBlockingExecutor)
		priorityBlockingExecutor(PriorityBlockingExecutor)
		enhancedPriorityBlockingExecutor(EnhancedPriorityBlockingExecutor)
	}
}
