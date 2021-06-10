package xyz.sunziyue.dubbo.common;

import com.alibaba.dubbo.config.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(
        prefix = "dubbo"
)
@Data
@NoArgsConstructor
public class DubboProperties {
    @NestedConfigurationProperty
    private ApplicationConfig application;
    @NestedConfigurationProperty
    private ModuleConfig module;
    @NestedConfigurationProperty
    private RegistryConfig registry;
    @NestedConfigurationProperty
    private ProtocolConfig protocol;
    @NestedConfigurationProperty
    private MonitorConfig monitor;
    @NestedConfigurationProperty
    private ProviderConfig provider;
    @NestedConfigurationProperty
    private ConsumerConfig consumer;
}