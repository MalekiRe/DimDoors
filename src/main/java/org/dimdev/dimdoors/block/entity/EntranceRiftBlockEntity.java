package org.dimdev.dimdoors.block.entity;

import java.util.Optional;

import com.sun.media.jfxmedia.logging.Logger;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.block.Block;
import net.minecraft.datafixer.fix.ChunkPalettedStorageFix;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import org.dimdev.dimdoors.ModConfig;
import org.dimdev.dimdoors.immersiveportalintegration.BiDirectionalPortal;
import org.dimdev.dimdoors.immersiveportalintegration.DoubleLocation;
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
	BiDirectionalPortal portal;
	Location originCheck;
	Location destCheck;
	int portalIndex;
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
	public void destroyPortal() {
		if(portalCreated) {
			EntityTarget target = this.getTarget().as(Targets.ENTITY);
			ImmersivePortalUtil.portalLocations.remove(originCheck);
			ImmersivePortalUtil.portalLocations.remove(destCheck);
			ImmersivePortalUtil.portals.remove(portal);
			portal.destroyPortals();
			portalCreated = false;
			System.out.println("Destorying Portals");
		}
		else
		{

		}
	}
	public void createPortal()
	{
		if(!portalCreated) {

			portalCreated = true;
			EntityTarget target = this.getTarget().as(Targets.ENTITY);
			if (target instanceof RiftBlockEntity) {
				RiftBlockEntity riftTarget = (RiftBlockEntity) target;
				DoubleLocation origin = new DoubleLocation(this.getWorld().getRegistryKey(), Vec3d.of(this.getPos()));
				DoubleLocation dest = new DoubleLocation(riftTarget.getWorld().getRegistryKey(), Vec3d.of(riftTarget.getPos()));
				Location originCheck = new Location(this.getWorld().getRegistryKey(), this.getPos());
				Location destCheck = new Location(riftTarget.getWorld().getRegistryKey(), riftTarget.getPos());
				//float rotation = 0;
				/*
				double firstTransform = 180;
				double secondTransform = 180;
				double rotTo = (float) ((float) Math.toRadians(world.getBlockState(this.getPos()).get(DoorBlock.FACING).asRotation()));
				rotTo = 0;

				double rotFrom = (float) ((float) Math.toRadians(riftTarget.getWorld().getBlockState(riftTarget.getPos()).get(DoorBlock.FACING).asRotation()));
				rotFrom = 0;
				switch(world.getBlockState(this.getPos()).get(DoorBlock.FACING))
				{
					case SOUTH: rotTo = Math.PI; secondTransform += 90;





				}
				*/
				double enteringTransform = 270;
				double exitingTransform = 270;

				double entranceRotation = 0;
				double exitRotation = 0;
				double threePixels = 0.999;
				//origin.add(new Vec3d(1.0, 2.0, 0.0));
				Vec3d north = new Vec3d(0.5, 1, 1 - threePixels);
				Vec3d south = new Vec3d(0.5, 1, threePixels);
				Vec3d east = new Vec3d(threePixels, 1, 0.5);
				Vec3d west = new Vec3d(1.0-threePixels, 1, 0.5);

				threePixels = -0.001;

				Vec3d north2 = new Vec3d(0.5, 1, 1 - threePixels);
				Vec3d south2 = new Vec3d(0.5, 1, threePixels);
				Vec3d east2 = new Vec3d(threePixels, 1, 0.5);
				Vec3d west2 = new Vec3d(1.0-threePixels, 1, 0.5);
				switch(world.getBlockState(this.getPos()).get(DoorBlock.FACING))
				{
					case EAST : entranceRotation = Math.toRadians(270); origin.add(east); break;
					case WEST : entranceRotation = Math.toRadians(90); origin.add(west); enteringTransform -= 180; break;
					case NORTH : entranceRotation = Math.toRadians(0); origin.add(north); enteringTransform -= 90; break;
					case SOUTH : entranceRotation = Math.toRadians(180); origin.add(south); enteringTransform -= 270; break;
					default : break;

				}
				switch(riftTarget.getWorld().getBlockState(riftTarget.getPos()).get(DoorBlock.FACING))
				{
					case EAST : exitRotation = Math.toRadians(270); dest.add(east2); break;
					case WEST : exitRotation = Math.toRadians(90); dest.add(west2); exitingTransform -= 180; break;
					case NORTH : exitRotation = Math.toRadians(0); dest.add(north2); exitingTransform -= 90; break;
					case SOUTH : exitRotation = Math.toRadians(180); dest.add(south2); exitingTransform -= 270; break;
					default : break;

				}
				System.out.println(origin.getPos().toString());
				/*
				switch(riftTarget.getWorld().getBlockState(riftTarget.getPos()).get(DoorBlock.FACING))
				{

					case EAST : exitRotation = Math.toRadians(270); break;
					case WEST : exitRotation = Math.toDegrees(0); break;
					case NORTH : exitRotation = Math.toDegrees(-20); break;
					case SOUTH : exitRotation = Math.toDegrees(0); break;
					default : break;

				}
				*/
				exitingTransform += Math.toDegrees(entranceRotation);
				enteringTransform += Math.toDegrees(exitRotation);


				if(!ImmersivePortalUtil.portalLocations.contains(originCheck) && !ImmersivePortalUtil.portalLocations.contains(destCheck)) {
					portal = new BiDirectionalPortal(origin, dest, entranceRotation, exitRotation, enteringTransform, exitingTransform);
					ImmersivePortalUtil.portalLocations.add(originCheck);
					ImmersivePortalUtil.portalLocations.add(destCheck);
					ImmersivePortalUtil.portals.add(portal);
					portalIndex = ImmersivePortalUtil.portals.indexOf(portal);
					System.out.println("Created Portal");
				}
				else{
					portal = ImmersivePortalUtil.portals.get(((EntranceRiftBlockEntity)destCheck.getWorld().getBlockEntity(destCheck.getBlockPos())).portalIndex);
					portalIndex = ((EntranceRiftBlockEntity)destCheck.getWorld().getBlockEntity(destCheck.getBlockPos())).portalIndex;
				}
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
