package ai.wanaku.code.engine.camel.codegen;

import ai.wanaku.capabilities.sdk.api.types.InputSchema;
import ai.wanaku.capabilities.sdk.api.types.Property;
import ai.wanaku.capabilities.sdk.api.types.ToolReference;
import ai.wanaku.capabilities.sdk.services.ServicesHttpClient;
import ai.wanaku.code.engine.camel.codegen.tools.GenerateOrchestrationTool;
import ai.wanaku.code.engine.camel.codegen.tools.ReadKameletTool;
import ai.wanaku.code.engine.camel.codegen.tools.SearchServicesTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registers and deregisters code generation tools with Wanaku.
 *
 * <p>This registrar handles:
 * <ul>
 *   <li>Creating tool references for searchServicesTool, readKamelet, and generateOrchestrationCode</li>
 *   <li>Registering tools with the Wanaku services API</li>
 *   <li>Deregistering tools on shutdown</li>
 * </ul>
 */
public class CodeGenToolRegistrar {
    private static final Logger LOG = LoggerFactory.getLogger(CodeGenToolRegistrar.class);

    private final ServicesHttpClient servicesHttpClient;
    private final CodeGenResourceLoader resourceLoader;
    private final List<ToolReference> registeredTools;
    private final String serviceName;

    /**
     * Creates a new CodeGenToolRegistrar.
     *
     * @param servicesHttpClient the HTTP client for registering tools
     * @param resourceLoader the resource loader providing tool configurations
     * @param serviceName the name of this service (used for tool URIs)
     */
    public CodeGenToolRegistrar(
            ServicesHttpClient servicesHttpClient, CodeGenResourceLoader resourceLoader, String serviceName) {
        this.servicesHttpClient = servicesHttpClient;
        this.resourceLoader = resourceLoader;
        this.serviceName = serviceName;
        this.registeredTools = new ArrayList<>();
    }

    /**
     * Registers all code generation tools with Wanaku.
     */
    public void registerTools() {
        LOG.info("Registering code generation tools with Wanaku");

        // Create tool instances
        SearchServicesTool searchTool = new SearchServicesTool(resourceLoader);
        ReadKameletTool readTool = new ReadKameletTool(resourceLoader);
        GenerateOrchestrationTool genTool = new GenerateOrchestrationTool(resourceLoader);

        // Register each tool
        registerTool(createSearchServicesToolReference(searchTool));
        registerTool(createReadKameletToolReference(readTool));
        registerTool(createGenerateOrchestrationToolReference(genTool));

        LOG.info("Successfully registered {} code generation tools", registeredTools.size());

        // Add shutdown hook for deregistration
        Runtime.getRuntime().addShutdownHook(new Thread(this::deregisterTools));
    }

    private void registerTool(ToolReference toolReference) {
        try {
            servicesHttpClient.addTool(toolReference);
            registeredTools.add(toolReference);
            LOG.info("Registered tool: {}", toolReference.getName());
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("409")) {
                LOG.warn("Tool {} already exists, skipping registration", toolReference.getName());
            } else {
                LOG.error("Failed to register tool {}: {}", toolReference.getName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Deregisters all previously registered tools from Wanaku.
     */
    public void deregisterTools() {
        LOG.info("Deregistering {} code generation tools from Wanaku", registeredTools.size());

        for (ToolReference ref : registeredTools) {
            try {
                servicesHttpClient.removeTool(ref.getName());
                LOG.debug("Deregistered tool: {}", ref.getName());
            } catch (Exception e) {
                LOG.warn("Failed to deregister tool {}: {}", ref.getName(), e.getMessage());
            }
        }

        registeredTools.clear();
        LOG.info("Tool deregistration complete");
    }

    /**
     * Returns the list of registered tools.
     *
     * @return unmodifiable list of registered tool references
     */
    public List<ToolReference> getRegisteredTools() {
        return Collections.unmodifiableList(registeredTools);
    }

    /**
     * Builds a tool URI using the service name as the scheme.
     *
     * @param toolName the tool name
     * @return the full tool URI (e.g., "myservice://toolName")
     */
    private String buildToolUri(String toolName) {
        return serviceName + "://" + toolName;
    }

    /**
     * Returns the configured namespace, or null if not set.
     *
     * @return the namespace from config, or null
     */
    private String getNamespace() {
        return resourceLoader.getConfig().getNamespace();
    }

    /**
     * Applies common settings to a tool reference.
     *
     * @param ref the tool reference to configure
     * @param name the tool name
     * @param description the tool description
     */
    private void applyCommonSettings(ToolReference ref, String name, String description) {
        ref.setName(name);
        ref.setDescription(description);
        ref.setUri(buildToolUri(name));
        ref.setType(serviceName);

        String namespace = getNamespace();
        if (namespace != null) {
            ref.setNamespace(namespace);
        }
    }

    private ToolReference createSearchServicesToolReference(SearchServicesTool tool) {
        ToolReference ref = new ToolReference();
        applyCommonSettings(ref, tool.getName(), tool.getDescription());

        // Empty input schema (no parameters)
        InputSchema schema = new InputSchema();
        schema.setType("object");
        schema.setProperties(Collections.emptyMap());
        schema.setRequired(Collections.emptyList());
        ref.setInputSchema(schema);

        return ref;
    }

    private ToolReference createReadKameletToolReference(ReadKameletTool tool) {
        ToolReference ref = new ToolReference();
        applyCommonSettings(ref, tool.getName(), tool.getDescription());

        // Input schema with 'name' parameter
        InputSchema schema = new InputSchema();
        schema.setType("object");

        Map<String, Property> properties = new HashMap<>();
        Property nameProp = new Property();
        nameProp.setType("string");
        nameProp.setDescription("The name of the Kamelet to read (without .kamelet.yaml suffix)");
        properties.put(ReadKameletTool.PARAM_NAME, nameProp);

        schema.setProperties(properties);
        schema.setRequired(List.of(ReadKameletTool.PARAM_NAME));
        ref.setInputSchema(schema);

        return ref;
    }

    private ToolReference createGenerateOrchestrationToolReference(GenerateOrchestrationTool tool) {
        ToolReference ref = new ToolReference();
        applyCommonSettings(ref, tool.getName(), tool.getDescription());

        // Empty input schema (no parameters)
        InputSchema schema = new InputSchema();
        schema.setType("object");
        schema.setProperties(Collections.emptyMap());
        schema.setRequired(Collections.emptyList());
        ref.setInputSchema(schema);

        return ref;
    }
}
