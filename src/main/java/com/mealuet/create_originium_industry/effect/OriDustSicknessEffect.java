package com.mealuet.create_originium_industry.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

public class OriDustSicknessEffect extends MobEffect {
    private static final int EFFECT_INTERVAL = 20; // Every second

    public OriDustSicknessEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % EFFECT_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // Apply increasing slowness and weakness based on amplifier
        // Level 0 (Amplifier 0): Slowness 1
        // Level 1 (Amplifier 1): Slowness 1, Weakness 1
        // Level 2 (Amplifier 2): Slowness 2, Weakness 1
        // Level 3 (Amplifier 3): Slowness 2, Weakness 2
        // Level 4 (Amplifier 4): Slowness 3, Weakness 2

        int slownessAmplifier = amplifier / 2;
        int weaknessAmplifier = (amplifier > 0) ? (amplifier - 1) / 2 : -1;

        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_INTERVAL + 5, slownessAmplifier, true, false, true)); // Show icon

        if (weaknessAmplifier >= 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_INTERVAL + 5, weaknessAmplifier, true, false, true)); // Show icon
        }

        return true;
    }
}
