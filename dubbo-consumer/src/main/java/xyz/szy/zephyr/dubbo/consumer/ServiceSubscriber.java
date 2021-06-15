package xyz.szy.zephyr.dubbo.consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import xyz.szy.zephyr.dubbo.common.DubboProperties;

@Component
public class ServiceSubscriber {
    private final ApplicationContext applicationContext;
    private final DubboProperties dubboProperties;

    @Autowired
    public ServiceSubscriber(ApplicationContext applicationContext, DubboProperties dubboProperties) {
        this.applicationContext = applicationContext;
        this.dubboProperties = dubboProperties;
    }

    public <T> ConsumerService<T> consumer(Class<T> interfaceClass) {
        return new ConsumerService<>(this.applicationContext, this.dubboProperties, interfaceClass);
    }
}
