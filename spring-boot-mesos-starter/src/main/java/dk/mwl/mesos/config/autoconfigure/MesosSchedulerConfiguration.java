package dk.mwl.mesos.config.autoconfigure;

import dk.mwl.mesos.scheduler.*;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Configuration
public class MesosSchedulerConfiguration {

    @Autowired
    Environment environment;

    @Bean
    public Scheduler scheduler() {
        return new UniversalScheduler();
    }

    @Bean
    public OfferStrategyFilter offerStrategyFilter() {
        return new OfferStrategyFilter();
    }

    private ResourceRequirement simpleScalarRequirement(String name, double minimumRequirement) {
        return (taskId, offer) -> new OfferEvaluation(
                taskId,
                offer,
                ResourceRequirement.scalarSum(offer, name) > minimumRequirement,
                Protos.Resource.newBuilder()
                        .setType(Protos.Value.Type.SCALAR)
                        .setName(name)
                        .setScalar(Protos.Value.Scalar.newBuilder().setValue(minimumRequirement))
                        .build()
        );

    }

    @Bean
    public Supplier<UUID> uuidSupplier() {
        return UUID::randomUUID;
    }

    @Bean
    @ConditionalOnMissingBean(TaskInfoFactory.class)
    @ConditionalOnProperty(prefix = "mesos.docker", name = {"image"})
    public TaskInfoFactory taskInfoFactory() {
        return new TaskInfoFactoryDocker();
    }

    @Bean
    @ConditionalOnMissingBean(name = "distinctHostRequirement")
    @ConditionalOnProperty(prefix = "mesos.resource.distinctSlave", havingValue = "true")
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ResourceRequirement distinctHostRequirement() {
        return new DistinctSlaveRequirement();
    }

    @Bean
    @ConditionalOnMissingBean(name = "scaleFactorRequirement")
    @ConditionalOnProperty(prefix = "mesos.resource", name = "scale")
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ResourceRequirement scaleFactorRequirement() {
        return new ScaleFactorRequirement(environment.getProperty("mesos.resource.scale", Integer.class, 1));
    }

    @Bean
    @ConditionalOnMissingBean(name = "cpuRequirement")
    @ConditionalOnProperty(prefix = "mesos.resources", name = "cpus")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ResourceRequirement cpuRequirement() {
        return simpleScalarRequirement("cpus", environment.getRequiredProperty("mesos.resources.cpus", Double.class));
    }

    @Bean
    @ConditionalOnMissingBean(name = "memRequirement")
    @ConditionalOnProperty(prefix = "mesos.resources", name = "mem")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ResourceRequirement memRequirement() {
        return simpleScalarRequirement("mem", environment.getRequiredProperty("mesos.resources.mem", Double.class));
    }

    @Bean
    @ConditionalOnMissingBean(name = "portsRequirement")
    @ConditionalOnProperty(prefix = "mesos.resources", name = "ports")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ResourceRequirement portsRequirement() {
        final int ports = environment.getRequiredProperty("mesos.resources.ports", Integer.class);

        return (taskId, offer) -> {
            final List<Long> chosenPorts = offer.getResourcesList().stream()
                    .filter(resource -> resource.getName().equals("ports"))
                    .flatMap(resource -> resource.getRanges().getRangeList().stream())
                    .flatMapToLong(range -> LongStream.rangeClosed(range.getBegin(), range.getEnd()))
                    .limit(ports)
                    .boxed()
                    .peek(port -> System.out.println("port = " + port))
                    .collect(Collectors.toList());
            return new OfferEvaluation(
                    taskId,
                    offer,
                    chosenPorts.size() == ports,
                    Protos.Resource.newBuilder()
                            .setType(Protos.Value.Type.RANGES)
                            .setName("ports")
                            .setRanges(Protos.Value.Ranges.newBuilder().addRange(Protos.Value.Range.newBuilder().setBegin(chosenPorts.get(0)).setEnd(chosenPorts.get(0))))
                            .build()
            );
        };
    }

}