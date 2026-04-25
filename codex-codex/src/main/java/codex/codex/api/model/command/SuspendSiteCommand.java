package codex.codex.api.model.command;

import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Represents an immutable request to suspend a Site.
 *
 * <p>This is an application command, not a GoF executable command.
 * It carries the intention and required input data for the suspend-site use case.
 * The command is interpreted by an application service or command handler.</p>
 */
public record SuspendSiteCommand(SiteKey key) {

    public SuspendSiteCommand {
        Objects.requireNonNull(key, "key cannot be null");
    }

    public static SuspendSiteCommand of(final SiteKey key) {
        return new SuspendSiteCommand(key);
    }
}
