package codex.codex.api.model.command;

import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Represents an immutable request to start a Site.
 *
 * <p>This is an application command, not a GoF executable command.
 * It carries the intention and required input data for the start-site use case.
 * The command is interpreted by an application service or command handler.</p>
 */
public record StartSiteCommand(SiteKey key) {

    public StartSiteCommand {
        Objects.requireNonNull(key, "key cannot be null");
    }

    public static StartSiteCommand of(final SiteKey key) {
        return new StartSiteCommand(key);
    }
}
