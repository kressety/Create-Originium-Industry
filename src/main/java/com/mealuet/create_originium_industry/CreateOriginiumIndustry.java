package com.mealuet.create_originium_industry;

import com.mealuet.create_originium_industry.core.oridust.OriDustTickHandler;
import com.mealuet.create_originium_industry.index.*;
import com.mojang.logging.LogUtils;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.tterrag.registrate.util.entry.RegistryEntry;
import net.createmod.catnip.lang.FontHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

@Mod(CreateOriginiumIndustry.MODID)
public class CreateOriginiumIndustry
{
    public static final String MODID = "create_originium_industry";
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(MODID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null)
            .setTooltipModifierFactory(item ->
                    new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                            .andThen(TooltipModifier.mapNull(KineticStats.create(item)))
            );
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateOriginiumIndustry(IEventBus modEventBus, ModContainer modContainer) {
        REGISTRATE.registerEventListeners(modEventBus);
        COICreativeTabs.register(modEventBus);
        COIItems.register();
        COIFluids.register();
        COIEffects.register(modEventBus);
        COIAttachments.register(modEventBus);

        NeoForge.EVENT_BUS.register(OriDustTickHandler.class);

        modEventBus.addListener(this::addCreative);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(COICreativeTabs.MAIN_CREATIVE_TAB.getKey())) {
            for (RegistryEntry<Item, Item> entry : REGISTRATE.getAll(Registries.ITEM)) {
                Item item = entry.get();
                if (item instanceof BlockItem) continue;
                if (item instanceof BucketItem) continue;
                event.accept(item);
            }
            for (RegistryEntry<Block, Block> entry : REGISTRATE.getAll(Registries.BLOCK)) {
                Block block = entry.get();
                if (block.asItem() == Items.AIR) continue;
                event.accept(block);
            }
            for (RegistryEntry<Fluid, Fluid> entry : REGISTRATE.getAll(Registries.FLUID)) {
                Fluid fluid = entry.get();
                if (fluid.defaultFluidState().isSource()) {
                    event.accept(fluid.getBucket());
                }
            }
        }
    }
}
