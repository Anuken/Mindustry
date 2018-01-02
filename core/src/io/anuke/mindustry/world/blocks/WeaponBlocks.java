package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.effect.TeslaOrb;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.LaserTurret;
import io.anuke.mindustry.world.blocks.types.defense.PowerTurret;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class WeaponBlocks{
	public static Block
	
	turret = new Turret("turret"){
		{
			range = 52;
			reload = 15f;
			bullet = BulletType.stone;
			health = 50;
			ammo = Item.stone;
		}
	},
	
	doubleturret = new Turret("doubleturret"){
		{
			range = 44;
			reload = 13f;
			bullet = BulletType.stone;
			ammo = Item.stone;
			health = 55;
			health = 50;
		}
		
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			
			Angles.vector.set(4, -2).rotate(entity.rotation);
			bullet(tile, entity.rotation);
				
			Angles.vector.set(4, 2).rotate(entity.rotation);
			bullet(tile, entity.rotation);
		}
	},
	
	machineturret = new Turret("machineturret"){
		{
			range = 65;
			reload = 7f;
			bullet = BulletType.iron;
			ammo = Item.iron;
			health = 65;
		}
	},
	
	shotgunturret = new Turret("shotgunturret"){
		{
			range = 50;
			reload = 30f;
			bullet = BulletType.iron;
			ammo = Item.iron;
			health = 70;
			shots = 5;
			inaccuracy = 15f;
			shotDelayScale = 0.7f;
		}
	},
	
	flameturret = new Turret("flameturret"){
		{
			range = 35f;
			reload = 5f;
			bullet = BulletType.flame;
			ammo = Item.coal;
			health = 90;
		}
	},
	
	sniperturret = new Turret("sniperturret"){
		{
			shootsound = "railgun";
			range = 120;
			reload = 50f;
			bullet = BulletType.sniper;
			ammo = Item.steel;
			health = 70;
			shootEffect = Fx.railshot;
		}
	},
	
	mortarturret = new Turret("mortarturret"){
		{
			shootsound = "bigshot";
			rotatespeed = 0.1f;
			range = 120;
			reload = 100f;
			bullet = BulletType.shell;
			ammo = Item.coal;
			ammoMultiplier = 5;
			health = 110;
			shootEffect = Fx.mortarshot;
			shootShake = 2f;
		}
	},
	
	laserturret = new LaserTurret("laserturret"){
		{
			shootsound = "laser";
			beamColor = Color.SKY;
			range = 60;
			reload = 4f;
			damage = 10;
			health = 110;
			powerUsed = 0.2f;
		}
	},
	
	teslaturret = new PowerTurret("waveturret"){
		{
			shootsound = "tesla";
			range = 70;
			reload = 15f;
			bullet = BulletType.shell;
			health = 140;
		}
		
		@Override
		public void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			Angles.translation(entity.rotation, 4);

			new TeslaOrb(tile.worldx() + Angles.x(), tile.worldy() + Angles.y(), 
					range, (int)(9*Vars.multiplier)).add();
		}
	},
		
	plasmaturret = new Turret("plasmaturret"){
		{
			shootsound = "flame2";
			inaccuracy = 7f;
			range = 60f;
			reload = 3f;
			bullet = BulletType.plasmaflame;
			ammo = Item.coal;
			health = 180;
			ammoMultiplier = 40;
		}
	},
	
	chainturret = new Turret("chainturret"){
		{
			shootsound = "bigshot";
			inaccuracy = 8f;
			range = 80f;
			reload = 8f;
			bullet = BulletType.chain;
			ammo = Item.uranium;
			health = 430;
			width = height = 2;
			shootCone = 9f;
			ammoMultiplier = 8;
			shots = 2;
			shootEffect = Fx.chainshot;
		}
		
		//TODO specify turret shootInternal effect in turret instead of doing it manually
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			Vector2 offset = getPlaceOffset();
			
			float len = 8;
			float space = 3.5f;
			
			for(int i = -1; i < 1; i ++){
				Angles.vector.set(len, Mathf.sign(i) * space).rotate(entity.rotation);
				bullet(tile, entity.rotation);
				Effects.effect(shootEffect, tile.worldx() + Angles.x() + offset.x, 
						tile.worldy()+ Angles.y() + offset.y, entity.rotation);
			}
			
			Effects.shake(1f, 1f, tile.worldx(), tile.worldy());
		}
	},
	
	titanturret = new Turret("titancannon"){
		{
			shootsound = "blast";
			range = 120f;
			reload = 23f;
			bullet = BulletType.titanshell;
			ammo = Item.uranium;
			health = 800;
			ammoMultiplier = 4;
			width = height = 3;
			rotatespeed = 0.07f;
			shootCone = 9f;
			shootEffect = Fx.titanshot;
			shootShake = 3f;
		}
	};
}
