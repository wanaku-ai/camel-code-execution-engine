package ai.wanaku.code.engine.camel.downloader;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;

public interface Downloader {

    void downloadResource(ResourceRefs<URI> resourceName, Map<ResourceType, Path> downloadedResources) throws Exception;
}
