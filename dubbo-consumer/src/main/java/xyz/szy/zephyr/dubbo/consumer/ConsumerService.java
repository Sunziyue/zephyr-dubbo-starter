package xyz.szy.zephyr.dubbo.consumer;

import com.alibaba.dubbo.config.MethodConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import xyz.szy.zephyr.dubbo.common.DubboProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class ConsumerService<T> {
    private final ReferenceBean<T> consumer = new ReferenceBean<>();
    private final List<MethodConfig> methodSpecials = new ArrayList<>();
    private boolean consumed;

    public ConsumerService(ApplicationContext applicationContext, DubboProperties dubboProperties, Class<T> interfaceClazz) {
        this.consumer.setApplicationContext(applicationContext);
        this.consumer.setApplication(dubboProperties.getApplication());
        this.consumer.setRegistry(dubboProperties.getRegistry());
        this.consumer.setConsumer(dubboProperties.getConsumer());
        if (dubboProperties.getConsumer() != null) {
            this.consumer.setVersion(dubboProperties.getConsumer().getVersion());
        }

        this.consumer.setRetries(0);
        if (interfaceClazz != null && interfaceClazz.isInterface()) {
            this.consumer.setInterface(interfaceClazz);
        } else {
            throw new IllegalArgumentException("必须提供要订阅的接口.");
        }
    }

    public ConsumerService<T> version(String version) {
        this.checkState();
        this.consumer.setVersion(version);
        return this;
    }

    public ConsumerService<T> group(String group) {
        this.checkState();
        this.consumer.setGroup(group);
        return this;
    }

    public ConsumerService<T> registry(String... addresses) {
        this.checkState();
        List<RegistryConfig> registries = Arrays.stream(addresses).map(RegistryConfig::new).collect(Collectors.toList());
        this.consumer.setRegistries(registries);
        return this;
    }

    public ConsumerService<T> timeout(int timeout) {
        this.checkState();
        this.consumer.setTimeout(timeout);
        return this;
    }

    public ConsumerService<T> checked(Boolean needChecked) {
        this.checkState();
        this.consumer.setCheck(needChecked);
        return this;
    }

    public ConsumerService<T> methodTimeout(String methodName, int timeout) {
        this.checkState();
        MethodConfig methodConfig = new MethodConfig();
        methodConfig.setName(methodName);
        methodConfig.setTimeout(timeout);
        this.methodSpecials.add(methodConfig);
        return this;
    }

    public ConsumerService<T> directUrl(String url) {
        this.checkState();
        if (StringUtils.hasText(url)) {
            this.consumer.setUrl(url);
            log.warn("DUBBO消费者[{}]配置为连接[{}], 生产环境不推荐使用!", this.uniqueName(), url);
        }
        return this;
    }

    public T subscribe() {
        try {
            this.consumer.setMethods(this.methodSpecials);
            this.consumer.setVersion(StringUtils.hasText(this.consumer.getVersion()) ? this.consumer.getVersion() : "1.0.0");
            this.consumer.afterPropertiesSet();
            this.consumed = true;
            log.info("DUBBO消费者[{}]准备好了", this.uniqueName());
            return this.consumer.get();
        } catch (Exception e) {
            throw new RuntimeException("订阅DUBBO服务失败: [" + this.uniqueName() + "]", e);
        }
    }

    private String uniqueName() {
        return this.consumer.getInterface() + ":" + this.consumer.getVersion();
    }

    private void checkState() {
        if (this.consumed) {
            throw new IllegalStateException("由于DUBBO消费者 [" + this.uniqueName() + "] 已经建立, 因此无法设置更多属性.");
        }
    }
}
