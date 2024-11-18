package com.lupicus.vm.tileentity;

import java.util.Set;

import com.lupicus.vm.block.ModBlocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModTileEntities
{
	public static final BlockEntityType<VendingMachineTileEntity> VENDING_MACHINE = new BlockEntityType<>(VendingMachineTileEntity::new, Set.of(ModBlocks.VENDING_MACHINE));

	public static void register(IForgeRegistry<BlockEntityType<?>> forgeRegistry)
	{
		forgeRegistry.register("vending_machine", VENDING_MACHINE);
	}
}
