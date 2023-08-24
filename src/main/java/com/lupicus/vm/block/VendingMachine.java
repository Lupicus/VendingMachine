package com.lupicus.vm.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.item.ModItems;
import com.lupicus.vm.tileentity.VendingMachineTileEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

public class VendingMachine extends RotateContainerBase
{
	public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;
	// save temp values to support drops (should be okay for main server thread only)
	private BlockEntity saveTE;
	private BlockState saveState;

	public VendingMachine(Properties properties) {
		super(properties);
		registerDefaultState(getStateDefinition().any().setValue(HORIZONTAL_FACING, Direction.NORTH).setValue(BOTTOM, true));
	}

	public static boolean isNormalCube(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return false;
	}

	public static int lightValue(BlockState state) {
		return 8;
	}

	public static boolean isEnabled(BlockState state) {
		return state.getValue(BOTTOM);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		if (!context.getLevel().getBlockState(context.getClickedPos().above()).canBeReplaced(context))
			return null;
		return defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
	}

	@Override
	public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player,
			InteractionHand handIn, BlockHitResult hit)
	{
		if (!worldIn.isClientSide)
		{
			if (!state.getValue(BOTTOM))
				pos = pos.below();
			BlockEntity te = worldIn.getBlockEntity(pos);
			if (te instanceof VendingMachineTileEntity)
				((VendingMachineTileEntity) te).openGui(player);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		super.setPlacedBy(worldIn, pos, state, placer, stack);
		if (!worldIn.isClientSide)
		{
			worldIn.setBlock(pos.above(), state.setValue(BOTTOM, false), 3);
			CompoundTag tag = stack.getTag();
			if (tag != null)
			{
				BlockEntity te = worldIn.getBlockEntity(pos);
				if (te instanceof VendingMachineTileEntity)
				{
					VendingMachineTileEntity vte = (VendingMachineTileEntity) te;
					vte.readMined(tag);
					if (stack.hasCustomHoverName())
						vte.setCustomName(stack.getHoverName());
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving)
	{
		if (!worldIn.isClientSide && state.getBlock() != newState.getBlock())
		{
			BlockPos pos2 = pos.relative(state.getValue(BOTTOM) ? Direction.UP : Direction.DOWN);
			if (worldIn.getBlockState(pos2).getBlock() == this)
				worldIn.setBlock(pos2, Blocks.AIR.defaultBlockState(), 3);
		}
		super.onRemove(state, worldIn, pos, newState, isMoving);
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player,
			boolean willHarvest, FluidState fluid)
	{
		boolean flag = !world.isClientSide;
		if (flag)
		{
			saveTE = null;
			if (!state.getValue(BOTTOM))
				saveMainBlock(world, pos);
		}
		boolean removed = super.onDestroyedByPlayer(state, world, pos, player, willHarvest, fluid);
		if (flag && !(removed && willHarvest))
			saveTE = null;
		return removed;
	}

	@Override
	public boolean canDropFromExplosion(BlockState state, BlockGetter world, BlockPos pos, Explosion explosion)
	{
		if (world instanceof ServerLevel)
		{
			saveTE = null;
			if (!state.getValue(BOTTOM))
				saveMainBlock(world, pos);
		}
		return true;
	}

	private void saveMainBlock(BlockGetter world, BlockPos pos)
	{
		BlockPos blockpos = pos.relative(Direction.DOWN);
		BlockState blockstate = world.getBlockState(blockpos);
		if (blockstate.getBlock() == this && blockstate.getValue(BOTTOM))
		{
			saveTE = world.getBlockEntity(blockpos);
			saveState = blockstate;
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder)
	{
		if (saveTE != null)
		{
			builder.withOptionalParameter(LootContextParams.BLOCK_ENTITY, saveTE);
			state = saveState;
			saveTE = null;
		}
		if (!MyConfig.minable)
			return Collections.emptyList();
		List<ItemStack> ret = super.getDrops(state, builder);
		if (!ret.isEmpty())
		{
			BlockEntity entity = builder.getParameter(LootContextParams.BLOCK_ENTITY);
			if (entity instanceof VendingMachineTileEntity)
			{
				VendingMachineTileEntity vte = (VendingMachineTileEntity) entity;
				for (ItemStack e : ret)
				{
					if (e.getItem() == ModItems.VENDING_MACHINE)
					{
						CompoundTag tag = e.getOrCreateTag();
						vte.writeMined(tag);
						if (vte.hasCustomName())
							e.setHoverName(vte.getCustomName());
					}
				}
			}
		}
		return ret;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new VendingMachineTileEntity(pos, state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(HORIZONTAL_FACING, BOTTOM);
	}
}
