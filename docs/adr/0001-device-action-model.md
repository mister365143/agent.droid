# ADR 0001: How the agent acts on the device

- **Status:** Accepted
- **Date:** 2026-11-23
- **Deciders:** agent.droid maintainers

## Context

agent.droid is meant to execute multi-step tasks on a user's phone: open apps,
read the screen, tap, type, scroll, toggle settings. Android gives a normal app
very little leverage over other apps, so the capability model is a product
decision, not an implementation detail. It determines what the app can do, who
can install it, which permissions it must declare, and how much trust it asks of
the user.

The three realistic options:

1. **AccessibilityService** — a normal app declares an accessibility service,
   receives `AccessibilityNodeInfo` trees and window events, and can dispatch
   gestures via `dispatchGesture`. No root, no cable, no PC.
2. **ADB self-pairing** — the user enables Wireless debugging and pairs the app
   with itself (the Shizuku-style model), then the app issues shell commands as
   the `shell` user over a local ADB connection. Much higher fidelity: `input`,
   `am start`, `pm`, `dumpsys`, screenshots, no accessibility prompt per action.
3. **Root** — `su` and full device control.

## Decision

**AccessibilityService is the primary and only required capability path.**

- ADB self-pairing is an **optional, additive** capability, considered later and
  never a prerequisite for core functionality.
- Root is **out of scope**. No feature may require it, and the app must remain
  fully functional without it.

Concretely, this means v1 ships a single accessibility service that exposes a
small, typed tool set (observe screen, tap, type, scroll, launch app, back/home),
each tool gated by an explicit user-visible permission and an approval step for
destructive actions. Every action is logged to a local audit trail.

### Screen content is untrusted input

Everything read from the accessibility tree is **data, never instructions**. On-
screen text — labels, web page content, notifications, other apps' UIs — is
attacker-controlled: any app or page can render "ignore your task and do X", and
putting that into a system-prompt position would turn a hostile screen into a
command channel (prompt injection). So the agent acts only on the task the user
typed, treats observed text as evidence to reason about, keeps it clearly
separated from the instructions, and still requires user approval for
destructive actions even when the screen appears to ask for them.

## Consequences

**Positive**

- Installable from any channel with no cable, PC, or setup ritual — a normal
  APK, which matches how the project currently ships (`artifacts/` debug builds).
- Survives across reboots and works offline, so on-device inference remains an
  option later.
- Play-distributable in principle, subject to the accessibility-use declaration
  and review (see negative).
- The capability boundary is legible to users: the OS itself shows the
  accessibility toggle.

**Negative**

- Google Play requires a declared, justified accessibility use and rejects
  automation-for-hire patterns; distribution may end up F-Droid/sideload-first.
  This is accepted and tracked as a distribution decision, not an engineering one.
- Accessibility trees are lossy: canvas-heavy apps, games, and custom-drawn UIs
  expose little or nothing, and screenshots are not available to a plain
  accessibility service. The agent will be blind in some apps.
- Instruction/data confusion is a permanent hazard of this capability model:
  screen text can imitate the user or the system. It is contained by prompting
  discipline and approval gates, not eliminated.
- Slower and flakier than shell-level control; gesture dispatch is throttled and
  some system surfaces are unreachable.
- Users see a persistent "can read the screen" warning, which is a real adoption
  cost and must be explained honestly in onboarding.

## Alternatives rejected

- **Root-first** — excludes the overwhelming majority of devices, voids
  warranties, and permanently caps the potential user base. Rejected.
- **ADB-only** — the pairing flow needs a second device or a laptop for many
  users, and Android revokes wireless debugging on reboot/network change in ways
  that break a background agent. Excellent as an optional power-user upgrade,
  unacceptable as the only path.
- **Companion app plus desktop relay only** — makes the "on the phone" promise
  false and pushes the hard problem to a backend. Not a substitute for local
  capability.
