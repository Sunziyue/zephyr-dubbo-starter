package xyz.szy.zephyr.dubbo.provider;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
public class JBoss6VFS extends VFS {
    private static Boolean valid;

    public JBoss6VFS() {
    }

    protected static synchronized void initialize() {
        if (valid == null) {
            valid = Boolean.TRUE;
            JBoss6VFS.VFS.VFS = checkNotNull(getClass("org.jboss.vfs.VFS"));
            JBoss6VFS.VirtualFile.VirtualFile = checkNotNull(getClass("org.jboss.vfs.VirtualFile"));
            JBoss6VFS.VFS.getChild = checkNotNull(getMethod(VFS.VFS, "getChild", URL.class));
            JBoss6VFS.VirtualFile.getChildrenRecursively = checkNotNull(getMethod(VirtualFile.VirtualFile, "getChildrenRecursively"));
            JBoss6VFS.VirtualFile.getPathNameRelativeTo = checkNotNull(getMethod(VirtualFile.VirtualFile, "getPathNameRelativeTo", VirtualFile.VirtualFile));
            checkReturnType(JBoss6VFS.VFS.getChild, JBoss6VFS.VirtualFile.VirtualFile);
            checkReturnType(JBoss6VFS.VirtualFile.getChildrenRecursively, List.class);
            checkReturnType(JBoss6VFS.VirtualFile.getPathNameRelativeTo, String.class);
        }

    }

    protected static <T> T checkNotNull(T object) {
        if (object == null) {
            setInvalid();
        }

        return object;
    }

    protected static void checkReturnType(Method method, Class<?> expected) {
        if (method != null && !expected.isAssignableFrom(method.getReturnType())) {
            log.error("Method " + method.getClass().getName() + "." + method.getName() + "(..) should return " + expected.getName() + " but returns " + method.getReturnType().getName() + " instead.");
            setInvalid();
        }
    }

    protected static void setInvalid() {
        if (valid.equals(Boolean.TRUE)) {
            log.debug("JBoss 6 VFS API is not available in this environment.");
            valid = Boolean.FALSE;
        }
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> list(URL url, String path) throws IOException {
        JBoss6VFS.VirtualFile directory = JBoss6VFS.VFS.getChild(url);
        if (directory == null) {
            return Collections.emptyList();
        } else {
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            List<JBoss6VFS.VirtualFile> children = directory.getChildren();
            List<String> names = new ArrayList<>(children.size());
            for (VirtualFile vf : children) {
                names.add(path + vf.getPathNameRelativeTo(directory));
            }
            return names;
        }
    }

    static {
        initialize();
    }

    static class VFS {
        static Class<?> VFS;
        static Method getChild;

        private VFS() {
        }

        static JBoss6VFS.VirtualFile getChild(URL url) throws IOException {
            Object o = xyz.szy.zephyr.dubbo.provider.VFS.invoke(getChild, VFS, url);
            return o == null ? null : new JBoss6VFS.VirtualFile(o);
        }
    }

    static class VirtualFile {
        static Class<?> VirtualFile;
        static Method getPathNameRelativeTo;
        static Method getChildrenRecursively;
        Object virtualFile;

        VirtualFile(Object virtualFile) {
            this.virtualFile = virtualFile;
        }

        String getPathNameRelativeTo(JBoss6VFS.VirtualFile parent) {
            try {
                return xyz.szy.zephyr.dubbo.provider.VFS.invoke(getPathNameRelativeTo, this.virtualFile, new Object[]{parent.virtualFile});
            } catch (IOException var3) {
                JBoss6VFS.log.error("This should not be possible. VirtualFile.getPathNameRelativeTo() threw IOException.");
                return null;
            }
        }

        List<JBoss6VFS.VirtualFile> getChildren() throws IOException {
            List<?> objects = xyz.szy.zephyr.dubbo.provider.VFS.invoke(getChildrenRecursively, this.virtualFile, new Object[0]);
            List<JBoss6VFS.VirtualFile> children = new ArrayList<>(objects.size());
            for (Object object : objects) {
                children.add(new VirtualFile(object));
            }
            return children;
        }
    }
}
