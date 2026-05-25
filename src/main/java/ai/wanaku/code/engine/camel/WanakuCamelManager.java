package ai.wanaku.code.engine.camel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.wanaku.capabilities.sdk.maven.GAV;
import ai.wanaku.capabilities.sdk.maven.WanakuMavenDownloader;
import ai.wanaku.capabilities.sdk.runtime.camel.downloader.ResourceType;
import ai.wanaku.capabilities.sdk.runtime.camel.util.WanakuRoutesLoader;

public class WanakuCamelManager {
    private static final Logger LOG = LoggerFactory.getLogger(WanakuCamelManager.class);

    private final CamelContext context;
    private final String routesPath;
    private List<GAV> gavs = new ArrayList<>();

    public WanakuCamelManager(Map<ResourceType, Path> downloadedResources, String repositoriesList) throws Exception {
        this.routesPath = downloadedResources.get(ResourceType.ROUTES_REF).toString();
        if (downloadedResources.containsKey(ResourceType.DEPENDENCY_REF)) {
            String dependenciesPath =
                    downloadedResources.get(ResourceType.DEPENDENCY_REF).toString();
            try {
                final List<String> depLines = Files.readAllLines(Path.of(dependenciesPath));
                this.gavs = depLines.stream()
                        .filter(l -> !l.startsWith("#"))
                        .map(GAV::parse)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        WanakuMavenDownloader mavenDownloader = new WanakuMavenDownloader(WanakuCamelManager.class.getClassLoader());
        mavenDownloader.download(gavs);

        context = new DefaultCamelContext();
        context.setApplicationContextClassLoader(mavenDownloader.getClassLoader());
        loadRoutes();
    }

    public WanakuCamelManager(Path routesPath, String dependenciesList, String repositoriesList) throws Exception {
        this.routesPath = routesPath.toString();

        if (dependenciesList != null && !dependenciesList.isBlank()) {
            this.gavs = Arrays.stream(dependenciesList.split("[,\\n]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .filter(s -> !s.startsWith("#"))
                    .map(GAV::parse)
                    .collect(Collectors.toList());
        }

        WanakuMavenDownloader mavenDownloader = new WanakuMavenDownloader(WanakuCamelManager.class.getClassLoader());
        mavenDownloader.download(gavs);

        this.context = new DefaultCamelContext();
        context.setApplicationContextClassLoader(mavenDownloader.getClassLoader());
        loadRoutes();
    }

    private void loadRoutes() throws Exception {
        WanakuRoutesLoader routesLoader = new WanakuRoutesLoader();

        String routeFileUrl = Path.of(routesPath).toUri().toString();
        routesLoader.loadRoute(context, routeFileUrl);
        context.start();

        if (context.getRoutes().isEmpty()) {
            throw new RuntimeException("Failed to load routes from " + routeFileUrl);
        }
    }

    public CamelContext getCamelContext() {
        return context;
    }

    public void stop() {
        if (context != null) {
            context.stop();
        }
    }
}
