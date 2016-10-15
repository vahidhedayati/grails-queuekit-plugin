package org.grails.plugin.queuekit.examples

import grails.core.GrailsApplication
import grails.core.support.GrailsApplicationAware

/**
 * Created by Vahid Hedayati on 11/10/16.
 * Interceptor for ReportDemo to disable it if user disables configuration
 *
 */
class DemoInterceptor implements GrailsApplicationAware {
    def config
    GrailsApplication grailsApplication

    public DemoInterceptor() {
        match controller: 'reportDemo'
    }

    boolean before() {
        if (config.disableExamples) {
            redirect controller: 'reportDemo', action: 'notFound'
            return false
        }
        return true
    }

    void setGrailsApplication(GrailsApplication ga) {
        config = ga.config.queuekit
    }


}
