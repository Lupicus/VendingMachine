package com.lupicus.vm.block;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import com.lupicus.vm.config.MyConfig;
import com.lupicus.vm.item.ModItems;
import com.lupicus.vm.tileentity.VendingMachineTileEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
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
	public static final MapCodec<VendingMachine> CODEC = simpleCodec(VendingMachine::new);
	public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;
	// save temp values to support drops (should be okay for main server thread only)
	private boolean skipDrop = false;
	private BlockEntity saveTE;
	private BlockState saveState;

	@Override
	protected MapCodec<VendingMachine> codec() {
		return CODEC;
	}

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
	public InteractionResult useWithoutItem(BlockState state, Level worldIn, BlockPos pos, Player player,
			BlockHitResult hit)
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

	@SuppressWarnings("deprecation")
	@Override
	public void setPlacedBy(Level worldIn, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		super.setPlacedBy(worldIn, pos, state, placer, stack);
		if (!worldIn.isClientSide)
		{
			worldIn.setBlock(pos.above(), state.setValue(BOTTOM, false), 3);
			BlockEntity te = worldIn.getBlockEntity(pos);
			if (te instanceof VendingMachineTileEntity)
			{
				VendingMachineTileEntity vte = (VendingMachineTileEntity) te;
				CustomData data = stack.get(DataComponents.CUSTOM_DATA);
				if (data != null)
					vte.readMined(data.getUnsafe());
				if (stack.has(DataComponents.CUSTOM_NAME))
					vte.setCustomName(stack.getHoverName());
			}
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader worldIn, ScheduledTickAccess sta,
			BlockPos pos, Direction dir, BlockPos pos2, BlockState state2, RandomSource rand)
	{
		Direction dir2 = state.getValue(BOTTOM) ? Direction.UP : Direction.DOWN;
		if (dir == dir2 && !state2.is(this))
		{
			if (!worldIn.isClientSide())
				skipDrop = true;
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, worldIn, sta, pos, dir, pos2, state2, rand);
	}

	@Override
	public boolean onDestroyedByPlayer(BlockState state, Level world, BlockPos pos, Player player,
			boolean willHarvest, FluidState fluid)
	{
		boolean flag = !world.isClientSide;
		if (flag)
		{
			skipDrop = false;
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
			skipDrop = false;
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
		if (blockstate.is(this) && blockstate.getValue(BOTTOM))
		{
			saveTE = world.getBlockEntity(blockpos);
			saveState = blockstate;
		}
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder)
	{
		if (skipDrop)
		{
			skipDrop = false;
			return Collections.emptyList();
		}
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
						CustomData data = e.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
						data = data.update(t -> vte.writeMined(t));
						e.set(DataComponents.CUSTOM_DATA, data);
						if (vte.hasCustomName())
							e.set(DataComponents.CUSTOM_NAME, vte.getCustomName());
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
