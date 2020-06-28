package com.lupicus.vm.tileentity;

import com.lupicus.vm.block.ModBlocks;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.IForgeRegistry;

public class ModTileEntities
{
	public static final TileEntityType<VendingMachineTileEntity> VENDING_MACHINE = create("vending_machine", TileEntityType.Builder.create(VendingMachineTileEntity::new, ModBlocks.VENDING_MACHINE).build(null));

	public static <T extends TileEntity> TileEntityType<T> create(String key, TileEntityType<T> type)
	{
		type.setRegistryName(key);
		return type;
	}

	public static void register(IForgeRegistry<TileEntityType<?>> forgeRegistry)
	{
		forgeRegistry.register(VENDING_MACHINE);
	}
}
