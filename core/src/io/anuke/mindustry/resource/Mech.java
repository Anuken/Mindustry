package io.anuke.mindustry.resource;

public enum Mech{
	normal("default"), 
	scout("scout"){{
		
	}};
	public final String name;
	public float speedBoost = 1f, damageBoost = 1f;
	public int regenRate = 10;
	public int health = 20;

	private Mech(String name){
		this.name = name;
	}
}
