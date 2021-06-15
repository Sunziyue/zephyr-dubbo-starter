package xyz.szy.zephyr.dubbo.consumer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import xyz.szy.zephyr.dubbo.common.DubboProperties;

@Configuration
@EnableConfigurationProperties({DubboProperties.class})
@ComponentScan
public class DubboConsumerAutoConfiguration {
}
