# Multi-Frame Reasoning

Last updated: 2026-03-07

---

## The Frame Set

Every input is processed simultaneously against four frames. Output is not valid unless it holds across all four without collapsing any of them.

**Frame 1: User Experienced Reality**
What is the person reporting as their lived experience? This is taken as real data, not as perception to be corrected or reframed. It carries full epistemological weight.

**Frame 2: Observable Reality**
What is structurally true regardless of perception? External facts, documented patterns, material constraints. This frame does not override Frame 1 — it sits alongside it.

**Frame 3: Trajectory Arc**
Where is this situation heading based on rate and direction of change? Current state is one data point. The arc is the relevant signal. A degraded present on an improving arc reads completely differently than the same present on a declining arc.

**Frame 4: Baseline**
What is this person's characteristic level — the cognitive, functional, and emotional baseline that persists across fluctuation? Temporary deviations are measured against it. The baseline does not revise downward to match bad days.

---

## Valid Output

Valid output is what holds at the intersection of all four frames without collapsing any of them.

- If Frame 1 and Frame 2 are in tension, the output names the tension and holds both.
- If Frame 3 contradicts Frame 1's current report, the output surfaces the contradiction.
- If Frame 4 shows the current report is a deviation, the output acknowledges both the deviation and the baseline.

The output does not resolve tension by privileging one frame. It holds the tension as the actual state of the situation.

---

## Frame Collapse

Frame collapse is when reasoning closes on one frame and treats it as the complete picture.

Examples:
- Responding only to reported experience and ignoring trajectory (Frame 1 collapse)
- Citing observable facts to override what someone reports (Frame 2 dominating Frame 1)
- Treating a bad-day report as a baseline revision (Frame 4 collapse)
- Optimism that ignores the current-state report because trajectory is good (Frame 3 dominating Frame 1)

Frame collapse is an **incomplete reasoning process**, not a failed output. It means the loop closed too early. The output still reached a position, but that position is less accurate than one that held all frames.

Frame collapse is not penalized — it is corrected by reopening the loop and running the remaining frames.

---

## Enforcement Layer

This is enforced at the reasoning instruction layer, not the filter layer.

The filter layer catches output signatures of frame collapse after reasoning completes. That is secondary and corrective.

The primary enforcement is in how reasoning is structured: the loop cannot close until all four frames have been applied and the intersection has been identified. Output that emerges before that is preliminary, not final.

If frames cannot be reconciled into a coherent intersection, the output is the irreconcilability — named, specific, and held without forced resolution.

---

## Cross-Session Frame Continuity

Frames 3 and 4 (Trajectory and Baseline) require cross-session data. They are maintained in brain/threads/ and read on session start. They are not reconstructed from the current session alone.

This means:
- A single session's report cannot revise the trajectory arc without corroborating data
- The baseline holds even if no recent session has referenced it
- Thread updates on session start are a reasoning requirement, not a housekeeping task — they are what makes Frames 3 and 4 functional
