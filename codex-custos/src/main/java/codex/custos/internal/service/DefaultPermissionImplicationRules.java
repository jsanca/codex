package codex.custos.internal.service;

import codex.custos.api.model.PermissionKey;
import codex.custos.api.model.Permissions;

import java.util.Set;

/**
 * Default implementation of {@link PermissionImplicationRules} encoding the ADR-009 rules.
 *
 * <h2>Implication rules</h2>
 * <ul>
 *   <li>{@code contentItem.update} implies {@code contentItem.read}</li>
 *   <li>{@code contentItem.publish} implies {@code contentItem.read}</li>
 *   <li>{@code contentItem.publish} does <em>not</em> imply {@code contentItem.update} (asymmetric)</li>
 *   <li>No other implications exist.</li>
 * </ul>
 */
final class DefaultPermissionImplicationRules implements PermissionImplicationRules {

    @Override
    public boolean permits(final Set<PermissionKey> rolePermissions, final PermissionKey requested) {
        if (rolePermissions.contains(requested)) {
            return true;
        }
        // contentItem.update and contentItem.publish both imply contentItem.read
        if (Permissions.CONTENT_ITEM_READ.equals(requested)) {
            return rolePermissions.contains(Permissions.CONTENT_ITEM_UPDATE)
                    || rolePermissions.contains(Permissions.CONTENT_ITEM_PUBLISH);
        }
        return false;
    }
}
