package com.mealuet.create_originium_industry.index;

import com.simibubi.create.AllCreativeModeTabs;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.mealuet.create_originium_industry.CreateOriginiumIndustry.MODID;

public class COICreativeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATE_TABS_REGISTRATE = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_CREATIVE_TAB = CREATE_TABS_REGISTRATE.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.create_originium_industry.main"))
            .withTabsBefore(AllCreativeModeTabs.PALETTES_CREATIVE_TAB.getKey())
            .icon(() -> COIItems.PUREST_ORIGINIUM.get().getDefaultInstance())
            .build());

    public static void register(IEventBus modEventBus) {
        CREATE_TABS_REGISTRATE.register(modEventBus);
    }
}
