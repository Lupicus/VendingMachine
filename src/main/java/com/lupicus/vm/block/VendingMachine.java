package com.lupicus.vm.block;

import javax.annotation.Nullable;

import com.lupicus.vm.tileentity.VendingMachineTileEntity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

public class VendingMachine extends RotateContainerBase
{
	public static final BooleanProperty BOTTOM = BlockStateProperties.BOTTOM;

	public VendingMachine(Properties properties) {
		super(properties);
		setDefaultState(this.stateContainer.getBaseState().with(HORIZONTAL_FACING, Direction.NORTH).with(BOTTOM, true));
	}

	public static boolean isNormalCube(BlockState state, IBlockReader worldIn, BlockPos pos) {
		return false;
	}

	public static int lightValue(BlockState state) {
		return 8;
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
			Hand handIn, BlockRayTraceResult p_225533_6_)
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
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn,
			BlockPos currentPos, BlockPos facingPos)
	{
		Direction want = stateIn.get(BOTTOM) ? Direction.UP : Direction.DOWN;
		if (facing == want)
			return facingState.getBlock() == this ? stateIn : Blocks.AIR.getDefaultState();
		return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
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
