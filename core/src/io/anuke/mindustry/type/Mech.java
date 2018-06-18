package io.anuke.mindustry.type;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Weapons;
import io.anuke.ucore.graphics.Draw;

public class Mech extends Upgrade {
	public boolean flying;
	public float speed = 1.1f;
	public float maxSpeed = 1.1f;
	public float mass = 1f;
	public int drillPower = -1;
	public float carryWeight = 1f;
	public float armor = 1f;
	public Weapon weapon = Weapons.blaster;

	public TextureRegion baseRegion, legRegion, region;

	public Mech(String name, boolean flying){
		super(name);
		this.flying = flying;
	}

	@Override
	public void load() {
		if (!flying){
			legRegion = Draw.region(name + "-leg");
			baseRegion = Draw.region(name + "-base");
		}

		region = Draw.region(name);
	}
}
