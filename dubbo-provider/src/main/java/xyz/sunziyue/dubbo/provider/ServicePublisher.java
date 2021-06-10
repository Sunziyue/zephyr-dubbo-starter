package xyz.sunziyue.dubbo.provider;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import xyz.sunziyue.dubbo.common.DubboProperties;

@Slf4j
@Component
public class ServicePublisher {
    private final DubboProperties dubboProperties;
    private final DubboServiceLocator serviceLocator;

    @Autowired
    public ServicePublisher(DubboProperties dubboProperties, DubboServiceLocator serviceLocator) {
        this.dubboProperties = dubboProperties;
        this.serviceLocator = serviceLocator;
    }

    @EventListener
    public void publish(ContextRefreshedEvent event) {
        if (this.dubboProperties.getProtocol() != null) {
            log.info("[DUBBO CONFIG] service export port: {}", this.dubboProperties.getProtocol().getPort());
            log.info("[DUBBO CONFIG] service registry: {}", this.dubboProperties.getRegistry().getAddress());
            try {
                ApplicationContext applicationContext = event.getApplicationContext();
                if (CollectionUtils.isEmpty(this.serviceLocator.services())) {
                    log.error("No DUBBO service found");
                    throw new RuntimeException("no DUBBO service to exported");
                } else {
                    for (Class<?> serviceInterface : this.serviceLocator.services()) {
                        (new ServiceExporter(applicationContext, this.dubboProperties)).service(serviceInterface).publish();
                    }
                    String name = this.dubboProperties.getApplication().getName();
                    log.info("DUBBO service(name={}) start successfully", name);
                }
            } catch (Exception e) {
                log.error("DUBBO service start failed, cause:{}", Throwables.getStackTraceAsString(e));
                throw new RuntimeException("application start failed", e);
            }
        }
    }
}
