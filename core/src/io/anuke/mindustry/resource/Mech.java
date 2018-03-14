package io.anuke.mindustry.resource;

public class Mech extends Upgrade{
	public static final Mech

	standard = new Mech("standard", false),
	standardShip = new Mech("standard-ship", true);

	public boolean flying;
	public float mass = 1f;

	public Mech(String name, boolean flying){
		super(name);
		this.flying = flying;
	}
}
