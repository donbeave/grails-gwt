grails.project.dependency.resolver = 'maven'

grails.project.dependency.resolution = {
    inherits('global')
    log 'warn'
    repositories {
        grailsCentral()
        mavenCentral()
    }
    dependencies {
        build 'com.google.gwt:gwt-user:2.5.1'
        build 'com.google.gwt:gwt-servlet:2.5.1'
    }
    plugins {
        build(':release:3.1.1', ':rest-client-builder:2.1.1') {
            export = false
        }
    }
}

grails.release.scm.enabled = false
