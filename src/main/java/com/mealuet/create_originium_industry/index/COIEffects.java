package com.mealuet.create_originium_industry.index;

import com.mealuet.create_originium_industry.CreateOriginiumIndustry;
import com.mealuet.create_originium_industry.effect.OriDustSicknessEffect;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class COIEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, CreateOriginiumIndustry.MODID);

    public static final DeferredHolder<MobEffect, OriDustSicknessEffect> ORI_DUST_SICKNESS_EFFECT = EFFECTS.register("ori_dust_sickness",
            () -> new OriDustSicknessEffect(MobEffectCategory.HARMFUL, 0x8B8989));

    public static void register(IEventBus modEventBus) {
        EFFECTS.register(modEventBus);
    }
}
