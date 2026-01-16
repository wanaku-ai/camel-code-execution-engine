package ai.wanaku;

import ai.wanaku.downloader.ResourceType;
import ai.wanaku.util.WanakuRoutesLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

public class WanakuCamelManager {
    private final CamelContext context;
    private final String routesPath;
    private final String dependenciesList;
    private final String repositoriesList;

    public WanakuCamelManager(Map<ResourceType, Path> downloadedResources, String repositoriesList) {
        this.repositoriesList = repositoriesList;
        context = new DefaultCamelContext();

        this.routesPath = downloadedResources.get(ResourceType.ROUTES_REF).toString();
        if (downloadedResources.containsKey(ResourceType.DEPENDENCY_REF)) {
            String dependenciesPath =
                    downloadedResources.get(ResourceType.DEPENDENCY_REF).toString();
            try {
                this.dependenciesList = Files.readString(Path.of(dependenciesPath));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            this.dependenciesList = null;
        }

        loadRoutes();
    }

    public WanakuCamelManager(Path routesPath, String dependenciesList, String repositoriesList) {
        this.context = new DefaultCamelContext();
        this.routesPath = routesPath.toString();
        this.dependenciesList = dependenciesList;
        this.repositoriesList = repositoriesList;
        loadRoutes();
    }

    private void loadRoutes() {
        WanakuRoutesLoader routesLoader = new WanakuRoutesLoader(dependenciesList, repositoriesList);

        String routeFileUrl = String.format("file://%s", routesPath);
        routesLoader.loadRoute(context, routeFileUrl);
        context.start();
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
