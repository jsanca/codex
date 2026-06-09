# ADR-009: Custos Domain Authorization Model

## Status

Proposed

## Context

Codex needs a security and permission model before exposing REST endpoints, agentic operations, or administrative tooling.

The authorization model must be independent from HTTP, Spring Security, servlet APIs, JWT, sessions, SAML, OIDC, or any specific authentication mechanism. Codex should not authorize transport-level requests directly. Instead, Codex should authorize domain operations performed by known actors over Codex resources.

Authentication may be delegated to an external identity provider, such as SSO, OIDC, SAML, API gateways, or other third-party mechanisms. Once authentication has happened, Codex receives or resolves an internal `Actor` and evaluates what that actor is allowed to do.

The model must also support future interaction with Olorin, the Codex AI agent. Olorin should be able to explain, propose, and prepare permission changes, but must not become an authority capable of granting permissions by itself.

## Decision

Custos will model authorization around domain concepts:

- `Actor`
- `Role`
- `Permission`
- `ResourceRef`
- `ResourceScope`
- `Policy`
- `AccessDecision`

Codex will authorize domain operations, not endpoints.

For example, Codex should evaluate:

```text
Can Actor X publish ContentItem Y in Site Z?
````

not:

```text
Can this HTTP request call POST /api/content/{id}/publish?
```

Transport adapters, such as REST, CLI, cron, or Olorin, may translate external requests into Codex domain operations. Custos remains transport-agnostic.

## Core Rule

Custos answers this question:

```text
May this Actor perform this Permission on this Codex Resource under this Context?
```

And it must explain the answer through an `AccessDecision`.

## Actor Model

Custos must explicitly model who is acting.

Initial actor types:

```text
USER
SYSTEM
API_CLIENT
ANONYMOUS
AGENT
CRON_JOB
```

Examples:

* `UserActor`: a real human user.
* `SystemActor`: internal Codex system execution.
* `ApiClientActor`: an external integration.
* `AnonymousActor`: unauthenticated public actor.
* `AgentActor`: Olorin or another AI/automation agent.
* `CronJobActor`: scheduled execution.

Cron jobs may be represented as system-driven actors, but for audit purposes they should allow an owner, delegated actor, or virtual system user to be recorded.

For example:

```text
actor: CronJobActor("nightly-publisher")
owner/delegatedBy: SystemActor("codex-scheduler")
reason: "scheduled publishing job"
```

Olorin may also act as a proposer under a human context:

```text
requestedBy: AgentActor("olorin")
approvedBy: UserActor("jsanca")
executedAs: UserActor("jsanca")
```

Olorin does not execute privileged permission changes as itself.

## Authentication Boundary

Custos does not own authentication in the initial design.

Codex may receive an already-authenticated actor from:

* SSO
* OIDC
* SAML
* API gateway
* CLI credentials
* system context
* internal execution
* other authentication adapters

Custos only evaluates authorization for a resolved `Actor`.

The authentication mechanism is external to this ADR.

## Permissions

Permissions are domain-oriented and granular.

Examples:

```text
site.read
site.create
site.start
site.suspend
site.archive

contentType.read
contentType.create
contentType.update
contentType.delete

contentItem.read
contentItem.create
contentItem.update
contentItem.publish
contentItem.unpublish
contentItem.archive

permission.read
permission.grant
permission.revoke

role.assign
role.revoke
```

Permissions should not be modeled as endpoint permissions.

Avoid:

```text
GET /api/sites
POST /api/content
```

Prefer:

```text
site.read
contentItem.create
contentItem.publish
```

## Write, Update, Edit, and Olorin Vocabulary

The canonical internal permission name for modifying content is:

```text
contentItem.update
```

However, human-facing tools and Olorin may treat the following words as equivalent user intent:

```text
write
edit
modify
update
escribir
editar
modificar
```

For example, if a user tells Olorin:

```text
Give Juan write permissions on BlogPost in site-a.
```

Olorin may translate that to:

```text
contentItem.update on ContentTypeScope(site-a, BlogPost)
```

The internal model remains precise, while the user-facing language remains natural.

## Read, Update, and Publish Semantics

For content resources, `read`, `update`, and `publish` are separate capabilities.

The following implication rules apply:

```text
contentItem.update implies contentItem.read
contentItem.publish implies contentItem.read
contentItem.publish does not imply contentItem.update
```

Publishing is not the same as editing.

This allows editorial models such as:

```text
COPYWRITER:
- read
- update

REVIEWER:
- read
- publish

EDITOR:
- read
- update
- publish
```

A user may be allowed to publish content without being allowed to modify the content body.

This distinction is important for workflows, auditability, and Olorin explanations.

## Roles

Roles are permission blueprints.

Examples:

```text
SUPER_ADMIN
SITE_ADMIN
EDITOR
COPYWRITER
REVIEWER
VIEWER
```

A role is not necessarily global. Role assignments are scoped.

For example:

```text
Juan has COPYWRITER on Site A.
Juan has VIEWER on Site B.
Ana has REVIEWER on ContentType BlogPost in Site A.
```

This means the same role name may have different effective meaning depending on scope and configuration.

Roles should simplify administration, but the authorization engine should ultimately evaluate permissions and policies.

Avoid hard-coding application logic as:

```text
if user has role ADMIN
```

Prefer evaluating capabilities:

```text
Can this actor perform contentItem.publish on this resource?
```

## ResourceRef and ResourceScope

Custos distinguishes between `ResourceRef` and `ResourceScope`.

### ResourceRef

A `ResourceRef` points to the concrete resource being evaluated.

Examples:

```text
SiteResourceRef(siteKey)
ContentTypeResourceRef(siteKey, contentTypeKey)
ContentItemResourceRef(siteKey, contentTypeKey, contentItemKey)
GlobalResourceRef
```

A resource reference should carry enough site context to avoid accidental cross-site permission lookup.

### ResourceScope

A `ResourceScope` represents where a permission or role is assigned.

Examples:

```text
GlobalScope
SiteScope(siteKey)
ContentTypeScope(siteKey, contentTypeKey)
ContentItemScope(siteKey, contentTypeKey, contentItemKey)
```

A permission assigned in one site never applies to another site.

## Permission Resolution

Permissions are scoped by site, content type, and content item.

For content item authorization, Custos should resolve permissions using a clear hierarchy:

```text
1. Check direct grants on the ContentItem scope.
2. Check grants on the ContentType scope within the same Site.
3. Check grants on the Site scope.
4. If no grant is found, deny.
```

The lookup must not cross site boundaries.

For example:

```text
contentItem: home-page
site: site-a
contentType: LandingPage
```

The lookup may check:

```text
ContentItemScope(site-a, LandingPage, home-page)
ContentTypeScope(site-a, LandingPage)
SiteScope(site-a)
```

It must not check:

```text
SiteScope(site-b)
```

## SUPER_ADMIN

`SUPER_ADMIN` is the only normal permission bypass.

A valid `SUPER_ADMIN` may bypass scoped permission lookup.

However, `SUPER_ADMIN` still respects hard system invariants.

For example, even a `SUPER_ADMIN` must not be allowed to:

```text
- convert an AgentActor into SUPER_ADMIN
- assign impossible permissions to an actor type
- violate structural invariants of the system
- bypass hardcoded safety rules
```

`SUPER_ADMIN` means bypassing normal scoped grants. It does not mean corrupting the authorization model.

## Agent Restrictions

Agent actors, including Olorin, must not manage permissions.

This must be a hardcoded rule, not only a database configuration.

An `AgentActor` cannot:

```text
- assign roles
- revoke roles
- grant permissions
- revoke permissions
- receive SUPER_ADMIN
- approve privileged security changes
```

Even if invalid data exists granting these capabilities to an agent, Custos must deny the operation.

Example hard rule:

```text
If actor.type == AGENT and permission is permission-management related, deny.
```

Another hard rule:

```text
If targetActor.type == AGENT and requested role is SUPER_ADMIN, deny.
```

Olorin may explain, propose, compare, or prepare permission changes, but it may not apply privileged changes by its own authority.

## Anonymous Actor

`AnonymousActor` must not receive general mutative permissions over internal CMS resources.

For example, anonymous should not be granted:

```text
contentItem.update
contentItem.publish
contentType.create
role.assign
permission.grant
```

However, anonymous may execute explicitly public operations controlled by policy.

Examples:

```text
page.view
asset.read
search.query
form.submit
```

A public form submission does not mean anonymous has write permission on the CMS.

Instead, it means a public operation is enabled by policy:

```text
AnonymousActor may perform form.submit on FormResourceRef(contact-us)
if the form accepts anonymous submissions and all public-form policies pass.
```

Public-form policies may include:

```text
- form is active
- site is started
- anonymous submissions are enabled
- rate limits pass
- captcha or anti-spam rules pass
- field allowlist is respected
- origin policy passes, if configured
```

## Policy

A policy is a rule that may grant, deny, constrain, or explain an authorization decision beyond simple role-permission lookup.

Examples:

```text
- Agent actors cannot manage permissions.
- Anonymous can submit a public form only if the form allows anonymous submissions.
- A content item cannot be published if it is archived.
- A user cannot grant permissions they do not possess.
- A user cannot assign a role outside their administrative scope.
```

Policies must produce explainable decisions.

## AccessDecision

Custos authorization should return an `AccessDecision`, not only a boolean.

An access decision should communicate:

```text
- granted or denied
- actor
- permission
- resource
- reason
- optional policy that decided the result
```

Example granted decision:

```text
GRANTED:
Actor Juan has contentItem.update on Site site-a through role COPYWRITER.
contentItem.update implies contentItem.read.
```

Example denied decision:

```text
DENIED:
Actor Juan does not have contentItem.publish on the content item,
its content type, or its site.
```

Example agent denial:

```text
DENIED:
Agent actors cannot grant permissions.
```

This is important for debugging, audit logs, UI explanations, and Olorin.

## Higher-Level Permission Services

Custos may expose a low-level evaluator:

```java
AccessDecision evaluate(
        Actor actor,
        PermissionKey permission,
        ResourceRef resource,
        SecurityEvaluationContext context
);
```

However, Codex services should prefer expressive domain-specific permission services.

Examples:

```java
sitePermissionsService.canRead(actor, siteKey);
sitePermissionsService.canStart(actor, siteKey);
contentItemPermissionsService.canUpdate(actor, resource);
contentItemPermissionsService.canPublish(actor, resource);
roleAssignmentPermissionsService.canAssignRole(actor, targetActor, role, scope);
```

This makes security checks easier to read and audit.

Prefer:

```java
contentItemPermissionsService.canPublish(actor, resource).requireGranted();
```

over less expressive calls scattered everywhere.

## Secured Service Decorators

Custos authorization should integrate with Codex services through explicit decorators, consistent with the existing forwarding service pattern.

Example:

```text
SecuredSiteService
  -> delegate SiteService
```

A secured service checks authorization before delegating to the underlying domain service.

Example flow:

```text
1. Resolve current Actor from Codex context.
2. Ask SitePermissionsService if actor can start the site.
3. If denied, throw AccessDeniedException with AccessDecision.
4. If granted, delegate to SiteService.start(siteKey).
```

This keeps domain entities free from security infrastructure while keeping authorization close to domain operations.

## Permission Shortcuts and Olorin

Internally, Custos stores precise scoped permissions.

However, UI and Olorin may expose shortcuts for human-friendly administration.

Examples:

```text
Give Juan the same Copywriter permissions in site-y that he has in site-x.

Copy the permission profile of COPYWRITER from site-x to site-y.

Show me what Ana can do in site-a.

Compare EDITOR permissions between site-a and site-b.
```

These shortcuts must not bypass Custos.

They must be translated into explicit permission grants, revokes, or role assignments, and then validated normally.

For example:

```text
1. Read permission profile for COPYWRITER in site-x.
2. Build a PermissionChangePlan for site-y.
3. Validate that the requesting actor can grant those permissions.
4. Validate that the target actor or role can receive those permissions.
5. Apply hardcoded actor-type restrictions.
6. Require step-up approval if the change is sensitive.
7. Audit the applied changes.
```

## Olorin Execution Model

Olorin may propose permission changes but cannot approve or apply privileged permission changes by itself.

For security-sensitive operations, Olorin must prepare a `PermissionChangePlan`.

A human or trusted non-agent actor must approve and execute the plan.

Audit should preserve the difference between:

```text
requestedBy: AgentActor("olorin")
approvedBy: UserActor("jsanca")
executedAs: UserActor("jsanca")
```

Olorin is an assistant, not a security authority.

## Step-Up Approval for Sensitive Actions

Security-sensitive operations require explicit confirmation beyond normal authorization.

This is called step-up authorization.

Sensitive operations may include:

```text
role.assign
role.revoke
permission.grant
permission.revoke
user.disable
apiClient.create
apiClient.rotateSecret
site.archive
site.delete
contentType.delete
workflow.security.change
```

The step-up mechanism must be authentication-agnostic.

Codex must not assume it can ask for a local password.

Step-up approval may come from:

```text
- SSO re-authentication
- OIDC/SAML provider challenge
- passkey / WebAuthn
- MFA / TOTP
- email magic link
- temporary approval code
- signed approval token
```

Custos only consumes a verified approval.

The approval should be:

```text
- short-lived
- single-use
- bound to the approving actor
- bound to the exact action plan
- bound to the target scope
```

A token approved for one permission change must not be reusable for another.

## PermissionChangePlan

For agent-assisted or sensitive security changes, Codex should represent the intended change as a plan before applying it.

A plan may include:

```text
- target actor or role
- target scope
- grants to add
- grants to remove
- roles to assign
- roles to revoke
- explanation
- fingerprint/hash of the exact change
```

The step-up approval should be tied to the exact plan fingerprint.

If the plan changes after approval, Custos must deny execution and require a new approval.

## Audit Expectations

Custos decisions should be auditable.

For permission changes, audit should capture:

```text
- who requested the change
- who approved the change
- who executed the change
- what changed
- target actor
- target role
- target scope
- step-up approval reference, if applicable
- reason or explanation
```

For denied sensitive operations, audit should capture enough information to explain the denial without leaking secrets.

## Consequences

### Positive

* Codex remains independent from REST, Spring Security, JWT, SAML, OIDC, and servlet APIs.
* Authorization is based on domain operations, not transport endpoints.
* Permissions are granular and easier to explain.
* Multi-tenant site boundaries are explicit.
* Olorin can explain and propose permission changes safely.
* Security-sensitive operations require human or trusted actor approval.
* Agent actors are prevented from becoming security authorities.
* `AccessDecision` improves debugging, UI explanations, and auditability.

### Negative / Tradeoffs

* More explicit permission checks may produce more code.
* Scoped permissions are more complex than global roles.
* `read`, `update`, and `publish` separation requires careful UI design.
* Step-up approval introduces additional workflow complexity.
* Permission shortcuts must be carefully translated into explicit operations.

### Neutral

* Authentication is intentionally deferred.
* Persistence model is intentionally deferred.
* REST integration is intentionally deferred.
* UI representation is intentionally deferred.

## Initial Implementation Direction

Start with a minimal Custos authorization kernel.

Phase 0:

```text
Actor
ActorId
ActorType
PermissionKey
ResourceRef
ResourceScope
AccessDecision
AccessDeniedException
SecurityEvaluationContext
PermissionEvaluator
AccessDecisionService
```

Phase 1:

```text
Role
RoleKey
RoleAssignment
PermissionGrant
PermissionResolver
BuiltInRoles
```

Phase 2:

```text
SitePermissionsService
ContentTypePermissionsService
ContentItemPermissionsService
RoleAssignmentPermissionsService
```

Phase 3:

```text
SecuredSiteService
SecuredContentTypeService
SecuredContentItemService
```

Phase 4:

```text
PermissionChangePlan
StepUpApproval
Olorin permission proposal flow
Audit integration
```

Do not start with:

```text
JWT
SAML
OIDC
passwords
REST endpoints
database persistence
full user management
```

Those concerns should be added after the domain authorization model is stable.

## Summary

Custos will be a domain authorization system, not a web security adapter.

It will determine whether a known `Actor` can perform a domain `Permission` on a scoped Codex resource, and it will explain the decision.

The core principle is:

```text
Actor + Permission + Resource + Context -> AccessDecision
```

Olorin may help humans understand and prepare permission changes, but only a properly authorized human or trusted non-agent actor with valid step-up approval may execute sensitive security changes.

