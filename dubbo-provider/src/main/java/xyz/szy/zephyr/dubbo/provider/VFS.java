package xyz.szy.zephyr.dubbo.provider;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

@Slf4j
public abstract class VFS {
    public static final List<Class<? extends VFS>> IMPLEMENTATIONS = Arrays.asList(JBoss6VFS.class, DefaultVFS.class);
    public static final List<Class<? extends VFS>> USER_IMPLEMENTATIONS = new ArrayList<>();
    private static VFS instance;

    public VFS() {
    }

    public static VFS getInstance() {
        USER_IMPLEMENTATIONS.add(SpringBootVFS.class);
        if (instance == null) {
            List<Class<? extends VFS>> impls = new ArrayList<>();
            impls.addAll(USER_IMPLEMENTATIONS);
            impls.addAll(IMPLEMENTATIONS);
            VFS vfs = null;
            for (int i = 0; vfs == null || vfs.isValid(); ++i) {
                Class<? extends VFS> impl = impls.get(i);
                try {
                    vfs = impl.newInstance();
                    if (vfs.isValid() && log.isDebugEnabled()) {
                        log.debug("VFS implementation " + impl.getName() + " is not valid in this environment.");
                    }
                } catch (InstantiationException | IllegalAccessException e) {
                    log.error("Failed to instantiate " + impl, e);
                    return null;
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Using VFS adapter " + vfs.getClass().getName());
            }
            instance = vfs;
        }
        return instance;
    }

    public static void addImplClass(Class<? extends VFS> clazz) {
        if (clazz != null) {
            USER_IMPLEMENTATIONS.add(clazz);
        }
    }

    protected static Class<?> getClass(String className) {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(className);
        } catch (ClassNotFoundException var2) {
            if (log.isDebugEnabled()) {
                log.debug("Class not found: " + className);
            }
            return null;
        }
    }

    protected static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        if (clazz == null) {
            return null;
        } else {
            try {
                return clazz.getMethod(methodName, parameterTypes);
            } catch (SecurityException e) {
                log.error("Security exception looking for method " + clazz.getName() + "." + methodName + ".  Cause: " + e);
                return null;
            } catch (NoSuchMethodException e) {
                log.error("Method not found " + clazz.getName() + "." + methodName + "." + methodName + ".  Cause: " + e);
                return null;
            }
        }
    }

    protected static <T> T invoke(Method method, Object object, Object... parameters) throws IOException, RuntimeException {
        try {
            return (T) method.invoke(object, parameters);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof IOException) {
                throw (IOException) e.getTargetException();
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    protected static List<URL> getResources(String path) throws IOException {
        return Collections.list(Thread.currentThread().getContextClassLoader().getResources(path));
    }

    public abstract boolean isValid();

    protected abstract List<String> list(URL var1, String var2) throws IOException;

    public List<String> list(String path) throws IOException {
        List<String> names = new ArrayList<>();
        for (URL url : getResources(path)) {
            names.addAll(this.list(url, path));
        }
        return names;
    }
}
