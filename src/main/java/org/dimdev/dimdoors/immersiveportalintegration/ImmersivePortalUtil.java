package org.dimdev.dimdoors.immersiveportalintegration;

import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.util.math.Vec3d;
import org.dimdev.dimdoors.util.Location;

public class ImmersivePortalUtil {
	public static void createBiDirectionalPortal(Location to, Location from, float rot)
	{
		createPortal(to, from, rot);
		createPortal(from, to, rot + (float) Math.PI);
	}
	public static void createPortal(Location to, Location from, float yaw)
	{
		//Quaternion q = new Quaternion(0, yaw , 0, 1.0F);

		Portal portal = Portal.entityType.create(from.getWorld());
		portal.setOriginPos(Vec3d.ofBottomCenter(from.getBlockPos().up()));

		portal.setDestinationDimension(to.getWorldId());

		portal.setDestination(Vec3d.ofBottomCenter(to.getBlockPos().up()));
		portal.setOrientationAndSize(
				new Vec3d(1, 0, 0).rotateY(yaw),//axisW
				new Vec3d(0, 1, 0),//axisH
				1,//width
				2//height
		);
		//portal.setHeadYaw(yaw);
		//portal.setYaw(yaw);

		//portal.setRotationTransformation(q);


		portal.world.spawnEntity(portal);
	}


}
