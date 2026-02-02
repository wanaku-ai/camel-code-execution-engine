package ai.wanaku.code.engine.camel.downloader;

/**
 * Defines the types of resources that can be downloaded and managed by the code execution engine.
 */
public enum ResourceType {
    /** Reference to Camel route definitions. */
    ROUTES_REF,
    /** Reference to MCP rules definitions. */
    RULES_REF,
    /** Reference to Maven dependency specifications. */
    DEPENDENCY_REF,
    /** Reference to a code generation package (tar.bz2 archive). */
    CODEGEN_PACKAGE,
}
