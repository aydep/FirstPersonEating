package com.firstpersoneating;

import java.lang.reflect.Field;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.Perspective;

import io.github.memorytoame.immersiveeating.neoforge.common.content.definition.FoodDefinitionManager;
import io.github.memorytoame.immersiveeating.neoforge.utils.AnimationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

public class EatingViewHandler {
    private static final int MAX_ZOOM_TICKS = 30;
    private static final int REPLAY_TICKS = 25;
    private static final double ZOOM_OUT_DONE_DISTANCE = 1.8;
    private static final int RESTORE_DELAY_TICKS = 8;
    private static final double ZOOM_DONE_DISTANCE = 0.15;

    private Perspective saved;
    private boolean zoomingIn;
    private int zoomTicks;
    private int replayTicks;
    private boolean needZero;
    private int zoomOutTicks;
    private int inactiveTicks;

    // Pre: Immersive Eating cancels its animation in its own Post tick whenever the camera isn't first person,
    // and it cancels vanilla right-click use of its foods, so there is no use-item event to hook; watch the held item instead.
    @SubscribeEvent
    public void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            reset();
            return;
        }
        if (ZoomPlugin.zoomingOut
                && (++zoomOutTicks > MAX_ZOOM_TICKS || !needZero && IShoulderSurfing.getInstance().getCamera().getOffset().length() > ZOOM_OUT_DONE_DISTANCE)) {
            ZoomPlugin.zoomingOut = false;
        }
        if (replayTicks > 0) {
            // Immersive Eating can silently refuse a start (swing/re-equip cooldown, block-latch flags that only reset on release),
            // so pulse the key until it reports an animation instead of replaying a single press.
            if (AnimationUtils.isAnimationLocked() || AnimationUtils.isAnimationRequestPending()) {
                Config.debug("replay ok with {} ticks left", replayTicks);
                replayTicks = 0;
            } else {
                replayTicks--;
            }
            mc.options.keyUse.setDown(replayTicks % 2 == 1);
        }
        boolean wants = wantsFirstPerson(mc);
        if (zoomingIn) {
            Config.debug("zoom tick {} offsetLen={} screen={} holdsFood={} hand={} locked={} pending={} using={}", zoomTicks, IShoulderSurfing.getInstance().getCamera().getOffset().length(), mc.screen, holdsFood(mc), mc.player.getMainHandItem(), AnimationUtils.isAnimationLocked(), AnimationUtils.isAnimationRequestPending(), mc.player.isUsingItem());
            if (mc.screen != null || !holdsFood(mc)) { // aborted: Shoulder Surfing glides back out by itself
                reset();
            } else if (++zoomTicks > MAX_ZOOM_TICKS
                    || IShoulderSurfing.getInstance().getCamera().getOffset().length() < ZOOM_DONE_DISTANCE) {
                Config.debug("zoom done after {} ticks hit={}", zoomTicks, mc.hitResult == null ? null : mc.hitResult.getType());
                ZoomPlugin.zooming = zoomingIn = false;
                ZoomPlugin.holdFirstPerson = true;
                IShoulderSurfing.getInstance().changePerspective(Perspective.FIRST_PERSON);
                mc.options.keyUse.setDown(true); // replay the click now that Immersive Eating will accept it
                replayTicks = REPLAY_TICKS;
            } else {
                mc.options.keyUse.setDown(false); // otherwise Immersive Eating starts, then cancels itself in third person
            }
            return;
        }
        boolean active = wants || AnimationUtils.isAnimationLocked() || AnimationUtils.isAnimationRequestPending();
        Perspective current = Perspective.current();
        if (saved == null) {
            if (active && current != Perspective.FIRST_PERSON) {
                saved = current;
                inactiveTicks = 0;
                if (current == Perspective.SHOULDER_SURFING) {
                    Config.debug("zoom start offsetLen={} hand={} hit={} down={}", IShoulderSurfing.getInstance().getCamera().getOffset().length(), mc.player.getMainHandItem(), mc.hitResult == null ? null : mc.hitResult.getType(), mc.options.keyUse.isDown());
                    ZoomPlugin.zooming = zoomingIn = true;
                    ZoomPlugin.zoomingOut = needZero = false;
                    zoomTicks = 0;
                    mc.options.keyUse.setDown(false); // Immersive Eating's own Post tick runs next and would start (and swap the held item) too early
                } else {
                    ZoomPlugin.holdFirstPerson = true;
                    IShoulderSurfing.getInstance().changePerspective(Perspective.FIRST_PERSON);
                }
            }
        } else if (active) {
            inactiveTicks = 0;
        } else if (++inactiveTicks >= RESTORE_DELAY_TICKS) { // debounce: leaving first person mid-animation makes Immersive Eating cancel it
            Config.debug("restore {} (current={})", saved, current);
            ZoomPlugin.holdFirstPerson = false;
            IShoulderSurfing.getInstance().changePerspective(saved);
            if (saved == Perspective.SHOULDER_SURFING) {
                ZoomPlugin.zoomingOut = needZero = true;
                zoomOutTicks = 0;
            }
            saved = null;
        } else {
            Config.debug("inactive tick {} wants={} locked={} pending={} using={} current={}", inactiveTicks, wants,
                    AnimationUtils.isAnimationLocked(), AnimationUtils.isAnimationRequestPending(), mc.player.isUsingItem(), current);
        }
    }

    private void reset() {
        if (zoomingIn || saved != null) {
            Config.debug("reset zoomingIn={} saved={}", zoomingIn, saved);
        }
        ZoomPlugin.zooming = zoomingIn = false;
        ZoomPlugin.holdFirstPerson = false;
        ZoomPlugin.zoomingOut = needZero = false;
        replayTicks = 0;
        saved = null;
    }

    // Re-entering shoulder view makes Shoulder Surfing snap its camera offset straight to the shoulder (its own tick runs in Pre);
    // run after it and put the offset back at the head so its normal smoothing glides the camera out.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onTickAfterShoulderSurfing(ClientTickEvent.Pre event) {
        if (!needZero) {
            return;
        }
        try {
            Object camera = IShoulderSurfing.getInstance().getCamera();
            Class<?> type = camera.getClass();
            Field initialized = type.getDeclaredField("initialized");
            initialized.setAccessible(true);
            if (!initialized.getBoolean(camera)) {
                return;
            }
            for (String name : new String[] {"offset", "offsetO", "renderOffset"}) {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                field.set(camera, Vec3.ZERO);
            }
            Config.debug("zoom out: offset zeroed");
        } catch (ReflectiveOperationException e) {
            FirstPersonEating.LOGGER.warn("zoom out unavailable", e); // shortcut: private Shoulder Surfing fields, only checked against 5.2.0
            ZoomPlugin.zoomingOut = false;
        }
        needZero = false;
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    // Mirrors when Immersive Eating starts a right-click animation: use key held, no screen, not aiming at a block.
    private static boolean wantsFirstPerson(Minecraft mc) {
        if (mc.screen != null || (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) || !holdsFood(mc)) {
            return false;
        }
        // A click shorter than a tick is never "down" when we look, so also count queued presses.
        boolean clicked = false;
        while (mc.options.keyUse.consumeClick()) {
            clicked = true;
        }
        return clicked || mc.options.keyUse.isDown();
    }

    private static boolean holdsFood(Minecraft mc) {
        var item = mc.player.getMainHandItem().getItem();
        return FoodDefinitionManager.init_ItemList.contains(item) && Config.matches(BuiltInRegistries.ITEM.getKey(item));
    }
}
