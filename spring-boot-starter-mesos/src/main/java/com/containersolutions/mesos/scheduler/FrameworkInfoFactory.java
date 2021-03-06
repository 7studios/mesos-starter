package com.containersolutions.mesos.scheduler;

import com.containersolutions.mesos.scheduler.config.MesosConfigProperties;
import com.containersolutions.mesos.scheduler.state.StateRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mesos.Protos;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Creates framework info
 */
public class FrameworkInfoFactory {
    protected final Log logger = LogFactory.getLog(getClass());

    @Value("${spring.application.name}")
    protected String applicationName;

    @Autowired
    MesosConfigProperties mesosConfig;

    @Autowired
    StateRepository stateRepository;

    @Autowired
    CredentialFactory credentialFactory;

    public Protos.FrameworkInfo.Builder create() {
        Protos.FrameworkInfo.Builder frameworkBuilder = Protos.FrameworkInfo.newBuilder()
                .setName(applicationName)
                .setUser("root")
                .setRole(mesosConfig.getRole())
                .setCheckpoint(true)
                .setFailoverTimeout(60.0)
                .setId(stateRepository.getFrameworkID().orElseGet(() -> Protos.FrameworkID.newBuilder().setValue("").build()));
        Protos.Credential credential = credentialFactory.create();
        if (credential.isInitialized()) {
            logger.debug("Adding framework principal: " + credential.getPrincipal());
            frameworkBuilder.setPrincipal(credential.getPrincipal());
        }
        return frameworkBuilder;
    }
}
