package xyz.szy.zephyr.dubbo.provider;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import xyz.szy.zephyr.dubbo.common.DubboProperties;

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
            log.info("[DUBBO CONFIG] 服务端口号: [{}]", this.dubboProperties.getProtocol().getPort());
            log.info("[DUBBO CONFIG] 服务注册在: [{}]", this.dubboProperties.getRegistry().getAddress());
            try {
                ApplicationContext applicationContext = event.getApplicationContext();
                if (CollectionUtils.isEmpty(this.serviceLocator.services())) {
                    log.error("未找到 DUBBO 服务");
                    throw new RuntimeException("未找到 DUBBO 服务");
                } else {
                    for (Class<?> serviceInterface : this.serviceLocator.services()) {
                        new ServiceExporter(applicationContext, this.dubboProperties).service(serviceInterface).publish();
                    }
                    String name = this.dubboProperties.getApplication().getName();
                    log.info("DUBBO 服务{}启动成功", name);
                }
                // 程序暂停
                HoldProcessor holdProcessor = new HoldProcessor();
                holdProcessor.startAwait();
                Runtime.getRuntime().addShutdownHook(new Thread(holdProcessor::stopAwait));
            } catch (Exception e) {
                log.error("DUBBO 服务启动失败, cause:{}", Throwables.getStackTraceAsString(e));
                throw new RuntimeException("DUBBO 服务启动失败", e);
            }
        }
    }
}
