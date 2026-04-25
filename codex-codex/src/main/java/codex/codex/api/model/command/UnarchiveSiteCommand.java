package codex.codex.api.model.command;

import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Represents an immutable request to unarchive a Site.
 *
 * <p>This is an application command, not a GoF executable command.
 * It carries the intention and required input data for the unarchive-site use case.
 * The command is interpreted by an application service or command handler.</p>
 */
public record UnarchiveSiteCommand(SiteKey key) {

    public UnarchiveSiteCommand {
        Objects.requireNonNull(key, "key cannot be null");
    }

    public static UnarchiveSiteCommand of(final SiteKey key) {
        return new UnarchiveSiteCommand(key);
    }
}
