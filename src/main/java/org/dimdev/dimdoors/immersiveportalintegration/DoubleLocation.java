package org.dimdev.dimdoors.immersiveportalintegration;

import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.dimdev.dimdoors.DimensionalDoorsInitializer;

public class DoubleLocation {

	RegistryKey<World> world;
	Vec3d v;
	public DoubleLocation(RegistryKey<World> w, double x, double y, double z) {
		this.world = w;
		this.setPos(x, y, z);
	}
	public DoubleLocation(RegistryKey<World> w, Vec3d vec) {
		this.world = w;
		this.v = vec;
	}
	public void setWorld(RegistryKey<World> w) {
		this.world = world;
	}
	public World getWorld(){
		return DimensionalDoorsInitializer.getServer().getWorld(world);
	}
	public void setPos(Vec3d vec)
	{
		this.v = vec;
	}
	public void setPos(double x, double y, double z)
	{
		this.v = new Vec3d(x, y, z);
	}
	public Vec3d getPos()
	{
		return this.v;
	}
	public void add(Vec3d vec)
	{
		this.v = this.v.add(vec);
	}
	public RegistryKey<World> getWorldId() {
		return this.world;
	}
}
