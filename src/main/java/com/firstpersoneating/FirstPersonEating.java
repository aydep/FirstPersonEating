package com.firstpersoneating;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = FirstPersonEating.MODID, dist = Dist.CLIENT)
public class FirstPersonEating {
    public static final String MODID = "firstpersoneating";

    public FirstPersonEating(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        // Handler touches Shoulder Surfing and Immersive Eating classes, so only load it when both are present.
        if (ModList.get().isLoaded("shouldersurfing") && ModList.get().isLoaded("food")) {
            NeoForge.EVENT_BUS.register(new EatingViewHandler());
        }
    }
}
