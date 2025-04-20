package com.mealuet.create_originium_industry.util;

import com.mealuet.create_originium_industry.CreateOriginiumIndustry;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class RecipeTypeRegisterHelper {
    public static <T extends Recipe<?>> Supplier<RecipeType<T>> registerRecipeType(DeferredRegister<RecipeType<?>> registries, String name) {
        return registries.register(name, () -> new RecipeType<>() {
            @Override
            public String toString() {
                return CreateOriginiumIndustry.MODID + ":" + name;
            }
        });
    }
}
