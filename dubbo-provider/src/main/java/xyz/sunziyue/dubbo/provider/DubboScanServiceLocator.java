package xyz.sunziyue.dubbo.provider;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class DubboScanServiceLocator implements DubboServiceLocator {
    private final DubboScanProperties dubboScanProperties;

    public DubboScanServiceLocator(DubboScanProperties dubboScanProperties) {
        this.dubboScanProperties = dubboScanProperties;
    }

    @Override
    public Set<Class<?>> services() {
        try {
            Set<Class<?>> exportedServiceSet = Sets.newHashSet();
            ResolverUtil resolverUtil = new ResolverUtil();
            Set<String> scanPackages = this.dubboScanProperties.getScanPackages();
            scanPackages.forEach((basePackage) -> {
                ResolverUtil util = resolverUtil.find(new ResolverUtil.IsA(Object.class), basePackage);
                Set<Class<?>> classes = util.getClasses();
                classes.forEach((candidate) -> {
                    if (candidate.isInterface() && candidate.getEnclosingClass() == null) {
                        exportedServiceSet.add(candidate);
                    }
                });
            });
            return exportedServiceSet;
        } catch (Exception e) {
            log.error("{}包中的DUBBO服务接口扫描失败, 原因:{}", Joiner.on(" , ").join(this.dubboScanProperties.getScanPackages()), Throwables.getStackTraceAsString(e));
            throw new RuntimeException("扫描DUBBO服务接口失败", e);
        }
    }
}
