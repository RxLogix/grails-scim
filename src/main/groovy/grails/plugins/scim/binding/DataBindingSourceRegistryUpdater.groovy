package grails.plugins.scim.binding

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.grails.web.databinding.bindingsource.DataBindingSourceRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent

@CompileStatic
@Slf4j
class DataBindingSourceRegistryUpdater implements ApplicationListener<ContextRefreshedEvent> {

    @Autowired
    DataBindingSourceRegistry dataBindingSourceRegistry

    @Autowired
    JsonScimApiDataBindingSourceCreator jsonScimApiDataBindingSourceCreator

    @Override
    void onApplicationEvent(ContextRefreshedEvent event) {
        dataBindingSourceRegistry.addDataBindingSourceCreator(jsonScimApiDataBindingSourceCreator)
        log.debug "JsonScimApiDataBindingSourceCreator added to DataBindingSourceRegistry"
    }
}