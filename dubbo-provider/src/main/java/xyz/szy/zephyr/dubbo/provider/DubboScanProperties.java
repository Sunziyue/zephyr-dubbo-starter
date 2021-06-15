package xyz.szy.zephyr.dubbo.provider;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ConfigurationProperties(
        prefix = "dubbo.provider"
)
public class DubboScanProperties {
    private Set<String> scanPackages = new LinkedHashSet<>();
}
