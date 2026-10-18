package com.firstpersoneating;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<List<? extends String>> ITEMS = BUILDER
            .comment("Items that force first person while being used. Item id or 'namespace:*'.")
            .defineListAllowEmpty("items", List.of("kaleidoscopecookery:*"), () -> "modid:item", o -> o instanceof String);

    static final ModConfigSpec SPEC = BUILDER.build();

    static boolean matches(ResourceLocation id) {
        for (String s : ITEMS.get()) {
            if (s.equals(id.toString()) || s.equals(id.getNamespace() + ":*")) {
                return true;
            }
        }
        return false;
    }
}
