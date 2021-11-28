package com.lupicus.vm.tileentity;

import com.lupicus.vm.block.ModBlocks;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModTileEntities
{
	public static final BlockEntityType<VendingMachineTileEntity> VENDING_MACHINE = create("vending_machine", BlockEntityType.Builder.of(VendingMachineTileEntity::new, ModBlocks.VENDING_MACHINE).build(null));

	public static <T extends BlockEntity> BlockEntityType<T> create(String key, BlockEntityType<T> type)
	{
		type.setRegistryName(key);
		return type;
	}

	public static void register(IForgeRegistry<BlockEntityType<?>> forgeRegistry)
	{
		forgeRegistry.register(VENDING_MACHINE);
	}
}
