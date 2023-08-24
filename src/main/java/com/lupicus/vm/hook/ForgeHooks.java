package com.lupicus.vm.hook;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraftforge.common.util.MutableHashedLinkedMap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.ModLoader;

public class ForgeHooks
{
	// copy code from ForgeHooksClient so we can call on server without crashing
    public static void onCreativeModeTabBuildContents(CreativeModeTab tab, ResourceKey<CreativeModeTab> tabKey, CreativeModeTab.DisplayItemsGenerator originalGenerator, CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output output)
    {
        final var entries = new MutableHashedLinkedMap<ItemStack, CreativeModeTab.TabVisibility>(ItemStackLinkedSet.TYPE_AND_TAG,
            (key, left, right) -> {
                //throw new IllegalStateException("Accidentally adding the same item stack twice " + key.getDisplayName().getString() + " to a Creative Mode Tab: " + tab.getDisplayName().getString());
                // Vanilla adds enchanting books twice in both visibilities.
                // This is just code cleanliness for them. For us lets just increase the visibility and merge the entries.
                return CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
            }
        );

        originalGenerator.accept(params, (stack, vis) -> {
            if (stack.getCount() != 1)
                throw new IllegalArgumentException("The stack count must be 1");
            entries.put(stack, vis);
        });

        ModLoader.get().postEvent(new BuildCreativeModeTabContentsEvent(tab, tabKey, params, entries));

        for (var entry : entries)
            output.accept(entry.getKey(), entry.getValue());
    }
}
