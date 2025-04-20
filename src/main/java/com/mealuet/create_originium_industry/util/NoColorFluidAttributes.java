package com.mealuet.create_originium_industry.util;

import com.simibubi.create.AllFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;

public class NoColorFluidAttributes extends AllFluids.TintedFluidType {
    public NoColorFluidAttributes(Properties properties, ResourceLocation stillTexture, ResourceLocation flowingTexture) {
        super(properties, stillTexture, flowingTexture);
    }

    @Override
    protected int getTintColor(FluidStack stack) {
        return NO_TINT;
    }

    @Override
    public int getTintColor(FluidState state, BlockAndTintGetter world, BlockPos pos) {
        return 0x00ffffff;
    }
}
