package xyz.szy.zephyr.dubbo.provider;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class SpringBootVFS {
    private final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver(this.getClass().getClassLoader());

    private SpringBootVFS() {
    }

    private static SpringBootVFS instance;

    public static SpringBootVFS getInstance(){
        if (instance == null) {
            instance =  new SpringBootVFS();
        }
        return instance;
    }

    protected List<String> list(String path) throws IOException {
        Resource[] resources = this.resourceResolver.getResources("classpath*:" + path + "/**/*.class");
        List<String> resourcePaths = new ArrayList<>();
        for (Resource resource : resources) {
            resourcePaths.add(preserveSubpackageName(resource.getURI(), path));
        }
        return resourcePaths;
    }

    private static String preserveSubpackageName(URI uri, String rootPath) {
        String uriStr = uri.toString();
        int start = uriStr.indexOf(rootPath);
        return uriStr.substring(start);
    }
}
