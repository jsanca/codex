package codex.custos.api.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable snapshot of the role assignments and role registry used during resolution.
 * <p>
 * The caller assembles this snapshot from the available data (in-memory store, database query
 * result, or test fixture) and passes it to {@code PermissionResolver.resolve()} alongside a
 * {@link PermissionResolutionRequest}.
 * <p>
 * Both collections are defensively copied on construction. Null elements and null map entries
 * are rejected at construction time.
 *
 * @see PermissionResolutionRequest
 */
public record PermissionResolutionSnapshot(
        Collection<RoleAssignment> assignments,
        Map<RoleKey, Role> roleRegistry) {

    public PermissionResolutionSnapshot {
        Objects.requireNonNull(assignments, "assignments must not be null");
        Objects.requireNonNull(roleRegistry, "roleRegistry must not be null");
        assignments = List.copyOf(assignments);    // immutable; throws NPE on null elements
        roleRegistry = Map.copyOf(roleRegistry);  // immutable; throws NPE on null keys or values
    }

    /**
     * Creates a {@link PermissionResolutionSnapshot} from the given assignments and registry.
     *
     * @param assignments  the known role assignments; must not be null or contain nulls
     * @param roleRegistry map from {@link RoleKey} to {@link Role} blueprint;
     *                     must not be null or contain null keys/values
     * @return an immutable snapshot
     */
    public static PermissionResolutionSnapshot of(final Collection<RoleAssignment> assignments,
                                                  final Map<RoleKey, Role> roleRegistry) {
        return new PermissionResolutionSnapshot(assignments, roleRegistry);
    }
}
