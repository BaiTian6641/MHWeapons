package org.example.common.combat.weapon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.example.common.capability.player.PlayerCombatState;
import org.example.common.capability.player.PlayerWeaponState;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.common.data.WeaponData;
import org.example.common.data.WeaponDataResolver;
import org.example.common.entity.EchoBubbleEntity;
import org.example.common.network.ModNetwork;
import org.example.common.network.packet.PlayAttackAnimationS2CPacket;
import org.example.common.network.packet.PlayerWeaponStateS2CPacket;
import org.example.registry.MHWeaponsItems;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Hunting Horn handler — Wilds-accurate implementation.
 * <p>
 * Manages note queue, song pattern matching, melody stocking/performance,
 * echo bubble spawning, Performance Beat/Encore combo chains, Hilt Stab counter,
 * and Iceborne-style Echo Attack (resonance through nearby bubbles).
 * <p>
 * References:
 * <ul>
 *   <li>MH Wilds official manual — Hunting Horn controls & mechanics</li>
 *   <li>MH World: Iceborne — Echo Attack (Echo Waves via active Echo Bubbles)</li>
 * </ul>
 */
@SuppressWarnings({"null", "deprecation"})
public final class HuntingHornHandler {
    private HuntingHornHandler() {}

    // ── constants ───────────────────────────────────────────────────────

    private static final int NOTE_ACTION_TICKS = 10;
    private static final int RECITAL_ACTION_TICKS = 12;
    private static final int PERFORMANCE_BEAT_TICKS = 10;
    private static final int ENCORE_ACTION_TICKS = 14;
    private static final int ECHO_BUBBLE_ACTION_TICKS = 12;
    private static final int HILT_STAB_ACTION_TICKS = 8;
    private static final int HILT_STAB_COUNTER_WINDOW = 6;
    private static final int ECHO_ATTACK_RADIUS = 8;
    private static final float ECHO_RESONANCE_DAMAGE = 3.0f;

    // ── internal records ────────────────────────────────────────────────

    record BubbleDef(MobEffect effect, int amplifier, int duration, float radius) {}

    record SongMatch(int melodyId, MobEffect effect, int amplifier, int duration, float radius) {}

    record MelodyPlay(int melodyId, String songId, MobEffect effect, int amplifier, int duration, float radius) {}

    // ── helpers ─────────────────────────────────────────────────────────

    private static void setAction(PlayerCombatState combatState, String key, int ticks) {
        combatState.setActionKey(key);
        combatState.setActionKeyTicks(ticks);
    }

    private static void syncState(Player player, PlayerWeaponState state) {
        if (player instanceof ServerPlayer sp) {
            if (!state.isDirty()) return;
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new PlayerWeaponStateS2CPacket(sp.getId(), state.serializeNBT()));
            state.clearDirty();
        }
    }

    private static void sendAnimation(Player player, String animKey, String actionKey, int actionTicks) {
        if (player instanceof ServerPlayer sp) {
            float length = WeaponDataResolver.resolveFloat(player, "animationTiming", "length", 16.666666f);
            float upswing = WeaponDataResolver.resolveFloat(player, "animationTiming", "upswing", 0.55f);
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp),
                    new PlayAttackAnimationS2CPacket(sp.getId(), animKey, length, upswing, 1.0f, actionKey, actionTicks));
        }
    }

    /**
     * AoE hit around the player (for recitals, performance beats, encore).
     * Returns true if at least one entity was hit.
     */
    private static boolean applyAoEHit(Player player, double radius, float damageMultiplier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return false;
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (Math.max(1.0, base) * damageMultiplier);
        AABB box = player.getBoundingBox().inflate(radius, 1.0, radius);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive());
        boolean hit = false;
        for (LivingEntity target : targets) {
            target.hurt(player.damageSources().playerAttack(player), damage);
            hit = true;
        }
        return hit;
    }

    /**
     * Single-target frontal hit (for hilt stab, focus strike).
     */
    @Nullable
    private static LivingEntity applyFrontalHit(Player player, double range, float damageMultiplier) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return null;
        Vec3 look = player.getLookAngle();
        Vec3 horiz = new Vec3(look.x, 0.0, look.z);
        if (horiz.lengthSqr() < 0.0001) horiz = new Vec3(0.0, 0.0, 1.0);
        horiz = horiz.normalize();
        Vec3 start = player.position().add(0.0, 0.9, 0.0);
        Vec3 end = start.add(horiz.scale(range));
        AABB box = new AABB(start, end).inflate(1.0, 0.8, 1.0);
        double baseDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float damage = (float) (Math.max(1.0, baseDmg) * damageMultiplier);

        LivingEntity target = serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive())
                .stream()
                .min((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)))
                .orElse(null);
        if (target != null) {
            target.hurt(player.damageSources().playerAttack(player), damage);
        }
        return target;
    }

    // ── note queue helpers ──────────────────────────────────────────────

    private static int[] lastNotes(PlayerWeaponState state, int max) {
        int[] notes = new int[] {
                state.getHornNoteA(), state.getHornNoteB(), state.getHornNoteC(),
                state.getHornNoteD(), state.getHornNoteE()
        };
        int count = Math.min(state.getHornNoteCount(), notes.length);
        int size = Math.min(max, count);
        int[] out = new int[size];
        int start = Math.max(0, count - size);
        for (int i = 0; i < size; i++) {
            out[i] = notes[start + i];
        }
        return out;
    }

    private static int[] readPattern(com.google.gson.JsonArray array) {
        int[] pattern = new int[array.size()];
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = array.get(i).getAsInt();
        }
        return pattern;
    }

    private static boolean matchesPattern(int[] last, int[] pattern) {
        if (pattern.length == 0 || last.length < pattern.length) return false;
        int offset = last.length - pattern.length;
        for (int i = 0; i < pattern.length; i++) {
            if (last[offset + i] != pattern[i]) return false;
        }
        return true;
    }

    // ── song resolution ─────────────────────────────────────────────────

    @Nullable
    private static SongMatch resolveSongMatch(WeaponData data, PlayerWeaponState state) {
        if (data == null) return null;
        var json = data.getJson();
        if (!json.has("songs") || !json.get("songs").isJsonArray()) return null;
        int queueSize = json.has("noteQueueSize") ? json.get("noteQueueSize").getAsInt() : 5;
        int[] last = lastNotes(state, queueSize);
        var songs = json.getAsJsonArray("songs");
        for (int i = 0; i < songs.size(); i++) {
            var song = songs.get(i).getAsJsonObject();
            if (!song.has("pattern") || !song.get("pattern").isJsonArray()) continue;
            int[] pattern = readPattern(song.getAsJsonArray("pattern"));
            if (!matchesPattern(last, pattern)) continue;
            int melodyId = i + 1;
            String bubbleId = song.has("bubble") ? song.get("bubble").getAsString() : null;
            BubbleDef bubble = resolveBubbleDef(json, bubbleId);
            if (bubble != null) {
                return new SongMatch(melodyId, bubble.effect, bubble.amplifier, bubble.duration, bubble.radius);
            }
        }
        return null;
    }

    /**
     * Auto-match: checked on every note input (Wilds instant-stock behavior).
     */
    @Nullable
    private static SongMatch resolveAutoSongMatch(WeaponData data, PlayerWeaponState state) {
        if (data == null) return null;
        var json = data.getJson();
        if (!json.has("songs") || !json.get("songs").isJsonArray()) return null;
        int queueSize = json.has("noteQueueSize") ? Math.max(1, json.get("noteQueueSize").getAsInt()) : 5;
        int[] last = lastNotes(state, queueSize);
        if (last.length == 0) return null;
        var songs = json.getAsJsonArray("songs");
        for (int i = 0; i < songs.size(); i++) {
            var song = songs.get(i).getAsJsonObject();
            if (!song.has("pattern") || !song.get("pattern").isJsonArray()) continue;
            int[] pattern = readPattern(song.getAsJsonArray("pattern"));
            if (pattern.length < 2 || pattern.length > 4) continue;
            if (!matchesPattern(last, pattern)) continue;
            int melodyId = i + 1;
            String bubbleId = song.has("bubble") ? song.get("bubble").getAsString() : null;
            BubbleDef bubble = resolveBubbleDef(json, bubbleId);
            if (bubble != null) {
                return new SongMatch(melodyId, bubble.effect, bubble.amplifier, bubble.duration, bubble.radius);
            }
        }
        return null;
    }

    @Nullable
    private static BubbleDef resolveBubbleDef(com.google.gson.JsonObject json, @Nullable String id) {
        if (!json.has("echoBubbles") || !json.get("echoBubbles").isJsonArray()) return null;
        var bubbles = json.getAsJsonArray("echoBubbles");
        for (int i = 0; i < bubbles.size(); i++) {
            var bubble = bubbles.get(i).getAsJsonObject();
            if (id != null && bubble.has("id") && !id.equals(bubble.get("id").getAsString())) continue;
            String effectId = bubble.has("effect") ? bubble.get("effect").getAsString() : "minecraft:speed";
            ResourceLocation effectKey = ResourceLocation.tryParse(effectId);
            if (effectKey == null) continue;
            MobEffect effect = BuiltInRegistries.MOB_EFFECT.get(effectKey);
            int amp = bubble.has("amplifier") ? bubble.get("amplifier").getAsInt() : 0;
            int duration = bubble.has("duration") ? bubble.get("duration").getAsInt() : 200;
            float radius = bubble.has("radius") ? bubble.get("radius").getAsFloat() : 3.5f;
            return new BubbleDef(effect, amp, duration, radius);
        }
        return null;
    }

    @Nullable
    private static BubbleDef resolveFixedEchoBubble(WeaponData data) {
        if (data == null) return null;
        var json = data.getJson();
        if (json.has("fixedEchoBubble") && json.get("fixedEchoBubble").isJsonPrimitive()) {
            return resolveBubbleDef(json, json.get("fixedEchoBubble").getAsString());
        }
        return resolveBubbleDef(json, null);
    }

    // ── melody play/stock ───────────────────────────────────────────────

    @Nullable
    private static MelodyPlay resolveMelodyPlay(WeaponData data, PlayerWeaponState state) {
        if (data == null || state.getHornMelodyCount() <= 0) return null;
        int index = Math.max(0, Math.min(state.getHornMelodyIndex(), state.getHornMelodyCount() - 1));
        int melodyId = state.consumeHornMelodyAt(index);
        if (melodyId <= 0) return null;
        return resolveMelodyById(data, melodyId);
    }

    @Nullable
    static MelodyPlay resolveMelodyById(WeaponData data, int melodyId) {
        if (data == null || melodyId <= 0) return null;
        var json = data.getJson();
        if (!json.has("songs") || !json.get("songs").isJsonArray()) return null;
        var songs = json.getAsJsonArray("songs");
        int idx = melodyId - 1;
        if (idx < 0 || idx >= songs.size()) return null;
        var song = songs.get(idx).getAsJsonObject();
        String songId = song.has("id") ? song.get("id").getAsString() : "melody_" + melodyId;
        String bubbleId = song.has("bubble") ? song.get("bubble").getAsString() : null;
        BubbleDef bubble = resolveBubbleDef(json, bubbleId);
        if (bubble == null) return null;
        return new MelodyPlay(melodyId, songId, bubble.effect, bubble.amplifier, bubble.duration, bubble.radius);
    }

    // ── buff application ────────────────────────────────────────────────

    private static void applyMelodyEffect(Player player, String songId, MobEffect effect,
                                          int amplifier, int duration, float radius) {
        AABB box = player.getBoundingBox().inflate(radius, 1.0, radius);
        for (Player target : player.level().getEntitiesOfClass(Player.class, box)) {
            if ("healing_medium_depoison".equals(songId)) {
                target.removeEffect(MobEffects.POISON);
            }
            target.addEffect(new MobEffectInstance(effect, duration, amplifier, false, true));
        }
    }

    private static void applyHornSongBuff(PlayerWeaponState state, String songId, int duration) {
        if (songId == null) return;
        switch (songId) {
            case "attack_up_small" -> state.setHornAttackSmallTicks(duration);
            case "attack_up_large" -> state.setHornAttackLargeTicks(duration);
            case "defense_up_large" -> state.setHornDefenseLargeTicks(duration);
            case "melody_hit" -> state.setHornMelodyHitTicks(duration);
            default -> {}
        }
    }

    // ── echo bubble spawning ────────────────────────────────────────────

    private static void spawnEchoBubble(Player player, MobEffect effect, int amplifier,
                                        int duration, float radius) {
        var bubble = new EchoBubbleEntity(
                MHWeaponsItems.ECHO_BUBBLE.get(),
                player.level(),
                BuiltInRegistries.MOB_EFFECT.getId(effect),
                amplifier, duration, radius
        );
        bubble.setPos(player.getX(), player.getY() + 0.1, player.getZ());
        player.level().addFreshEntity(bubble);
    }

    /**
     * Iceborne Echo Attack: When a Performance is done near active Echo Bubbles,
     * each bubble resonates — dealing AoE damage to monsters in its radius.
     * This is the "Echo Wave" mechanic from MHW:I, adapted for Wilds.
     */
    private static void triggerEchoResonance(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        AABB searchBox = player.getBoundingBox().inflate(ECHO_ATTACK_RADIUS, 4.0, ECHO_ATTACK_RADIUS);
        List<EchoBubbleEntity> nearbyBubbles = serverLevel.getEntitiesOfClass(
                EchoBubbleEntity.class, searchBox);
        if (nearbyBubbles.isEmpty()) return;

        double baseDmg = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float resonanceDmg = (float) (Math.max(1.0, baseDmg) * ECHO_RESONANCE_DAMAGE);

        for (EchoBubbleEntity bubble : nearbyBubbles) {
            float r = bubble.getRadius();
            AABB bubbleBox = bubble.getBoundingBox().inflate(r, 1.0, r);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, bubbleBox, e -> e != player && e.isAlive() && !(e instanceof Player));
            for (LivingEntity target : targets) {
                target.hurt(player.damageSources().playerAttack(player), resonanceDmg);
            }
            // Visual feedback: spawn particles at bubble center
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.SONIC_BOOM,
                    bubble.getX(), bubble.getY() + 0.5, bubble.getZ(),
                    3, r * 0.3, 0.3, r * 0.3, 0.0);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MAIN DISPATCH
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Main entry point — called from {@link WeaponActionHandler#handleAction}.
     * Routes Hunting Horn actions to specific sub-handlers.
     */
    public static void handleAction(WeaponActionType action, boolean pressed,
                                    Player player, PlayerCombatState combatState,
                                    PlayerWeaponState weaponState) {
        if (!pressed || weaponState == null || combatState == null) return;

        switch (action) {
            case WEAPON, WEAPON_ALT, HORN_NOTE_BOTH -> handleNoteAttack(action, player, combatState, weaponState);
            case SPECIAL -> handleSpecialAction(player, combatState, weaponState);
            default -> {}
        }

        syncState(player, weaponState);
    }

    // ── NOTE ATTACKS (Left Swing / Right Swing / Backwards Strike) ──────

    /**
     * Handles all note-generating attacks.
     * <p>
     * Wilds manual mapping:
     * <ul>
     *   <li>Left Swing (Note 1): LMB</li>
     *   <li>Right Swing (Note 2): RMB</li>
     *   <li>Backwards Strike / Overhead Smash (Note 3): LMB+RMB or Shift</li>
     * </ul>
     * <p>
     * After each note, auto-checks for song pattern matches (Wilds auto-stock).
     * Hilt Stab variant: pressing back + note during combo creates a quick counter window.
     */
    private static void handleNoteAttack(WeaponActionType action, Player player,
                                         PlayerCombatState combatState, PlayerWeaponState weaponState) {
        // --- Determine which note ---
        int note = resolveNote(action, player);
        if (note <= 0) return;

        boolean isInCombo = combatState.getActionKeyTicks() > 0
                && combatState.getActionKey() != null
                && (combatState.getActionKey().startsWith("note_")
                    || "hilt_stab".equals(combatState.getActionKey())
                    || "flourish".equals(combatState.getActionKey()));

        // --- Hilt Stab: back + any note during combo ---
        if (isInCombo && player.isShiftKeyDown()) {
            handleHiltStab(note, player, combatState, weaponState);
            return;
        }

        // --- Flourish: forward + RMB during combo (produces Note 2, chains further) ---
        boolean isForwardAlt = (action == WeaponActionType.WEAPON_ALT) && isInCombo && !player.isShiftKeyDown();
        if (isForwardAlt) {
            handleFlourish(note, player, combatState, weaponState);
            return;
        }

        // --- Standard note attack ---
        // Block during active animation (combo branches like Hilt Stab and Flourish are handled above)
        if (combatState.getActionKeyTicks() > 0) {
            return;
        }
        weaponState.addHornNote(note);
        String actionKey = noteActionKey(note);
        setAction(combatState, actionKey, NOTE_ACTION_TICKS);

        // Alt animation for RMB attacks
        if (action == WeaponActionType.WEAPON_ALT) {
            String fallbackAnim = WeaponDataResolver.resolveString(player, "animationOverrides", "hunting_horn_note",
                    "bettercombat:two_handed_slash_horizontal_right");
            String animId = BetterCombatAnimationBridge.resolveComboAnimationServer(player, 0, fallbackAnim);
            sendAnimation(player, animId, actionKey, NOTE_ACTION_TICKS);
        }

        // Auto-song match (Wilds behavior: songs stock automatically)
        checkAutoSongMatch(player, weaponState);
    }

    private static int resolveNote(WeaponActionType action, Player player) {
        if (action == WeaponActionType.HORN_NOTE_BOTH) return 3;
        if (action == WeaponActionType.WEAPON) {
            return player.isShiftKeyDown() ? 3 : 1;
        }
        if (action == WeaponActionType.WEAPON_ALT) {
            return player.isShiftKeyDown() ? 3 : 2;
        }
        return 0;
    }

    private static String noteActionKey(int note) {
        return switch (note) {
            case 1 -> "note_one";
            case 2 -> "note_two";
            default -> "note_three";
        };
    }

    private static void checkAutoSongMatch(Player player, PlayerWeaponState weaponState) {
        WeaponData data = WeaponDataResolver.resolve(player);
        SongMatch autoMatch = resolveAutoSongMatch(data, weaponState);
        if (autoMatch != null) {
            weaponState.addHornMelody(autoMatch.melodyId);
        }
    }

    // ── HILT STAB (back + note during combo → quick note + counter window) ─

    /**
     * Wilds manual: "A quick attack that produces a note. Can be used after a number of
     * different attacks. If you time this well with a monster's incoming attack, you can
     * perform a counter attack while reducing damage you receive."
     */
    private static void handleHiltStab(int note, Player player,
                                       PlayerCombatState combatState, PlayerWeaponState weaponState) {
        weaponState.addHornNote(note);
        setAction(combatState, "hilt_stab", HILT_STAB_ACTION_TICKS);

        // Counter window: grants Special Guard ticks (reduces incoming damage)
        weaponState.setHornSpecialGuardTicks(HILT_STAB_COUNTER_WINDOW);

        // Apply a quick forward-poke hit
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "hilt_stab", 0.6f);
        applyFrontalHit(player, 2.5, mv);

        String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "hilt_stab",
                "bettercombat:two_handed_stab");
        sendAnimation(player, animId, "hilt_stab", HILT_STAB_ACTION_TICKS);

        checkAutoSongMatch(player, weaponState);
    }

    // ── FLOURISH (forward + RMB during combo → Note 2, chains) ──────────

    /**
     * Wilds manual: "An attack that produces note 2 on the musical staff.
     * Pressing further inputs during the attack will allow you to produce another note."
     */
    private static void handleFlourish(int note, Player player,
                                       PlayerCombatState combatState, PlayerWeaponState weaponState) {
        weaponState.addHornNote(note);
        setAction(combatState, "flourish", NOTE_ACTION_TICKS);

        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "flourish", 0.8f);
        applyFrontalHit(player, 3.0, mv);

        String animId = WeaponDataResolver.resolveString(player, "animationOverrides", "flourish",
                "bettercombat:two_handed_slash_horizontal_left");
        sendAnimation(player, animId, "flourish", NOTE_ACTION_TICKS);

        checkAutoSongMatch(player, weaponState);
    }

    // ── SPECIAL ACTION (Perform / Echo Bubble / Performance Beat / Encore) ─

    /**
     * Routes the Special key based on context:
     * <ul>
     *   <li><b>Shift + Special</b>: Echo Bubble placement (Wilds R2+X)</li>
     *   <li><b>Special during Performance</b>: Performance Beat → Encore chain</li>
     *   <li><b>Special (default)</b>: Perform (play stocked melodies)</li>
     * </ul>
     */
    private static void handleSpecialAction(Player player, PlayerCombatState combatState,
                                            PlayerWeaponState weaponState) {
        // --- Echo Bubble: Shift + Special ---
        if (player.isShiftKeyDown()) {
            handleEchoBubble(player, combatState, weaponState);
            return;
        }

        String currentAction = combatState.getActionKey();
        int currentTicks = combatState.getActionKeyTicks();
        boolean inPerformance = currentTicks > 0 && currentAction != null
                && ("recital".equals(currentAction)
                    || "performance_beat".equals(currentAction)
                    || currentAction.startsWith("performance_beat_"));

        // --- Performance Beat: Special during active Perform ---
        if (inPerformance) {
            handlePerformanceBeat(player, combatState, weaponState);
            return;
        }

        // --- Encore: Both buttons during performance (LMB+RMB equivalent) ---
        // This is handled via note_three input during performance in handleNoteAttack,
        // but also accessible as a dedicated SPECIAL follow-up after performance beats.

        // --- Default: Perform / Recital ---
        handlePerform(player, combatState, weaponState);
    }

    // ── PERFORM (R2 — play stocked melodies) ────────────────────────────

    /**
     * Wilds manual: "A special attack that activates Melody Effects. Multiple stocked
     * melodies will be Performed in order."
     * <p>
     * Also triggers Iceborne Echo Attack: nearby Echo Bubbles resonate with damage.
     */
    private static void handlePerform(Player player, PlayerCombatState combatState,
                                      PlayerWeaponState weaponState) {
        setAction(combatState, "recital", RECITAL_ACTION_TICKS);
        WeaponData data = WeaponDataResolver.resolve(player);

        // 1. Try to match a new song from current notes
        SongMatch match = resolveSongMatch(data, weaponState);
        if (match != null) {
            weaponState.addHornMelody(match.melodyId);
            weaponState.setHornBuffTicks(match.duration);
            weaponState.clearHornNotes();
        }

        // 2. Try Encore (re-perform last melody with enhanced effect)
        if (match == null && weaponState.getHornLastMelodyEnhanceTicks() > 0
                && weaponState.getHornLastMelodyId() > 0) {
            MelodyPlay last = resolveMelodyById(data, weaponState.getHornLastMelodyId());
            if (last != null) {
                int amp = last.amplifier + 1;
                int duration = last.duration + (last.duration / 2);
                applyMelodyEffect(player, last.songId, last.effect, amp, duration, last.radius);
                applyHornSongBuff(weaponState, last.songId, duration);
                weaponState.setHornBuffTicks(duration);
                weaponState.setHornMelodyPlayTicks(RECITAL_ACTION_TICKS);
                weaponState.setHornLastMelodyEnhanceTicks(0);
                weaponState.clearHornNotes();

                // Recital AoE hit
                float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "recital", 1.4f);
                applyAoEHit(player, 3.0, mv);

                // Iceborne Echo Attack: resonate nearby bubbles
                triggerEchoResonance(player);
                return;
            }
        }

        // 3. Play the next stocked melody
        MelodyPlay play = resolveMelodyPlay(data, weaponState);
        if (play != null) {
            applyMelodyEffect(player, play.songId, play.effect, play.amplifier, play.duration, play.radius);
            applyHornSongBuff(weaponState, play.songId, play.duration);
            weaponState.setHornBuffTicks(play.duration);
            weaponState.setHornMelodyPlayTicks(RECITAL_ACTION_TICKS);
            weaponState.setHornLastMelodyId(play.melodyId);
            weaponState.setHornLastMelodyEnhanceTicks(60);
            weaponState.clearHornNotes();
        }

        // Recital AoE hit (even without a melody, the swing deals damage)
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "recital", 1.4f);
        applyAoEHit(player, 3.0, mv);

        // Iceborne Echo Attack: resonate nearby bubbles
        triggerEchoResonance(player);
    }

    // ── PERFORMANCE BEAT (R2 during Perform — chain up to 3 beats) ──────

    /**
     * Wilds manual: "While Performing multiple melodies, press R2 for a powerful
     * Performance Beat. Timing Performance Beat and Encore with melody effects
     * will increase damage."
     */
    private static void handlePerformanceBeat(Player player, PlayerCombatState combatState,
                                              PlayerWeaponState weaponState) {
        // Track beat step (up to 3 beats before Encore)
        String currentAction = combatState.getActionKey();
        int beatStep = 1;
        if (currentAction != null && currentAction.startsWith("performance_beat_")) {
            try {
                beatStep = Integer.parseInt(currentAction.substring("performance_beat_".length())) + 1;
            } catch (NumberFormatException ignored) {
                beatStep = 1;
            }
        } else if ("recital".equals(currentAction)) {
            beatStep = 1;
        }

        if (beatStep > 3) {
            // After 3 beats, transition to Encore
            handleEncore(player, combatState, weaponState);
            return;
        }

        String actionKey = "performance_beat_" + beatStep;
        setAction(combatState, actionKey, PERFORMANCE_BEAT_TICKS);

        // Each beat plays the next stocked melody
        WeaponData data = WeaponDataResolver.resolve(player);
        MelodyPlay play = resolveMelodyPlay(data, weaponState);
        if (play != null) {
            applyMelodyEffect(player, play.songId, play.effect, play.amplifier, play.duration, play.radius);
            applyHornSongBuff(weaponState, play.songId, play.duration);
            weaponState.setHornBuffTicks(play.duration);
            weaponState.setHornLastMelodyId(play.melodyId);
            weaponState.setHornLastMelodyEnhanceTicks(60);
        }

        // Performance Beat has increasing damage per step
        float baseMv = WeaponDataResolver.resolveFloat(player, "motionValues", "performance_beat", 1.2f);
        float scaledMv = baseMv + (beatStep * 0.2f);
        applyAoEHit(player, 3.0, scaledMv);

        // Echo Resonance on each beat
        triggerEchoResonance(player);
    }

    // ── ENCORE (LMB+RMB after Performance Beat — boosts & extends) ──────

    /**
     * Wilds manual: "Play an Encore with LMB+RMB to boost and extend Melody Effects."
     */
    private static void handleEncore(Player player, PlayerCombatState combatState,
                                     PlayerWeaponState weaponState) {
        setAction(combatState, "encore", ENCORE_ACTION_TICKS);

        WeaponData data = WeaponDataResolver.resolve(player);
        if (weaponState.getHornLastMelodyId() > 0) {
            MelodyPlay last = resolveMelodyById(data, weaponState.getHornLastMelodyId());
            if (last != null) {
                int amp = last.amplifier + 1;
                int duration = last.duration * 2;
                applyMelodyEffect(player, last.songId, last.effect, amp, duration, last.radius);
                applyHornSongBuff(weaponState, last.songId, duration);
                weaponState.setHornBuffTicks(duration);
            }
        }

        // Encore is the strongest hit in the chain
        float mv = WeaponDataResolver.resolveFloat(player, "motionValues", "encore", 2.0f);
        applyAoEHit(player, 3.5, mv);

        // Heavy resonance
        triggerEchoResonance(player);

        weaponState.setHornMelodyPlayTicks(ENCORE_ACTION_TICKS);
        weaponState.setHornLastMelodyEnhanceTicks(0);
    }

    // ── ECHO BUBBLE (Shift+Special — place a bubble) ────────────────────

    /**
     * Wilds manual: "A special attack that produces an Echo Bubble. The type of Echo
     * Bubble you can create is determined by your equipped hunting horn. You can produce
     * up to three notes while creating an Echo Bubble."
     */
    private static void handleEchoBubble(Player player, PlayerCombatState combatState,
                                         PlayerWeaponState weaponState) {
        setAction(combatState, "echo_bubble", ECHO_BUBBLE_ACTION_TICKS);
        WeaponData data = WeaponDataResolver.resolve(player);
        BubbleDef fixed = resolveFixedEchoBubble(data);
        if (fixed != null) {
            spawnEchoBubble(player, fixed.effect, fixed.amplifier, fixed.duration, fixed.radius);
            weaponState.setHornBuffTicks(fixed.duration);
        }
    }

    // ── COUNTER CALLBACK (called from damage events) ────────────────────

    /**
     * Called externally when the player takes damage during an active Hilt Stab
     * counter window ({@code hornSpecialGuardTicks > 0}).
     * Reduces incoming damage and allows chaining into a Perform.
     *
     * @return the damage reduction multiplier (0.0 = full block, 1.0 = no reduction)
     */
    public static float onCounterHit(Player player, PlayerWeaponState weaponState) {
        if (weaponState.getHornSpecialGuardTicks() <= 0) return 1.0f;
        weaponState.setHornSpecialGuardTicks(0);
        // Reward: stock all current notes as a melody immediately
        WeaponData data = WeaponDataResolver.resolve(player);
        SongMatch match = resolveSongMatch(data, weaponState);
        if (match != null) {
            weaponState.addHornMelody(match.melodyId);
        }
        return 0.3f; // 70% damage reduction on successful counter
    }
}
