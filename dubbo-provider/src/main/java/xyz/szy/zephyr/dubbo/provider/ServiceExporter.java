package xyz.szy.zephyr.dubbo.provider;

import lombok.extern.slf4j.Slf4j;
import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.spring.ServiceBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Primary;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import xyz.szy.zephyr.dubbo.common.DubboProperties;


@Slf4j
public class ServiceExporter {
    private final ApplicationContext applicationContext;
    private boolean published;
    private final ServiceBean provider = new ServiceBean<>();
    private final List<MethodConfig> methodSpecials = new ArrayList<>();

    ServiceExporter(ApplicationContext applicationContext, DubboProperties dubboProperties) {
        this.provider.setApplicationContext(applicationContext);
        this.provider.setApplication(dubboProperties.getApplication());
        this.provider.setProtocol(dubboProperties.getProtocol());
        this.provider.setRegistry(dubboProperties.getRegistry());
        this.provider.setProvider(dubboProperties.getProvider());
        if (dubboProperties.getProvider() != null) {
            this.provider.setVersion(dubboProperties.getProvider().getVersion());
        }
        this.provider.setRetries(0);
        this.applicationContext = applicationContext;
    }

    public ServiceExporter service(Class<?> service) {
        this.checkState();
        this.provider.setInterface(service);
        Map<String, ?> beans = this.applicationContext.getBeansOfType(service);
        if (CollectionUtils.isEmpty(beans)) {
            String interfaceName = service.getCanonicalName();
            log.error("no implementation found for service interface:{}", interfaceName);
            throw new RuntimeException("no implementation found for service interface: " + interfaceName);
        } else {
            Collection<?> values = beans.values();
            if (values.size() > 1) {
                Object impl = this.primaryBean(beans, service);
                this.provider.setRef(impl);
            } else {
                this.provider.setRef(values.iterator().next());
            }

            return this;
        }
    }

    public ServiceExporter name(String name) {
        this.checkState();
        this.provider.setBeanName(name);
        return this;
    }

    public ServiceExporter version(String version) {
        this.checkState();
        this.provider.setVersion(version);
        return this;
    }

    public ServiceExporter group(String group) {
        this.checkState();
        this.provider.setGroup(group);
        return this;
    }

    public ServiceExporter timeout(int timeout) {
        this.checkState();
        this.provider.setTimeout(timeout);
        return this;
    }

    public ServiceExporter methodTimeout(String methodName, int timeout) {
        this.checkState();
        MethodConfig ms = new MethodConfig();
        ms.setName(methodName);
        ms.setTimeout(timeout);
        this.methodSpecials.add(ms);
        return this;
    }

    public ServiceExporter serializeType(String serializeType) {
        this.checkState();
        this.provider.setSerialization(serializeType);
        return this;
    }

    public void publish() {
        try {
            this.provider.setMethods(this.methodSpecials);
            this.provider.setVersion(StringUtils.hasText(this.provider.getVersion()) ? this.provider.getVersion() : "1.0.0");
            this.provider.afterPropertiesSet();
            this.provider.export();
            this.published = true;
            log.info("Dubbo service({}) published", this.uniqueName());
        } catch (Exception var2) {
            throw new RuntimeException("failed to publish DUBBO service " + this.provider.getInterface() + ":" + this.provider.getVersion(), var2);
        }
    }

    private Object primaryBean(Map<String, ?> beans, Class<?> interfaceType) {
        Iterator<String> beanIterator = beans.keySet().iterator();
        String beanName;
        Primary primary;
        do {
            if (!beanIterator.hasNext()) {
                throw new RuntimeException("multiple bean of type(" + interfaceType + ") has been found, and no one annotated with @Primary ");
            }
            beanName = beanIterator.next();
            primary = this.applicationContext.findAnnotationOnBean(beanName, Primary.class);
        } while(primary == null);
        return beans.get(beanName);
    }

    private String uniqueName() {
        return this.provider.getInterface() + ":" + this.provider.getVersion();
    }

    private void checkState() {
        if (this.published) {
            throw new IllegalStateException("Dubbo provider has already been published");
        }
    }
}
