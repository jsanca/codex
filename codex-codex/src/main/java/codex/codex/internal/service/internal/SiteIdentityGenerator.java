package codex.codex.internal.service.internal;

import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.identity.SiteId;
import codex.fundamentum.api.model.IdentityGenerator;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

public class SiteIdentityGenerator implements IdentityGenerator<CreateSiteCommand, SiteId> {
    @Override
    public SiteId nextIdentity(final CreateSiteCommand source) {
        Objects.requireNonNull(source, "source cannot be null");

        final String identitySource = "site:" + source.key().value();
        final UUID uuid = UUID.nameUUIDFromBytes(identitySource.getBytes(StandardCharsets.UTF_8));

        return SiteId.of(uuid.toString());
    }
}
