package codex.fundamentum.api.model;

/**
 * Represents the kind of actor performing an action in Codex.
 */
public enum ActorType {

    /**
     * A human user.
     */
    HUMAN,

    /**
     * An internal system process.
     */
    SYSTEM,

    /**
     * An autonomous or semi-autonomous agent.
     */
    AGENT,

    /**
     * An external API client or integration.
     */
    API_CLIENT
}