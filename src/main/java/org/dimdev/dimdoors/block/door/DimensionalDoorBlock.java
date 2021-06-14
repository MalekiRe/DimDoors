package org.dimdev.dimdoors.block.door;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Pair;
import org.apache.logging.log4j.Level;
import org.dimdev.dimdoors.DimensionalDoorsInitializer;
import org.dimdev.dimdoors.api.rift.target.EntityTarget;
import org.dimdev.dimdoors.api.rift.target.Target;
import org.dimdev.dimdoors.api.util.math.MathUtil;
import org.dimdev.dimdoors.api.util.math.TransformationMatrix3d;
import org.dimdev.dimdoors.block.ModBlocks;
import org.dimdev.dimdoors.block.PortalProvider;
import org.dimdev.dimdoors.block.RiftProvider;
import org.dimdev.dimdoors.block.entity.DetachedRiftBlockEntity;
import org.dimdev.dimdoors.block.entity.EntranceRiftBlockEntity;
import org.dimdev.dimdoors.rift.ImmersivePortalsUtil;
import org.dimdev.dimdoors.rift.targets.Targets;
import org.jetbrains.annotations.Nullable;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import qouteall.imm_ptl.core.portal.Portal;

public class DimensionalDoorBlock extends WaterLoggableDoorBlock implements RiftProvider<EntranceRiftBlockEntity>, PortalProvider {
	public DimensionalDoorBlock(Settings settings) {
		super(settings);
	}

	@Override
	@SuppressWarnings("deprecation")
	// TODO: change from onEntityCollision to some method for checking if player crossed portal plane
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (world.isClient) {
			return;
		}

		// TODO: replace with dimdoor cooldown?
		if (entity.hasNetherPortalCooldown()) {
			entity.resetNetherPortalCooldown();
			return;
		}
		entity.resetNetherPortalCooldown();

		BlockPos top = state.get(HALF) == DoubleBlockHalf.UPPER ? pos : pos.up();
		BlockPos bottom = top.down();
		BlockState doorState = world.getBlockState(bottom);

		if (handleIpPortalCreation(doorState, world, bottom)) return;

		if (doorState.getBlock() == this && doorState.get(DoorBlock.OPEN)) { // '== this' to check if not half-broken
			this.getRift(world, pos, state).teleport(entity);
			if (DimensionalDoorsInitializer.getConfig().getDoorsConfig().closeDoorBehind) {
				world.setBlockState(top, world.getBlockState(top).with(DoorBlock.OPEN, false));
				world.setBlockState(bottom, world.getBlockState(bottom).with(DoorBlock.OPEN, false));
			}
		}
	}
	public boolean handleIpPortalCreation(BlockState state, World world, BlockPos pos) {
		if (!DimensionalDoorsInitializer.isIpLoaded()) return false;
		Block sourceBlock = state.getBlock();
		if (!(sourceBlock instanceof PortalProvider)) return false;
		PortalProvider sourcePortalProvider = (PortalProvider) sourceBlock;

		EntranceRiftBlockEntity rift = this.getRift(world, pos, state);
		Target target = rift.getTarget().as(Targets.ENTITY);
		if (!(target instanceof EntranceRiftBlockEntity)) return false;

		EntranceRiftBlockEntity targetRift = (EntranceRiftBlockEntity) target;
		if (rift.isIpPortalLinked() && targetRift.isIpPortalLinked()) return true;
		World targetWorld = targetRift.getWorld();

		BlockPos targetPos = targetRift.getPos();

		BlockState targetState = targetWorld.getBlockState(targetPos);
		Block targetBlock = targetState.getBlock();
		if (!(targetBlock instanceof PortalProvider)) return false;

		PortalProvider targetPortalProvider = (PortalProvider) targetBlock;

		if (!targetRift.isIpPortalLinked()) {
			if(!rift.isIpPortalLinked() || rift.getPortalState()) {
				targetPortalProvider.setupAsReceivingPortal(targetState, targetWorld, targetPos, state);
				if (!rift.isIpPortalLinked()) {
					sourcePortalProvider.setupAsSendingPortal(state, world, pos);
				}
			} else {
				targetPortalProvider.setupAsSendingPortal(targetState, targetWorld, targetPos);
			}
		} else {
			sourcePortalProvider.setupAsSendingPortal(state, world, pos);
		}
		targetState = targetWorld.getBlockState(targetPos);


		TransformationMatrix3d.TransformationMatrix3dBuilder targetRotatorBuilder = rotatorBuilder(targetState, targetPos);
		Pair<Portal, Portal> sourcePortals = sourcePortalProvider.createTwoSidedUnboundPortal(state, world, pos, targetRotatorBuilder);
		Portal sourceFront = sourcePortals.getLeft();
		Portal sourceBack = sourcePortals.getRight();

		TransformationMatrix3d.TransformationMatrix3dBuilder sourceRotatorBuilder = rotatorBuilder(state, pos);
		Pair<Portal, Portal> targetPortals = targetPortalProvider.createTwoSidedUnboundPortal(targetState, targetWorld, targetPos, sourceRotatorBuilder);
		Portal targetFront = targetPortals.getLeft();
		Portal targetBack = targetPortals.getRight();

		ImmersivePortalsUtil.linkPortals(sourceFront, targetBack);
		ImmersivePortalsUtil.linkPortals(sourceBack, targetFront);


		sourceFront.world.spawnEntity(sourceFront);
		sourceBack.world.spawnEntity(sourceBack);

		targetFront.world.spawnEntity(targetFront);
		targetBack.world.spawnEntity(targetBack);

		System.out.println(sourceFront.getPos());
		System.out.println(sourceBack.getPos());

		return true;
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hitResult) {
		state = state.cycle(OPEN);
		world.setBlockState(pos, state, 10);
		if (!world.isClient && state.get(WATERLOGGED)) {
			world.getFluidTickScheduler().schedule(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
		}
		world.syncWorldEvent(player, state.get(OPEN) ? this.material == Material.METAL ? 1005 : 1006 : this.material == Material.METAL ? 1011 : 1012, pos, 0);
		world.emitGameEvent(player, this.isOpen(state) ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
		if (!world.isClient()) {
			if (state.get(DoorBlock.OPEN)) {
				BlockPos bottom = state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
				BlockState doorState = world.getBlockState(bottom);
				handleIpPortalCreation(doorState, world, bottom);
			}
			EntranceRiftBlockEntity rift = this.getRift(world, pos, state);
			if (rift.isIpPortalLinked()) {
				EntityTarget target = rift.getTarget().as(Targets.ENTITY);
				if (target instanceof EntranceRiftBlockEntity) {
					EntranceRiftBlockEntity targetRift = ((EntranceRiftBlockEntity) target);
					BlockPos targetPos = targetRift.getPos();
					World targetWorld = targetRift.getWorld();
					BlockState targetState = targetWorld.getBlockState(targetPos);
					if (targetState.contains(Properties.OPEN)) {
						targetState = targetState.with(Properties.OPEN, state.get(Properties.OPEN));
						targetWorld.setBlockState(targetPos, targetState, 10);
					}
				}
			}
		}
		return ActionResult.SUCCESS;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean canReplace(BlockState state, ItemPlacementContext context) {
		return super.canReplace(state, context) || state.getBlock() == ModBlocks.DETACHED_RIFT;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		if (state.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
			return null;
		}
		return new EntranceRiftBlockEntity(pos, state);
	}

	public void createDetachedRift(World world, BlockPos pos) {
		createDetachedRift(world, pos, world.getBlockState(pos));
	}

	public void createDetachedRift(World world, BlockPos pos, BlockState state) {
		DoubleBlockHalf doubleBlockHalf = state.get(HALF);
		BlockPos blockPos = pos;
		BlockState blockState = world.getBlockState(pos);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
			blockPos = pos.down();
			blockState = world.getBlockState(blockPos);
			blockEntity = world.getBlockEntity(blockPos);
		}
		if (blockEntity instanceof EntranceRiftBlockEntity
				&& blockState.get(HALF) == DoubleBlockHalf.LOWER) {
			world.setBlockState(blockPos, ModBlocks.DETACHED_RIFT.getDefaultState().with(WATERLOGGED, blockState.get(WATERLOGGED)));
			((DetachedRiftBlockEntity) world.getBlockEntity(blockPos)).setData(((EntranceRiftBlockEntity) blockEntity).getData());
		}
	}


	@Override
	public EntranceRiftBlockEntity getRift(World world, BlockPos pos, BlockState state) {
		BlockEntity bottomEntity;
		BlockEntity topEntity;

		if (state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
			bottomEntity = world.getBlockEntity(pos);
			topEntity = world.getBlockEntity(pos.up());
		} else {
			bottomEntity = world.getBlockEntity(pos.down());
			topEntity = world.getBlockEntity(pos);
		}

		// TODO: Also notify player in case of error, don't crash
		if (bottomEntity instanceof EntranceRiftBlockEntity && topEntity instanceof EntranceRiftBlockEntity) {
			LOGGER.warn("Dimensional door at " + pos + " in world " + world + " contained two rifts, please report this. Defaulting to bottom.");
			return (EntranceRiftBlockEntity) bottomEntity;
		} else if (bottomEntity instanceof EntranceRiftBlockEntity) {
			return (EntranceRiftBlockEntity) bottomEntity;
		} else if (topEntity instanceof EntranceRiftBlockEntity) {
			return (EntranceRiftBlockEntity) topEntity;
		} else {
			throw new IllegalStateException("Dimensional door at " + pos + " in world " + world + " contained no rift.");
		}
	}

	@Override
	public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
		LOGGER.log(Level.INFO, DimensionalDoorsInitializer.isIpLoaded());
		DoubleBlockHalf doubleBlockHalf = state.get(HALF);
		BlockPos blockPos = pos;
		BlockState blockState = world.getBlockState(pos);
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (doubleBlockHalf == DoubleBlockHalf.UPPER) {
			blockPos = pos.down();
			blockState = world.getBlockState(blockPos);
			blockEntity = world.getBlockEntity(blockPos);
			if (blockState.isOf(state.getBlock()) && blockState.get(HALF) == DoubleBlockHalf.LOWER) {
				world.setBlockState(blockPos, world.getFluidState(blockPos).getFluid() == Fluids.WATER ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState(), 35);
				world.syncWorldEvent(player, 2001, blockPos, Block.getRawIdFromState(blockState));
			}
			if (blockEntity instanceof EntranceRiftBlockEntity
					&& blockState.get(HALF) == DoubleBlockHalf.LOWER
					&& !(player.isCreative()
					&& !DimensionalDoorsInitializer.getConfig().getDoorsConfig().placeRiftsInCreativeMode
			)
			) {
				world.setBlockState(blockPos, ModBlocks.DETACHED_RIFT.getDefaultState().with(WATERLOGGED, blockState.get(WATERLOGGED)));
				((DetachedRiftBlockEntity) world.getBlockEntity(blockPos)).setData(((EntranceRiftBlockEntity) blockEntity).getData());
			}
		}
		super.onBreak(world, pos, state, player);

	}

	@Override
	public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
		return VoxelShapes.fullCube();
	}

	@Override
	public TransformationMatrix3d.TransformationMatrix3dBuilder transformationBuilder(BlockState state, BlockPos pos) {
				if(!DimensionalDoorsInitializer.isIpLoaded()) {
					return TransformationMatrix3d.builder()
				.inverseTranslate(Vec3d.ofCenter(pos).add(Vec3d.of(state.get(DoorBlock.FACING).getVector()).multiply(-0.5)))
				.inverseRotate(MathUtil.directionEulerAngle(state.get(DoorBlock.FACING).getOpposite())); }
				return TransformationMatrix3d.builder().
						inverseTranslate(Vec3d.ofBottomCenter(pos.up()).add(Vec3d.of(state.get(DoorBlock.FACING).getVector()).multiply(-0.4997))) // -0.3124
				.inverseRotate(MathUtil.directionEulerAngle(state.get(DoorBlock.FACING).getOpposite()));

	}

	@Override
	public TransformationMatrix3d.TransformationMatrix3dBuilder rotatorBuilder(BlockState state, BlockPos pos) {
		return TransformationMatrix3d.builder()
				.inverseRotate(MathUtil.directionEulerAngle(state.get(DoorBlock.FACING).getOpposite()));
	}


	@Override
	public boolean isExitFlipped() {
		return true;
	}

	@Environment(EnvType.CLIENT)
	@Override
	public boolean isTall(BlockState cachedState) {
		return true;
	}

	static {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (player.isCreative() && !DimensionalDoorsInitializer.getConfig().getDoorsConfig().placeRiftsInCreativeMode) {
				return;
			}
			if (blockEntity instanceof EntranceRiftBlockEntity && state.get(HALF) == DoubleBlockHalf.LOWER) {
				world.setBlockState(pos, ModBlocks.DETACHED_RIFT.getDefaultState().with(WATERLOGGED, state.get(WATERLOGGED)));
				((DetachedRiftBlockEntity) world.getBlockEntity(pos)).setData(((EntranceRiftBlockEntity) blockEntity).getData());
			}
		});
	}


	@Override
	public int getPortalHeight() {
		return 2;
	}

	@Override
	public int getPortalWidth() {
		return 1;
	}

	@Override
	public void setupAsReceivingPortal(BlockState state, World world, BlockPos pos, BlockState sourceState) {
		state = state.rotate(BlockRotation.CLOCKWISE_180);
		if (sourceState.contains(Properties.DOOR_HINGE)) {
			state = state.with(Properties.DOOR_HINGE, sourceState.get(Properties.DOOR_HINGE));
		}
		world.setBlockState(pos, state, 10);
		getRift(world, pos, state).setPortalState(false);
		LOGGER.log(Level.ERROR, getRift(world, pos, state).getPortalState());
	}

	@Override
	public void setupAsSendingPortal(BlockState state, World world, BlockPos pos) {
		getRift(world, pos, state).setPortalState(true);
		LOGGER.log(Level.ERROR, getRift(world, pos, state).getPortalState());
	}
}
