package xyz.szy.zephyr.dubbo.provider;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Slf4j
public class ResolverUtil {
    private final Set<Class<?>> matches = new HashSet<>();
    private ClassLoader classloader;

    public ResolverUtil() {
    }

    public Set<Class<?>> getClasses() {
        return this.matches;
    }

    public ClassLoader getClassLoader() {
        return this.classloader == null ? Thread.currentThread().getContextClassLoader() : this.classloader;
    }

    public void setClassLoader(ClassLoader classloader) {
        this.classloader = classloader;
    }

    public ResolverUtil findImplementations(Class<?> parent, String... packageNames) {
        if (packageNames != null) {
            Test test = new IsA(parent);
            for (String pkg : packageNames) {
                this.find(test, pkg);
            }

        }
        return this;
    }

    public ResolverUtil findAnnotated(Class<? extends Annotation> annotation, String... packageNames) {
        if (packageNames != null) {
            Test test = new AnnotatedWith(annotation);
            for (String pkg : packageNames) {
                this.find(test, pkg);
            }
        }
        return this;
    }

    public ResolverUtil find(ResolverUtil.Test test, String packageName) {
        String path = this.getPackagePath(packageName);
        try {
            List<String> children = Objects.requireNonNull(SpringBootVFS.getInstance()).list(path);
            for (String child : children) {
                if (child.endsWith(".class")) {
                    this.addIfMatching(test, child);
                }
            }
        } catch (IOException e) {
            log.error("无法读取包: " + packageName, e);
        }
        return this;
    }

    protected String getPackagePath(String packageName) {
        return packageName == null ? null : packageName.replace('.', '/');
    }

    protected void addIfMatching(ResolverUtil.Test test, String fqn) {
        try {
            String externalName = fqn.substring(0, fqn.indexOf(46)).replace('/', '.');
            ClassLoader loader = this.getClassLoader();
            if (log.isDebugEnabled()) {
                log.debug("检查 " + externalName + " 类是否符合条件 [" + test + "]");
            }
            Class<?> type = loader.loadClass(externalName);
            if (test.matches(type)) {
                this.matches.add(type);
            }
        } catch (Throwable e) {
            log.warn("无法检查类 '" + fqn + "' 由于一个 " + e.getClass().getName() + " 消息: " + e.getMessage());
        }

    }

    public static class AnnotatedWith implements ResolverUtil.Test {
        private final Class<? extends Annotation> annotation;

        public AnnotatedWith(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }

        @Override
        public boolean matches(Class<?> clazz) {
            return clazz != null && clazz.isAnnotationPresent(this.annotation);
        }

        public String toString() {
            return "用 @" + this.annotation.getSimpleName() + " 注解";
        }
    }

    public static class IsA implements ResolverUtil.Test {
        private final Class<?> parent;

        public IsA(Class<?> parentType) {
            this.parent = parentType;
        }

        @Override
        public boolean matches(Class<?> clazz) {
            return clazz != null && this.parent.isAssignableFrom(clazz);
        }

        public String toString() {
            return "可分配给 " + this.parent.getSimpleName();
        }
    }

    public interface Test {
        boolean matches(Class<?> clazz);
    }
}
