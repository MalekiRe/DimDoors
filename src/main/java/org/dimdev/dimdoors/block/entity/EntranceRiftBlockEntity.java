package org.dimdev.dimdoors.block.entity;

import java.util.Optional;

import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import org.dimdev.dimdoors.ModConfig;
import org.dimdev.dimdoors.immersiveportalintegration.ImmersivePortalUtil;
import org.dimdev.dimdoors.rift.targets.EntityTarget;
import org.dimdev.dimdoors.rift.targets.Targets;
import org.dimdev.dimdoors.util.Location;
import org.dimdev.dimdoors.util.TeleportUtil;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class EntranceRiftBlockEntity extends RiftBlockEntity {
	boolean portalCreated = false;
	public EntranceRiftBlockEntity() {
		super(ModBlockEntityTypes.ENTRANCE_RIFT);
	}

	@Override
	public void fromTag(BlockState state, CompoundTag nbt) {
		super.fromTag(state, nbt);
		if(nbt.contains("portalCreated")) {
			portalCreated = nbt.getBoolean("portalCreated");
		}
	}

	@Override
	public CompoundTag toTag(CompoundTag tag) {
		tag = super.toTag(tag);

		if(portalCreated)
			tag.putBoolean("portalCreated", portalCreated);
		return tag;
	}

	@Override
	public boolean teleport(Entity entity) {
		boolean status = super.teleport(entity);

		if (this.riftStateChanged && !this.data.isAlwaysDelete()) {
			this.markDirty();
		}

		return status;
	}

	public void createPortal()
	{
		if(!portalCreated) {
			portalCreated = true;
			EntityTarget target = this.getTarget().as(Targets.ENTITY);
			if (target instanceof RiftBlockEntity) {

				RiftBlockEntity riftTarget = (RiftBlockEntity) target;
				ImmersivePortalUtil.createBiDirectionalPortal(new Location(this.getWorld().getRegistryKey(), this.getPos()), new Location(riftTarget.getWorld().getRegistryKey(), riftTarget.getPos()), (float) (world.getBlockState(this.getPos()).get(DoorBlock.FACING).asRotation()*Math.PI/180));
			}
		}
	}

	@Override
	public boolean receiveEntity(Entity entity, float yawOffset) {
		Vec3d targetPos = Vec3d.ofCenter(this.pos).add(Vec3d.of(this.getOrientation().getOpposite().getVector()).multiply(ModConfig.INSTANCE.getGeneralConfig().teleportOffset + 0.5));
		TeleportUtil.teleport(entity, this.world, targetPos, yawOffset);
		return true;
	}

	public Direction getOrientation() {
		//noinspection ConstantConditions
		return Optional.of(this.world.getBlockState(this.pos))
				.filter(state -> state.contains(HorizontalFacingBlock.FACING))
				.map(state -> state.get(HorizontalFacingBlock.FACING))
				.orElse(Direction.NORTH);
	}

	/**
	 * Specifies if the portal should be rendered two blocks tall
	 */
	public boolean isTall() {
		return this.getCachedState().getBlock() instanceof DoorBlock;
	}

	@Override
	public boolean isDetached() {
		return false;
	}
}
