package org.example.client.fx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.example.common.network.packet.DamageNumberS2CPacket;

public final class DamageNumberClientTracker {
    private static final int LIFETIME_TICKS = 24;
    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_YELLOW = 0xFFD24A;
    private static final int COLOR_FRAME_AFFINITY = 0xFF9B2F;
    private static final int COLOR_FRAME_NEGATIVE = 0x7FD3FF;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private DamageNumberClientTracker() {
    }

    @SuppressWarnings("null")
    public static void spawn(int targetId, float damage, int colorType, int frameType) {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(targetId);
        if (!(entity instanceof LivingEntity target)) {
            return;
        }
        float shown = Math.max(0.0f, damage);
        String text = String.valueOf(Mth.clamp(Math.round(shown), 0, 9999));
        int numberColor = colorType == DamageNumberS2CPacket.COLOR_YELLOW ? COLOR_YELLOW : COLOR_WHITE;
        int frameColor = switch (frameType) {
            case DamageNumberS2CPacket.FRAME_AFFINITY -> COLOR_FRAME_AFFINITY;
            case DamageNumberS2CPacket.FRAME_NEGATIVE_AFFINITY -> COLOR_FRAME_NEGATIVE;
            default -> -1;
        };

        MutableComponent component;
        if (frameColor != -1) {
            component = Component.literal("[").withStyle(Style.EMPTY.withColor(frameColor))
                .append(Component.literal(text).withStyle(Style.EMPTY.withColor(numberColor)))
                .append(Component.literal("]").withStyle(Style.EMPTY.withColor(frameColor)));
        } else {
            component = Component.literal(text).withStyle(Style.EMPTY.withColor(numberColor));
        }

        double baseY = target.getY() + target.getBbHeight() * 0.7 + level.random.nextDouble() * 0.2;
        double baseX = target.getX() + (level.random.nextDouble() - 0.5) * 0.4;
        double baseZ = target.getZ() + (level.random.nextDouble() - 0.5) * 0.4;
        Vec3 start = new Vec3(baseX, baseY, baseZ);
        Vec3 velocity = new Vec3(0.0, 0.03 + level.random.nextDouble() * 0.02, 0.0);

        ENTRIES.add(new Entry(start, velocity, component, 0, LIFETIME_TICKS));
    }

    @SuppressWarnings("null")
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) {
            ENTRIES.clear();
            return;
        }
        Iterator<Entry> iterator = ENTRIES.iterator();
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            if (entry.age >= entry.lifetime) {
                iterator.remove();
                continue;
            }
            entry.pos = entry.pos.add(entry.velocity);
            entry.age++;
        }
    }

    public static List<Entry> getEntries() {
        return ENTRIES;
    }

    public static final class Entry {
        private Vec3 pos;
        private final Vec3 velocity;
        private final Component text;
        private int age;
        private final int lifetime;

        private Entry(Vec3 pos, Vec3 velocity, Component text, int age, int lifetime) {
            this.pos = pos;
            this.velocity = velocity;
            this.text = text;
            this.age = age;
            this.lifetime = lifetime;
        }

        public Vec3 getPos() {
            return pos;
        }

        public Component getText() {
            return text;
        }

        public float getAlpha() {
            float life = Math.max(1.0f, lifetime);
            return 1.0f - (age / life);
        }
    }
}