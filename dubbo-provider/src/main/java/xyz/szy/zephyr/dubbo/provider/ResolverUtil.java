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
            List<String> children = Objects.requireNonNull(VFS.getInstance()).list(path);
            for (String child : children) {
                if (child.endsWith(".class")) {
                    this.addIfMatching(test, child);
                }
            }
        } catch (IOException e) {
            log.error("Could not read package: " + packageName, e);
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
                log.debug("Checking to see if class " + externalName + " matches criteria [" + test + "]");
            }

            Class<?> type = loader.loadClass(externalName);
            if (test.matches(type)) {
                this.matches.add(type);
            }
        } catch (Throwable var6) {
            log.warn("Could not examine class '" + fqn + "' due to a " + var6.getClass().getName() + " with message: " + var6.getMessage());
        }

    }

    public static class AnnotatedWith implements ResolverUtil.Test {
        private final Class<? extends Annotation> annotation;

        public AnnotatedWith(Class<? extends Annotation> annotation) {
            this.annotation = annotation;
        }

        public boolean matches(Class<?> type) {
            return type != null && type.isAnnotationPresent(this.annotation);
        }

        public String toString() {
            return "annotated with @" + this.annotation.getSimpleName();
        }
    }

    public static class IsA implements ResolverUtil.Test {
        private final Class<?> parent;

        public IsA(Class<?> parentType) {
            this.parent = parentType;
        }

        public boolean matches(Class<?> type) {
            return type != null && this.parent.isAssignableFrom(type);
        }

        public String toString() {
            return "is assignable to " + this.parent.getSimpleName();
        }
    }

    public interface Test {
        boolean matches(Class<?> var1);
    }
}
