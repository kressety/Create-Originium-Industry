package com.mealuet.create_originium_industry.index;

import com.tterrag.registrate.util.entry.FluidEntry;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import static com.mealuet.create_originium_industry.util.FluidRegisterHelper.createDensityFluid;
import static com.mealuet.create_originium_industry.util.FluidRegisterHelper.createLightFluid;

public class COIFluids {
    public static final FluidEntry<BaseFlowingFluid.Flowing> MOLTEN_ORIGINIUM = createDensityFluid("molten_originium");
    public static final FluidEntry<BaseFlowingFluid.Flowing> PUREST_MOLTEN_ORIGINIUM = createDensityFluid("purest_molten_originium");
    public static final FluidEntry<BaseFlowingFluid.Flowing> ORIGINIUM_CATALYST = createLightFluid("originium_catalyst");

    public static void register() {}
}
