
grails.project.work.dir = 'target'

grails.project.dependency.resolution = {
	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
		mavenRepo "https://repo.grails.org/grails/plugins"
	}

	dependencies {
		runtime 'org.springframework:spring-test:3.2.8.RELEASE'
	}

	plugins {
		runtime ":hibernate4:4.3.6.1", {
			export = false
		}		
		build ':release:3.0.1', ':rest-client-builder:2.0.3', {
			export = false
		}
		compile "org.grails.plugins:spring-events:1.2"
	}
}
