package org.example.client.compat;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.world.entity.player.Player;
import org.example.MHWeaponsMod;
import org.example.common.compat.BetterCombatAnimationBridge;
import org.example.common.compat.BetterCombatCompat;

public final class BetterCombatClientAdapter {
    private BetterCombatClientAdapter() {
    }

    public static void register() {
        if (!BetterCombatCompat.isLoaded()) {
            return;
        }
        try {
            Class<?> eventsClass = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents");
            Class<?> publisherClass = Class.forName("net.bettercombat.api.event.Publisher");

            Object attackStartPublisher = eventsClass.getField("ATTACK_START").get(null);
            Class<?> attackStartListener = Class.forName("net.bettercombat.api.client.BetterCombatClientEvents$PlayerAttackStart");

            Object attackStartProxy = Proxy.newProxyInstance(
                    BetterCombatClientAdapter.class.getClassLoader(),
                    new Class<?>[]{attackStartListener},
                    new AttackStartHandler()
            );
            Method register = findRegisterMethod(publisherClass, attackStartListener);
            if (register != null) {
                register.invoke(attackStartPublisher, attackStartProxy);
                MHWeaponsMod.LOGGER.info("Better Combat client adapter registered.");
            } else {
                MHWeaponsMod.LOGGER.warn("Better Combat client adapter failed to register: no compatible register method");
            }

        } catch (Exception ex) {
            MHWeaponsMod.LOGGER.warn("Better Combat client adapter failed to register", ex);
        }
    }

    private static Method findRegisterMethod(Class<?> publisherClass, Class<?> listenerType) {
        Method fallback = null;
        for (Method method : publisherClass.getMethods()) {
            if (method.getParameterCount() != 1) {
                continue;
            }
            Class<?> paramType = method.getParameterTypes()[0];
            if (!paramType.isAssignableFrom(listenerType)) {
                continue;
            }
            String name = method.getName();
            if ("register".equals(name) || "subscribe".equals(name) || "addListener".equals(name) || "listen".equals(name)) {
                return method;
            }
            if (!Object.class.equals(paramType)) {
                fallback = method;
            }
        }
        return fallback;
    }

    private static final class AttackStartHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (args != null && args.length > 0 && args[0] instanceof Player player) {
                BetterCombatAnimationBridge.onBetterCombatAttack(player, "bettercombat:attack_start");
            }
            return null;
        }
    }
}
