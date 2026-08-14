# CLAUDE.md

# Job Tracker — Claude Development Instructions

## 1. Project

This is a small private Job Tracker application for approximately 5–8 users.

Each user has completely isolated data.

Users can only access their own:

- Applications
- Application progress
- Resumes
- Profile information

The goal is a simple, maintainable application rather than an over-engineered system.

---

# 2. Source of Truth

Before making changes, inspect:

- `MVP.md`
- `UI_DESIGN.md` if it exists
- Existing source code
- Existing database migrations
- Existing API contracts
- Relevant installed Claude skills

Priority:

1. Explicit user instruction
2. `MVP.md`
3. `UI_DESIGN.md`
4. Existing working architecture
5. Implementation assumptions

Do not silently change functional requirements.

If a decision would materially affect architecture, security, data model, or visual direction and the requirement is unclear, ask the user.

---

# 3. Installed Bencium Skills

This project uses Bencium Marketplace skills where appropriate.

Relevant skills include:

- `bencium-controlled-ux-designer`
- `bencium-impact-designer`
- `bencium-innovative-ux-designer`
- `design-audit`
- `typography`
- `bencium-code-conventions`
- `human-architect-mindset`
- `renaissance-architecture`
- `vanity-engineering-review`
- `loop`

Do not invoke every skill for every task.

Use the skill that matches the task.

---

# 4. Skill Usage Rules

## UI/UX Design

Use:

`bencium-controlled-ux-designer`

as the primary design decision skill.

Use it when deciding:

- Color palette
- Typography
- Layout
- Spacing
- Visual hierarchy
- Responsive behavior
- Accessibility
- Component treatment

Follow its ask-first protocol for major visual decisions.

Do not independently invent a complete visual system when the user has not approved one.

---

## UI Implementation

Use:

`bencium-impact-designer`

when implementing production-quality React interfaces.

The goal is to avoid generic AI-generated SaaS UI.

The implementation should feel intentionally designed.

Do not add visual complexity merely to make the interface look impressive.

---

## Alternative Visual Direction

`bencium-innovative-ux-designer` may be used when the user explicitly asks for a more experimental, distinctive, or creative visual direction.

Do NOT automatically use it for the Job Tracker.

The default Job Tracker direction is:

- Professional
- Clean
- Modern
- Minimal
- Easy to scan
- Practical

---

## Visual Audit

After a significant UI implementation, use:

`design-audit`

to review the interface.

The audit should focus on:

- Visual hierarchy
- Spacing
- Typography
- Consistency
- Responsive behavior
- Accessibility
- Unnecessary visual elements
- Overall polish

The audit must not change application functionality.

---

## Typography

Use:

`typography`

when designing or reviewing typography.

Maintain:

- Consistent hierarchy
- Appropriate font sizing
- Proper line height
- Consistent spacing
- Clear labels
- Readable body text

Do not introduce multiple unrelated fonts.

---

## Code Conventions

Use:

`bencium-code-conventions`

for React/TypeScript/Tailwind implementation conventions when applicable.

---

## Architecture

Use:

`human-architect-mindset`

for significant domain or architecture decisions.

Use:

`renaissance-architecture`

only when the problem genuinely benefits from first-principles architectural reasoning.

Do not use architectural skills as an excuse to overengineer the MVP.

---

## Vanity Engineering

Use:

`vanity-engineering-review`

when reviewing architecture or implementation for unnecessary complexity.

Ask:

> Does this solve a real requirement?

If not, remove it.

---

# 5. Development Workflow

Every feature follows:

```text
1. Understand
2. Inspect
3. Plan
4. Implement
5. Test
6. Verify
7. Summarize
```

<!-- NOTE: this file was truncated when written; sections after §5 are missing. -->
