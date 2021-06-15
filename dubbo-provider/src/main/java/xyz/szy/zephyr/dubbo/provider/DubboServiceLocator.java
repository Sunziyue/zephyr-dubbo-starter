package xyz.szy.zephyr.dubbo.provider;

import java.util.Set;

public interface DubboServiceLocator {
    Set<Class<?>> services();
}
