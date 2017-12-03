package io.anuke.mindustry.resource;

public enum Mech{
	standard, 
	scout{{
		
	}};
	public float speedBoost = 1f, damageBoost = 1f;
	public int regenRate = 10;
	public int health = 20;

	private Mech(){
		
	}
}
