package xyz.sunziyue.dubbo.provider;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import xyz.sunziyue.dubbo.common.DubboProperties;

@Configuration
@ComponentScan
@EnableConfigurationProperties({DubboProperties.class, DubboScanProperties.class})
public class DubboProviderAutoConfiguration {
    @ConditionalOnMissingBean
    @Bean
    public DubboServiceLocator serviceLocator(DubboScanProperties dubboScanProperties) {
        return new DubboScanServiceLocator(dubboScanProperties);
    }
}
