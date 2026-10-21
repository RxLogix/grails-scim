package grails.plugins.scim

import grails.plugins.*
import grails.plugins.scim.binding.JsonScimApiDataBindingSourceCreator
import grails.plugins.scim.binding.DataBindingSourceRegistryUpdater
import groovy.util.logging.Slf4j

@Slf4j
class GrailsScimGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "7.0.0 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = []

    // TODO Fill in these fields
    def title = "Grails Scim" // Headline display name of the plugin
    def author = "RxLogix"
    def authorEmail = ""
    def description = '''\
    Grails Scim is a plugin library for dealing with scim interface integration for user/group resources.
'''
    def profiles = ['plugin']

    // URL to the plugin's documentation
    def documentation = "https://github.com/RxLogix/grails-scim"

    Closure doWithSpring() {
        { ->
            // PVADMIN-50831: these beans used to be registered only when the global grails.scim.enabled
            // was true. With per-tenant enablement the org-level flag can be false while a tenant is
            // enabled, and request bodies would then fail to bind. The host's scimTenantConfigProvider is
            // not yet defined at this point, so its presence cannot be checked here either. The beans
            // only add the application/scim+json body parser; access is still gated per request by
            // ScimControllerInterceptor.
            log.info "Loading scim plugin...."
            jsonScimApiDataBindingSourceCreator(JsonScimApiDataBindingSourceCreator) {
                grailsApplication = ref('grailsApplication')
            }
            dataBindingSourceRegistryUpdater(DataBindingSourceRegistryUpdater)
        }
    }

    void doWithDynamicMethods() {
        // TODO Implement registering dynamic methods to classes (optional)
    }

    void doWithApplicationContext() {
        // TODO Implement post initialization spring config (optional)
    }

    void onChange(Map<String, Object> event) {
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    void onConfigChange(Map<String, Object> event) {
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    void onShutdown(Map<String, Object> event) {
        // TODO Implement code that is executed when the application shuts down (optional)
    }

}
