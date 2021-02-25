package org.dimdev.dimdoors.immersiveportalintegration;

import com.qouteall.immersive_portals.portal.Portal;
import org.dimdev.dimdoors.util.Location;

public class BiDirectionalPortal {

	Portal portal1;
	Portal portal2;
	DoubleLocation location1;
	DoubleLocation location2;
	public BiDirectionalPortal(Portal p1, Portal p2)
	{
		this.portal1 = p1;
		this.portal2 = p2;
	}
	public BiDirectionalPortal(DoubleLocation to, DoubleLocation from, double entranceRotation, double exitRotation, double enteringTransform, double exitingTransform) {

		portal1 = ImmersivePortalUtil.createPortal(from, to, (float) entranceRotation, enteringTransform);
		portal2 = ImmersivePortalUtil.createPortal(to, from, (float) exitRotation, exitingTransform);


		this.location1 = to;
		this.location2 = from;

	}
	public boolean locationTaken(Location checkLocation) {
		return location1.equals(checkLocation) || location2.equals(checkLocation);
	}
	public void destroyPortals(){

		portal1.remove();
		portal2.remove();

	}

}
