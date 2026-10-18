package com.firstpersoneating;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.Perspective;

import io.github.memorytoame.immersiveeating.neoforge.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.neoforge.utils.AnimationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class EatingViewHandler {
    private Perspective saved;

    // Pre: Immersive Eating cancels its animation in its own Post tick whenever the camera isn't first person,
    // and it cancels vanilla right-click use of its foods, so there is no use-item event to hook; watch the held item instead.
    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            saved = null;
            return;
        }
        boolean active = wantsFirstPerson(mc) || AnimationUtils.isAnimationLocked() || AnimationUtils.isAnimationRequestPending();
        if (active && saved == null && Perspective.current() != Perspective.FIRST_PERSON) {
            saved = Perspective.current();
            IShoulderSurfing.getInstance().changePerspective(Perspective.FIRST_PERSON);
        } else if (!active && saved != null) {
            IShoulderSurfing.getInstance().changePerspective(saved);
            saved = null;
        }
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        saved = null;
    }

    // Mirrors when Immersive Eating starts a right-click animation: use key held, no screen, not aiming at a block.
    private static boolean wantsFirstPerson(Minecraft mc) {
        var item = mc.player.getMainHandItem().getItem();
        return mc.screen == null && mc.options.keyUse.isDown()
                && (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK)
                && FoodDefinitionManager.init_ItemList.contains(item)
                && Config.matches(BuiltInRegistries.ITEM.getKey(item));
    }
}
