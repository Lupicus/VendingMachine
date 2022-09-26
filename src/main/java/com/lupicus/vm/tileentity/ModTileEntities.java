package com.lupicus.vm.tileentity;

import com.lupicus.vm.block.ModBlocks;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModTileEntities
{
	public static final BlockEntityType<VendingMachineTileEntity> VENDING_MACHINE = BlockEntityType.Builder.of(VendingMachineTileEntity::new, ModBlocks.VENDING_MACHINE).build(null);

	public static void register(IForgeRegistry<BlockEntityType<?>> forgeRegistry)
	{
		forgeRegistry.register("vending_machine", VENDING_MACHINE);
	}
}
