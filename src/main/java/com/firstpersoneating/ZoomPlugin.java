package com.firstpersoneating;

import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputeTargetCameraOffsetEventHandler;
import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputeTemporaryFirstPersonStateEventHandler;
import com.github.exopandora.shouldersurfing.api.client.event.handler.PerspectiveChangedEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;

import net.minecraft.world.phys.Vec3;

/** Loaded by Shoulder Surfing; while {@link #zooming} is set, its own offset smoothing glides the camera into the head. */
public class ZoomPlugin implements IShoulderSurfingPlugin {
    static volatile boolean zooming;
    static volatile boolean holdFirstPerson;
    static volatile boolean zoomingOut;

    @Override
    public void register(IEventBus bus) {
        bus.register((PerspectiveChangedEventHandler) event ->
                Config.debug("perspective -> {}", event.getPerspective()));
        // Shoulder Surfing flips back out of first person by itself when its own checks stop wanting it (e.g. while walking),
        // which makes Immersive Eating cancel the animation; pin it while we are eating.
        bus.register((ComputeTemporaryFirstPersonStateEventHandler) event -> {
            if (holdFirstPerson) {
                event.setResult(true);
            } else if (zoomingOut) { // the camera starts inside the head, which would otherwise look like "too close, go first person"
                event.setResult(false);
            }
        });
        bus.register((ComputeTargetCameraOffsetEventHandler) event -> {
            if (zooming) {
                event.setResult(Vec3.ZERO);
            }
        });
    }
}
