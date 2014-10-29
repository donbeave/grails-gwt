grails.project.dependency.resolver = 'maven'

grails.project.dependency.resolution = {
    inherits('global')
    log 'warn'
    repositories {
        grailsCentral()
        mavenCentral()
    }
    dependencies {
        build 'com.google.gwt:gwt-user:2.5.0'
        build 'com.google.gwt:gwt-servlet:2.5.0'
    }
    plugins {
        build(':release:3.0.1', ':rest-client-builder:2.0.1') {
            export = false
        }
    }
}

grails.release.scm.enabled = false