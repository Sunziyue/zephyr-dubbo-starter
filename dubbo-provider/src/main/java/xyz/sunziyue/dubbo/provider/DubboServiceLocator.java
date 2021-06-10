package xyz.sunziyue.dubbo.provider;

import java.util.Set;

public interface DubboServiceLocator {
    Set<Class<?>> services();
}
