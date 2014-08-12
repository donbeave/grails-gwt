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
        test('org.spockframework:spock-grails-support:0.7-groovy-2.0') {
            export = false
        }
    }
    plugins {
        test(':spock:0.7') {
            export = false
        }
        build(':release:3.0.1', ':rest-client-builder:2.0.1') {
            export = false
        }
    }
}

grails.release.scm.enabled = false