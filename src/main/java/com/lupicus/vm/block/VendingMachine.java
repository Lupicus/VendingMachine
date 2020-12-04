package com.lupicus.vm.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.item.ModItems;
import com.lupicus.vm.tileentity.VendingMachineTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;

public class VendingMachine extends RotateContainerBase
{
	public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;
	// save temp values to support drops (should be okay for main server thread only)
	private TileEntity saveTE;
	private BlockState saveState;

	public VendingMachine(Properties properties) {
		super(properties);
		setDefaultState(this.stateContainer.getBaseState().with(HORIZONTAL_FACING, Direction.NORTH).with(BOTTOM, true));
	}

	@Override
	public boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos) {
		return false;
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockItemUseContext context)
	{
		if (!context.getWorld().getBlockState(context.getPos().up()).isReplaceable(context))
			return null;
		return getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing());
	}

	@Override
	public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player,
			Hand handIn, BlockRayTraceResult hit)
	{
		if (!worldIn.isRemote)
		{
			if (!state.get(BOTTOM))
				pos = pos.down();
			TileEntity te = worldIn.getTileEntity(pos);
			if (te instanceof VendingMachineTileEntity)
				((VendingMachineTileEntity) te).openGui(player);
		}
		return ActionResultType.SUCCESS;
	}

	@Override
	public void onBlockPlacedBy(World worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		super.onBlockPlacedBy(worldIn, pos, state, placer, stack);
		if (!worldIn.isRemote)
		{
			worldIn.setBlockState(pos.up(), state.with(BOTTOM, false), 3);
			CompoundNBT tag = stack.getTag();
			if (tag != null)
			{
				TileEntity te = worldIn.getTileEntity(pos);
				if (te instanceof VendingMachineTileEntity)
					((VendingMachineTileEntity) te).readMined(tag);
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onReplaced(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!worldIn.isRemote && state.getBlock() != newState.getBlock())
		{
			BlockPos pos2 = pos.offset(state.get(BOTTOM) ? Direction.UP : Direction.DOWN);
			if (worldIn.getBlockState(pos2).getBlock() == this)
				worldIn.setBlockState(pos2, Blocks.AIR.getDefaultState(), 3);
		}
		super.onReplaced(state, worldIn, pos, newState, isMoving);
	}

	@Override
	public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player,
			boolean willHarvest, IFluidState fluid)
	{
		boolean flag = !world.isRemote;
		if (flag)
		{
			saveTE = null;
			if (!state.get(BOTTOM))
				saveMainBlock(world, pos);
		}
		boolean removed = super.removedByPlayer(state, world, pos, player, willHarvest, fluid);
		if (flag && !(removed && willHarvest))
			saveTE = null;
		return removed;
	}

	@Override
	public boolean canDropFromExplosion(BlockState state, IBlockReader world, BlockPos pos, Explosion explosion)
	{
		if (world instanceof ServerWorld)
		{
			saveTE = null;
			if (!state.get(BOTTOM))
				saveMainBlock(world, pos);
		}
		return true;
	}

	private void saveMainBlock(IBlockReader world, BlockPos pos)
	{
		BlockPos blockpos = pos.offset(Direction.DOWN);
		BlockState blockstate = world.getBlockState(blockpos);
		if (blockstate.getBlock() == this && blockstate.get(BOTTOM))
		{
			saveTE = world.getTileEntity(blockpos);
			saveState = blockstate;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDrops(BlockState state, LootContext.Builder builder)
	{
		if (saveTE != null)
		{
			builder.withNullableParameter(LootParameters.BLOCK_ENTITY, saveTE);
			state = saveState;
			saveTE = null;
		}
		if (!MyConfig.minable)
			return Collections.emptyList();
		List<ItemStack> ret = super.getDrops(state, builder);
		if (!ret.isEmpty())
		{
			TileEntity entity = builder.get(LootParameters.BLOCK_ENTITY);
			if (entity instanceof VendingMachineTileEntity)
			{
				VendingMachineTileEntity vte = (VendingMachineTileEntity) entity;
				for (ItemStack e : ret)
				{
					if (e.getItem() == ModItems.VENDING_MACHINE)
					{
						CompoundNBT tag = e.getOrCreateTag();
						vte.writeMined(tag);
					}
				}
			}
		}
		return ret;
	}

	@Override
	public PushReaction getPushReaction(BlockState state) {
		return PushReaction.BLOCK;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Override
	public boolean hasTileEntity(BlockState state) {
		return state.get(BOTTOM);
	}

	@Override
	public TileEntity createNewTileEntity(IBlockReader worldIn) {
		return new VendingMachineTileEntity();
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING, BOTTOM);
	}
}
