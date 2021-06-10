package xyz.sunziyue.dubbo.provider;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

@Slf4j
public class DefaultVFS extends VFS {
    private static final byte[] JAR_MAGIC = new byte[]{80, 75, 3, 4};

    public DefaultVFS() {
    }

    public boolean isValid() {
        return true;
    }

    public List<String> list(URL url, String path) throws IOException {
        InputStream is = null;

        List<String> childrenList;
        try {
            List<String> resources = new ArrayList<>();
            URL jarUrl = this.findJarForResource(url);
            if (jarUrl != null) {
                is = jarUrl.openStream();
                if (log.isDebugEnabled()) {
                    log.debug("Listing " + url);
                }
                resources = this.listResources(new JarInputStream(is), path);
            } else {
                childrenList = new ArrayList<>();
                String child;
                try {
                    if (this.isJar(url)) {
                        is = url.openStream();
                        JarInputStream jarInput = new JarInputStream(is);
                        if (log.isDebugEnabled()) {
                            log.debug("Listing " + url);
                        }
                        JarEntry entry;
                        for (; (entry = jarInput.getNextJarEntry()) != null; childrenList.add(entry.getName())) {
                            if (log.isDebugEnabled()) {
                                log.debug("Jar entry: " + entry.getName());
                            }
                        }
                        jarInput.close();
                    } else {
                        is = url.openStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        ArrayList<String> lines = new ArrayList<>();

                        while ((child = reader.readLine()) != null) {
                            if (log.isDebugEnabled()) {
                                log.debug("Reader entry: " + child);
                            }

                            lines.add(child);
                            if (getResources(path + "/" + child).isEmpty()) {
                                lines.clear();
                                break;
                            }
                        }
                        if (!lines.isEmpty()) {
                            if (log.isDebugEnabled()) {
                                log.debug("Listing " + url);
                            }
                            childrenList.addAll(lines);
                        }
                    }
                } catch (FileNotFoundException var19) {
                    if (!"file".equals(url.getProtocol())) {
                        throw var19;
                    }
                    File file = new File(url.getFile());
                    if (log.isDebugEnabled()) {
                        log.debug("Listing directory " + file.getAbsolutePath());
                    }
                    if (file.isDirectory()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Listing " + url);
                        }
                        childrenList = Arrays.asList(Objects.requireNonNull(file.list()));
                    }
                }

                String prefix = url.toExternalForm();
                if (!prefix.endsWith("/")) {
                    prefix = prefix + "/";
                }
                for (String children : childrenList) {
                    String resourcePath = path + "/" + children;
                    resources.add(resourcePath);
                    URL childUrl = new URL(prefix + children);
                    resources.addAll(this.list(childUrl, resourcePath));
                }
            }
            childrenList = resources;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) { }
            }
        }
        return childrenList;
    }

    protected List<String> listResources(JarInputStream jar, String path) throws IOException {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        ArrayList<String> resources = new ArrayList<>();
        JarEntry entry;
        while ((entry = jar.getNextJarEntry()) != null) {
            if (!entry.isDirectory()) {
                String name = entry.getName();
                if (!name.startsWith("/")) {
                    name = "/" + name;
                }
                if (name.startsWith(path)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Found resource: " + name);
                    }
                    resources.add(name.substring(1));
                }
            }
        }
        return resources;
    }

    protected URL findJarForResource(URL url) {
        if (log.isDebugEnabled()) {
            log.debug("Find JAR URL: " + url);
        }
        try {
            while (true) {
                do {
                    url = new URL(url.getFile());
                } while (!log.isDebugEnabled());
                log.debug("Inner URL: " + url);
            }
        } catch (MalformedURLException e) {
            StringBuilder jarUrl = new StringBuilder(url.toExternalForm());
            int index = jarUrl.lastIndexOf(".jar");
            if (index >= 0) {
                jarUrl.setLength(index + 4);
                if (log.isDebugEnabled()) {
                    log.debug("Extracted JAR URL: " + jarUrl);
                }
                try {
                    URL testUrl = new URL(jarUrl.toString());
                    if (this.isJar(testUrl)) {
                        return testUrl;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Not a JAR: " + jarUrl);
                    }
                    jarUrl.replace(0, jarUrl.length(), testUrl.getFile());
                    File file = new File(jarUrl.toString());
                    if (!file.exists()) {
                        try {
                            file = new File(URLEncoder.encode(jarUrl.toString(), "UTF-8"));
                        } catch (UnsupportedEncodingException var7) {
                            throw new RuntimeException("Unsupported encoding?  UTF-8?  That's unpossible.");
                        }
                    }
                    if (file.exists()) {
                        if (log.isDebugEnabled()) {
                            log.debug("Trying real file: " + file.getAbsolutePath());
                        }
                        testUrl = file.toURI().toURL();
                        if (this.isJar(testUrl)) {
                            return testUrl;
                        }
                    }
                } catch (MalformedURLException var8) {
                    log.warn("Invalid JAR URL: " + jarUrl);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Not a JAR: " + jarUrl);
                }
                return null;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Not a JAR: " + jarUrl);
                }
                return null;
            }
        }
    }

    protected String getPackagePath(String packageName) {
        return packageName == null ? null : packageName.replace('.', '/');
    }

    protected boolean isJar(URL url) {
        return this.isJar(url, new byte[JAR_MAGIC.length]);
    }

    protected boolean isJar(URL url, byte[] buffer) {
        InputStream is = null;

        try {
            is = url.openStream();
            is.read(buffer, 0, JAR_MAGIC.length);
            if (Arrays.equals(buffer, JAR_MAGIC)) {
                if (log.isDebugEnabled()) {
                    log.debug("Found JAR: " + url);
                }
                return true;
            }
        } catch (Exception ignored) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception ignored) {
                }
            }

        }

        return false;
    }
}
