package org.dimdev.dimdoors.immersiveportalintegration;

import com.qouteall.immersive_portals.my_util.DQuaternion;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.util.math.Vec3d;
import org.dimdev.dimdoors.util.Location;

import java.util.ArrayList;

public class ImmersivePortalUtil {
	public static ArrayList<Location> portalLocations = new ArrayList<Location>();
	public static ArrayList<BiDirectionalPortal> portals = new ArrayList<BiDirectionalPortal>();
	public static Portal createPortal(DoubleLocation to, DoubleLocation from, float yaw, double degrees)
	{
		//Quaternion q = new Quaternion(0, yaw , 0, 1.0F);

		Portal portal = Portal.entityType.create(from.getWorld());

		//portal.setOriginPos(Vec3d.ofBottomCenter(from.getBlockPos().up()));
		portal.setOriginPos(from.getPos());
		portal.setDestinationDimension(to.getWorldId());
		portal.setDestination(to.getPos());
		//portal.setDestination(Vec3d.ofBottomCenter(to.getBlockPos().up()));
		portal.setOrientationAndSize(
				new Vec3d(1, 0, 0).rotateY(yaw),//axisW
				new Vec3d(0, 1, 0),//axisH
				1,//width
				2//height
		);
		//portal.setHeadYaw(yaw);
		//portal.setYaw(yaw);

		portal.setRotationTransformation(DQuaternion.rotationByDegrees(new Vec3d(0,1,0),degrees).toMcQuaternion());


		portal.world.spawnEntity(portal);
		return portal;
	}


}
