package codex.custos.api.model;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoleAssignmentTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Alice");
    private static final RoleKey EDITOR = RoleKey.of("EDITOR");
    private static final SiteKey SITE = SiteKey.of("site-a");

    @Test
    @DisplayName("Creates assignment with actor, role key, and scope")
    void createAssignment() {
        SiteScope scope = new SiteScope(SITE);
        RoleAssignment assignment = RoleAssignment.of(ACTOR, EDITOR, scope);

        assertThat(assignment.actor()).isEqualTo(ACTOR);
        assertThat(assignment.role()).isEqualTo(EDITOR);
        assertThat(assignment.scope()).isEqualTo(scope);
    }

    @Test
    @DisplayName("Rejects null actor")
    void rejectsNullActor() {
        assertThatThrownBy(() -> RoleAssignment.of(null, EDITOR, new SiteScope(SITE)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects null role key")
    void rejectsNullRoleKey() {
        assertThatThrownBy(() -> RoleAssignment.of(ACTOR, null, new SiteScope(SITE)))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Rejects null scope")
    void rejectsNullScope() {
        assertThatThrownBy(() -> RoleAssignment.of(ACTOR, EDITOR, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Preserves actor")
    void preservesActor() {
        RoleAssignment assignment = RoleAssignment.of(ACTOR, EDITOR, GlobalScope.INSTANCE);
        assertThat(assignment.actor()).isSameAs(ACTOR);
    }

    @Test
    @DisplayName("Preserves role key")
    void preservesRoleKey() {
        RoleAssignment assignment = RoleAssignment.of(ACTOR, EDITOR, GlobalScope.INSTANCE);
        assertThat(assignment.role()).isEqualTo(EDITOR);
    }

    @Test
    @DisplayName("Preserves scope")
    void preservesScope() {
        SiteScope scope = new SiteScope(SITE);
        RoleAssignment assignment = RoleAssignment.of(ACTOR, EDITOR, scope);
        assertThat(assignment.scope()).isEqualTo(scope);
    }

    @Test
    @DisplayName("Equality works by value")
    void equalityByValue() {
        RoleAssignment first  = RoleAssignment.of(ACTOR, EDITOR, new SiteScope(SITE));
        RoleAssignment second = RoleAssignment.of(ACTOR, EDITOR, new SiteScope(SITE));
        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("Can assign COPYWRITER on SiteScope")
    void assignCopywriterOnSiteScope() {
        RoleKey copywriter = RoleKey.of("COPYWRITER");
        RoleAssignment assignment = RoleAssignment.of(ACTOR, copywriter, new SiteScope(SITE));

        assertThat(assignment.role()).isEqualTo(copywriter);
        assertThat(assignment.scope()).isInstanceOf(SiteScope.class);
    }

    @Test
    @DisplayName("Can assign REVIEWER on ContentTypeScope")
    void assignReviewerOnContentTypeScope() {
        RoleKey reviewer = RoleKey.of("REVIEWER");
        ContentTypeScope scope = new ContentTypeScope(SITE, ContentTypeKey.of("blog-post"));
        RoleAssignment assignment = RoleAssignment.of(ACTOR, reviewer, scope);

        assertThat(assignment.role()).isEqualTo(reviewer);
        assertThat(assignment.scope()).isInstanceOf(ContentTypeScope.class);
    }

    @Test
    @DisplayName("Does not contain a PermissionKey field")
    void hasNoPermissionKeyField() {
        assertThat(RoleAssignment.class.getDeclaredFields())
                .noneMatch(f -> f.getType() == PermissionKey.class);
    }

    @Test
    @DisplayName("Does not contain a Role field")
    void hasNoRoleField() {
        assertThat(RoleAssignment.class.getDeclaredFields())
                .noneMatch(f -> f.getType() == Role.class);
    }

    @Test
    @DisplayName("Does not evaluate access — has no evaluate or isGranted method")
    void doesNotEvaluateAccess() {
        assertThat(RoleAssignment.class.getDeclaredMethods())
                .noneMatch(m -> m.getName().equals("evaluate") || m.getName().equals("isGranted"));
    }
}
