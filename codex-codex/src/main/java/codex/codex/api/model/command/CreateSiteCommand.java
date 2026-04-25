package codex.codex.api.model.command;

import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.SiteStatus;

import java.util.Objects;
import java.util.Set;

public record CreateSiteCommand(
        SiteKey key,
        String displayName,
        SiteStatus status,
        Set<SiteAlias> aliases
) {

    public CreateSiteCommand {
        Objects.requireNonNull(key, "key cannot be null");
        Objects.requireNonNull(displayName, "displayName cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(aliases, "aliases cannot be null");

        if (displayName.isBlank()) {
            throw new IllegalArgumentException("displayName cannot be blank");
        }

        aliases = Set.copyOf(aliases);
    }

    public static CreateSiteCommand of(final SiteKey key, final String displayName) {
        return new CreateSiteCommand(
                key,
                displayName,
                SiteStatus.STARTED,
                Set.of()
        );
    }

    public static CreateSiteCommand of(
            final SiteKey key,
            final String displayName,
            final Set<SiteAlias> aliases
    ) {
        return new CreateSiteCommand(
                key,
                displayName,
                SiteStatus.STARTED,
                aliases
        );
    }
}
