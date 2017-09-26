package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.Color;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.effect.TeslaOrb;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.LaserTurret;
import io.anuke.mindustry.world.blocks.types.RepairTurret;
import io.anuke.mindustry.world.blocks.types.Turret;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class WeaponBlocks{
	public static Block
	
	turret = new Turret("turret"){
		{
			formalName = "turret";
			range = 50;
			reload = 15f;
			bullet = BulletType.stone;
			ammo = Item.stone;
		}
	},
	
	doubleturret = new Turret("doubleturret"){
		{
			formalName = "double turret";
			range = 40;
			reload = 13f;
			bullet = BulletType.stone;
			ammo = Item.stone;
			health = 50;
		}
		
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			
			vector.set(4, -2).rotate(entity.rotation);
				bullet(tile, entity.rotation);
				
			vector.set(4, 2).rotate(entity.rotation);
				bullet(tile, entity.rotation);
		}
	},
	
	machineturret = new Turret("machineturret"){
		{
			formalName = "gattling turret";
			range = 65;
			reload = 7f;
			bullet = BulletType.iron;
			ammo = Item.iron;
			health = 65;
		}
	},
	
	shotgunturret = new Turret("shotgunturret"){
		{
			formalName = "splitter turret";
			range = 50;
			reload = 30f;
			bullet = BulletType.iron;
			ammo = Item.iron;
			health = 70;
		}
		
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			
			for(int i = 0; i < 7; i ++)
				Timers.run(i/1.5f, ()->{
					vector.set(4, 0).setAngle(entity.rotation);
					bullet(tile, entity.rotation + Mathf.range(30));
				});
		}
	},
	
	flameturret = new Turret("flameturret"){
		{
			formalName = "flamer turret";
			range = 35f;
			reload = 5f;
			bullet = BulletType.flame;
			ammo = Item.coal;
			health = 90;
		}
	},
	
	sniperturret = new Turret("sniperturret"){
		{
			formalName = "railgun turret";
			range = 120;
			reload = 50f;
			bullet = BulletType.sniper;
			ammo = Item.steel;
			health = 70;
		}
	},
	
	//TODO
	mortarturret = new Turret("mortarturret"){
		{
			rotatespeed = 0.1f;
			formalName = "flak turret";
			range = 120;
			reload = 100f;
			bullet = BulletType.shell;
			ammo = Item.coal;
			ammoMultiplier = 5;
			health = 110;
		}
	},
	
	//TODO
	laserturret = new LaserTurret("laserturret"){
		
		
		{
			beamColor = Color.SKY;
			formalName = "laser turret";
			range = 60;
			reload = 4f;
			damage = 10;
			ammo = Item.coal;
			health = 110;
			ammoMultiplier = 60;
		}
	},
	
	//TODO
	teslaturret = new Turret("waveturret"){
		{
			formalName = "tesla turret";
			range = 70;
			reload = 15f;
			bullet = BulletType.shell;
			ammo = Item.coal;
			health = 140;
		}
		
		@Override
		public void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			Angles.translation(entity.rotation, 4);

			new TeslaOrb(tile.worldx() + Angles.x(), tile.worldy() + Angles.y(), 
					70, (int)(9*Vars.multiplier)).add();
		}
	},
		
	//TODO
	plasmaturret = new Turret("plasmaturret"){
		{
			inaccuracy = 7f;
			formalName = "plasma turret";
			range = 60f;
			reload = 2f;
			bullet = BulletType.plasmaflame;
			ammo = Item.coal;
			health = 180;
			ammoMultiplier = 40;
		}
	},
	
	repairturret = new RepairTurret("repairturret"){
		{
			formalName = "heal turret";
			range = 30;
			reload = 40f;
			health = 60;
		}
	},
	
	megarepairturret = new RepairTurret("megarepairturret"){
		{
			formalName = "heal turret II";
			range = 50;
			reload = 20f;
			health = 90;
		}
	};
}
