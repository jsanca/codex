# Codex Workflow Model (MVP)

Workflows orchestrate **how knowledge evolves** inside Codex.

While the content model defines *what knowledge is*, workflows define *how knowledge moves*, *who can act on it*, and *how actions are recorded*.

In Codex, workflows are designed as **graph-based processes** capable of representing approvals, automated tasks, integrations, and long‑running operations.

Every significant action performed in the platform should pass through a workflow and leave a rich historical trace.

---

# 1. Core Principles of Workflows

1. **Everything is an execution**

   Every operation performed through a workflow generates an execution history.

2. **Workflows are graphs**

   Workflows are modeled as directed graphs rather than simple state machines.

3. **History is first‑class**

   Every step in a workflow is recorded as an event.

4. **Extensibility by design**

   Workflows must allow integration with scripts, services, and external systems.

5. **Async by default**

   Workflows must support background execution, waiting states, and external signals.

---

# 2. Workflow Definition

A workflow definition describes the structure of a process.

## 2.1 WorkflowDefinition

Fields:

- `id`
- `name`
- `description`
- `version`
- `siteId` (optional if shared via `_system`)
- `nodes[]`
- `transitions[]`

Invariants:

- definitions are immutable once activated
- new changes create a new version

---

# 3. Workflow Graph Model

Workflows are represented as a **directed graph**.

## 3.1 Node

A node represents a step in the workflow.

Fields:

- `id`
- `type`
- `name`
- `config`

Node types supported in MVP:

### HumanTask

A task that requires action from a user.

Examples:

- content approval
- editorial review
- manual publishing

### ServiceTask

Executes backend logic.

Examples:

- call an external API
- run a script
- trigger a system action

### ScriptTask

Executes logic within the **Scriptorium**.

Used for custom logic defined by administrators.

### Gateway

Represents conditional branching.

Transitions leaving the gateway contain conditions.

### Wait

Pauses the workflow until an external signal is received.

Signals may come from:

- external webhooks
- user actions
- internal system events

### End

Marks workflow completion.

---

# 4. Transitions

Transitions connect nodes.

Fields:

- `id`
- `fromNode`
- `toNode`
- `condition` (optional)

Conditions may evaluate:

- workflow context
- content attributes
- user attributes

Example:

```
if content.price > 10000
```

---

# 5. Workflow Execution

Each run of a workflow produces an execution record.

## 5.1 WorkflowExecution

Fields:

- `executionId`
- `workflowId`
- `workflowVersion`
- `siteId`
- `entityRef` (e.g. ContentItem id)
- `status` (`RUNNING | WAITING | COMPLETED | FAILED`)
- `startedAt`
- `completedAt`

---

# 6. Workflow Tokens

Execution moves through the graph using tokens.

## 6.1 Token

Fields:

- `tokenId`
- `executionId`
- `currentNode`
- `state`

Tokens allow workflows to support:

- branching
- parallel flows
- loops

---

# 7. Execution History

Codex records a detailed history of workflow activity.

Events may include:

- `WorkflowStarted`
- `NodeEntered`
- `ActionPerformed`
- `NodeCompleted`
- `TransitionTaken`
- `ExternalSignalReceived`
- `WorkflowCompleted`
- `WorkflowFailed`

This event stream enables:

- auditing
- debugging
- analytics

---

# 8. Background Processing

Workflow execution may run asynchronously.

A workflow runner component processes pending work.

Responsibilities:

- execute service tasks
- resume waiting workflows
- retry failed operations

Retries must be **idempotent**.

---

# 9. Signals

Signals resume paused workflows.

Examples:

- webhook callback
- user approval
- scheduled trigger

Signal fields:

- `signalId`
- `executionId`
- `payload`

---

# 10. Streaming Operations

Some workflows may operate on large sets of content.

To support this efficiently Codex introduces streaming operations.

Concepts:

- `ItemSelector` — query describing the items to process
- `StreamCursor` — checkpoint position
- `BatchSize`

Nodes may process items incrementally rather than loading all items into memory.

---

# 11. Merge Operations

Workflows may combine multiple streams of data.

Merge strategies may include:

- join by key
- completion synchronization
- window‑based merging

This allows complex content processing pipelines.

---

# 12. Example Workflow

Example: Content Approval

```
Draft Saved
     |
     v
Editorial Review (HumanTask)
     |
     v
Approved? (Gateway)
   /   \
  yes   no
  |      |
  v      v
Publish  Back to Draft
  |
  v
End
```

---

# 13. Future Extensions

Potential future capabilities:

- visual workflow editor
- BPMN compatibility
- sub‑workflows
- distributed execution
- workflow replay and recovery

These features should build on the core execution model defined here.

---

Workflows turn Codex from a passive content repository into a **living system of knowledge evolution**.
