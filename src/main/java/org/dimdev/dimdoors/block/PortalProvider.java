package org.dimdev.dimdoors.block;


import net.minecraft.block.BlockState;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Quaternion;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.dimdev.dimdoors.api.util.math.TransformationMatrix3d;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.my_util.DQuaternion;

public interface PortalProvider extends CoordinateTransformerProvider {
	int getPortalHeight();

	int getPortalWidth();
	 static DQuaternion getRotationBetween(Vec3d from, Vec3d to) {
		from = from.normalize();
		to = to.normalize();
		Vec3d axis = from.crossProduct(to).normalize();
		double cos = from.dotProduct(to);
		double angle = Math.acos(cos);
		return DQuaternion.rotationByRadians(axis, angle);
	}

	default Pair<Portal, Portal> createTwoSidedUnboundPortal(BlockState state, World world, BlockPos pos, TransformationMatrix3d.TransformationMatrix3dBuilder targetRotatorBuilder) {
		TransformationMatrix3d.TransformationMatrix3dBuilder transformationBuilder = transformationBuilder(state, pos);
		Vec3d origin = this.transformOut(transformationBuilder, new Vec3d(0, 0, 0));

		TransformationMatrix3d.TransformationMatrix3dBuilder rotatorBuilder = rotatorBuilder(state, pos);
		Vec3d axisW = this.transformOut(rotatorBuilder, new Vec3d(1, 0, 0));
		Vec3d axisH = this.transformOut(rotatorBuilder, new Vec3d(0, 1, 0));

		Vec3d forwards = new Vec3d(0, 0, 1);

		Vec3d first = transformOut(rotatorBuilder, forwards);
		Vec3d second = transformOut(targetRotatorBuilder, forwards);
		Quaternion rotationTransformation = null;
		if (first.squaredDistanceTo(second.multiply(-1)) < 0.01) {
			rotationTransformation = DQuaternion.rotationByDegrees(new Vec3d(0, 1, 0), 180).toMcQuaternion(); // weird edge case
		} else {
			rotationTransformation = getRotationBetween(first, second).toMcQuaternion();
		}

		Portal portal = Portal.entityType.create(world);
		portal.setOriginPos(origin);
		portal.setOrientationAndSize(axisW, axisH, getPortalWidth(), getPortalHeight());
		portal.setRotationTransformation(rotationTransformation);

		Portal flippedPortal = Portal.entityType.create(world);
		flippedPortal.setOriginPos(origin);
		flippedPortal.setOrientationAndSize(axisW.multiply(-1), axisH, getPortalWidth(), getPortalHeight());
		flippedPortal.setRotationTransformation(rotationTransformation);

		return new Pair<>(portal, flippedPortal);
	}

	default Portal createUnboundPortal(BlockState state, World world, BlockPos pos) {
		TransformationMatrix3d.TransformationMatrix3dBuilder transformationBuilder = transformationBuilder(state, pos);
		Vec3d origin = this.transformOut(transformationBuilder, new Vec3d(0, 0, 0));

		TransformationMatrix3d.TransformationMatrix3dBuilder rotatorBuilder = rotatorBuilder(state, pos);
		Vec3d axisW = this.transformOut(rotatorBuilder, new Vec3d(1, 0, 0));
		Vec3d axisH = this.transformOut(rotatorBuilder, new Vec3d(0, 1, 0));

		Portal portal = Portal.entityType.create(world);
		portal.setOriginPos(origin);
		portal.setOrientationAndSize(axisW, axisH, getPortalWidth(), getPortalHeight());

		return portal;
	}

	void setupAsReceivingPortal(BlockState state, World world, BlockPos pos, BlockState sourceState);

	void setupAsSendingPortal(BlockState state, World world, BlockPos pos);
}
