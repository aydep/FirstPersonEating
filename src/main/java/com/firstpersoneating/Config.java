package com.firstpersoneating;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEMS = BUILDER
            .comment("Items that force first person while being used. Item id or 'namespace:*'.")
            .defineListAllowEmpty("items", List.of("kaleidoscope_cookery:*"), () -> "modid:item", o -> o instanceof String);

    private static final ModConfigSpec.BooleanValue DEBUG = BUILDER
            .comment("Log camera/animation state transitions (prefix FPE-debug) to latest.log.")
            .define("debug", false);

    static final ModConfigSpec SPEC = BUILDER.build();

    static boolean matches(ResourceLocation id) {
        for (String s : ITEMS.get()) {
            if (s.equals(id.toString()) || s.equals(id.getNamespace() + ":*")) {
                return true;
            }
        }
        return false;
    }

    static void debug(String format, Object... args) {
        if (SPEC.isLoaded() && DEBUG.get()) {
            FirstPersonEating.LOGGER.info(format, args);
        }
    }
}
