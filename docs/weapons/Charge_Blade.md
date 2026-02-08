# Charge Blade (CB) ⚡ — Detailed Implementation Plan

**Overview:**
Charge Blade combines a sword-and-shield mode (for speed and charging phials) with an axe mode (high mobility/impact). The core unique system is the phial: attacks in sword mode charge phials stored in the shield; these phials can be consumed in powerful "discharge" attacks (Elemental Discharge / Ultra Burst / Super Amped Discharge) that trade stored resources for burst AOE / high single-target damage.

---

## 1) Canonical Core Mechanics
- Two modes: **Sword** (charge phials, fast oriented slashes) and **Axe** (heavy swings, reach change). Mode switching via transform action.
- **Phials (PhialStorage):** A discrete resource (usually 1–3 capacity) that is charged by hitting monsters in Sword mode and sometimes by special charge sequences (guard, counter) depending on generation.
- **Discharge (Elemental/Power Discharge):** Consumes some or all phials to produce either area or single-target burst attacks; varies by phial type (Elemental vs Power) with different damage formulas and effects.
- **Guard Points / Amped Guards:** Some attacks provide guard point behavior (frame window acting as a guard with counter properties) enabling advanced defensive play.
- **Super Discharge / Ultra Bursts:** High-commitment special moves that consume multiple phials and have long wind-ups but huge damage; often the defining CB finisher.
- **Phial Types & Variants:** Elemental Phials (deal elemental bursts on discharge), Power Phials (add raw damage), Impact/KO variants (rare). Generation-specific behaviours exist (e.g., phial recharge rules, guard-charge interactions).

---

## 2) Mapping to Better Combat (BC)
- **Mode State Machine:** Implement `ChargeBladeMode` (SWORD, AXE, TRANSFORMING) as a BC weapon state. Allow conditional combos based on mode.
- **PhialStorage Component:** Add server-authoritative `PhialStorage` component with methods: addPhials(player, amount, type), consumePhials(player, amount), getPhialCount(), setCapacity(). Expose sync packets and client prediction hooks for HUD display.
- **Charge Action:** `PhialChargeAction` triggered by specific sword-mode attacks or charge sequences. Charge should be validated and batched server-side to avoid spoofing.
- **Discharge Actions:** `ElementalDischargeAction` and `SuperDischargeAction` that consume phials and create server-validated area/targeted damage events (use the server's AoE damage utilities; do not trust client to spawn AoE effects).
- **Guard Point & Counter:** Implement `GuardPointAction` frames for certain attack animations; hits during these frames can trigger guard/counter behavior and optionally charge phials.
- **Attack Descriptors:** Use phased attack descriptors (phase timings + hitboxes + motion values) for discharges (e.g., initial burst, trailing explosion) so animation syncing and multi-hit damage is exact and testable.

---

## 3) GeckoLib — Models & Animations
- **Animations to Author:** `idle_sword`, `idle_axe`, `sword_combo_*`, `axe_combo_*`, `phial_charge_fx` (per-phial fill animation), `charge_shield`, `elemental_discharge_windup`, `elemental_discharge_release`, `super_discharge_windup`, `super_discharge_release`, `guard_point_frame`.
- **VFX:** per-phial glow UI, pulse when full, discharge explosion particle systems (elemental-specific), camera shake and audio cues for high-impact releases.
- **Blend Transitions:** Smooth morph animations between Sword ↔ Axe; VFX layers driven by phial counts to visually show charge state.

---

## 4) Data & Config (tunable)
- Schema candidates (JSON):
  - phials: { capacity: 3, types: { "power": { damageMultiplier, dischargeCost, aoeRadius }, "elemental": {...} }, chargePerHit: 1, chargeSources: ["sword_hit", "guard_point"] }
  - discharge: { baseDamage, motionPhases: [...], consumesAll: bool, cooldownMs, vulnerabilityWindowMs }
  - guardPoint: { activeFrames: [startMs,endMs], chargesPhialOnHit: bool }
- Support generation overrides with rulesets `default`, `mhwilds`, `mhw`, etc., to capture behavior differences (e.g., charge rules, discharge potency, whether phials are capped differently).

---

## 5) Implementation Tasks (small steps)
1. **Data files (S):** Create `data/mhweapons/charge_blade/default.json` and `data/mhweapons/charge_blade/mhwilds.json` with example phial and discharge configs.
2. **PhialStorage component (M):** Implement server-authoritative storage with packet sync and client prediction for HUD (UI should show predicted count and correct after server ack).
3. **BC: Mode & Actions (M):** Implement `ChargeBladeMode` state, `PhialChargeAction`, `ElementalDischargeAction`, `SuperDischargeAction`, and `GuardPointAction` with unit tests for transitions.
4. **Item class (M):** `ChargeBladeItem` hooking inputs (attack, transform, discharge) and reading `data/` config; expose read-only state for UI.
5. **GL: Visuals & Animations (M):** Add placeholder GL animations and phial VFX to iterate quickly.
6. **Server AoE validation tests (M):** Test discharge AoE damage events for correctness across latencies and confirm no client-side spoofing possible.
7. **Playtest & Balancing (S):** Tuning session to tweak phial gains per-hit, discharge cost, and cooldown to match intended generation behavior.

---

## 6) Cross-Mod & Integration Notes
- **Damage Mods / Buffs:** Provide an event hook for other mods to modify discharge damage or phial gain (e.g., skills or enchantment mods). Use existing `SkillModifier` concept for consistent stacking.
- **UI Mods:** Expose a light-weight HUD API and a capability to plug in custom HUD mods for phial info and discharge cooldowns.
- **GeckoLib & Animation Packs:** Keep animations data-driven (names and timing) so third-party GL packs can replace or augment visuals.
- **Persistence & Sync:** Ensure phial state is part of player session persist data where reasonable (e.g., in singleplayer or reconnect logic), or optionally reset on death/respawn via config.

---

## 7) Risks & Mitigations
- **Risk: AoE / Discharge exploits** — Mitigate by authoritative server-side damage computation and by disallowing client-requested AoE spawns to directly apply damage.
- **Risk: Phial Desync** — Mitigate by using conservative client prediction (increment locally on expected hits), server confirmation messages, and reconciliation logic that triggers a VFX correction rather than abrupt gameplay changes.
- **Risk: Balancing Complexity** — Mitigate by providing easy tuning in `data/` and snapshotting default rulesets for common generation flavors.

---

## 8) Acceptance Criteria
- Phial charging is robust and consistent across network conditions and changes in gauge are correctly shown to clients with minimal perceived lag.
- Discharge actions consume phials and produce server-authorized damage with predictable multi-phase motion values.
- Mode toggling and combo transitions are smooth and repeatable, with animations and effects synchronized across clients.

---

## 9) Next Steps
- Create sample `data/mhweapons/charge_blade/*.json` rulesets and scaffold `PhialStorage` and basic discharge action server classes so we can run playtests.

---

*Shall I scaffold the `PhialStorage` class and a simple `ElementalDischargeAction` (server-side stub + unit tests) now, or generate the sample ruleset JSONs first?*