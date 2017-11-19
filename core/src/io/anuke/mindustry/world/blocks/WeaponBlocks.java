package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Fx;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.effect.TeslaOrb;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.LaserTurret;
import io.anuke.mindustry.world.blocks.types.defense.PowerTurret;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class WeaponBlocks{
	public static Block
	
	turret = new Turret("turret"){
		{
			formalName = "turret";
			range = 52;
			reload = 15f;
			bullet = BulletType.stone;
			ammo = Item.stone;
		}
	},
	
	doubleturret = new Turret("doubleturret"){
		{
			formalName = "double turret";
			range = 44;
			reload = 13f;
			bullet = BulletType.stone;
			ammo = Item.stone;
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
					Angles.translation(entity.rotation, 4f);
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
	
	laserturret = new LaserTurret("laserturret"){
		{
			beamColor = Color.SKY;
			formalName = "laser turret";
			range = 60;
			reload = 4f;
			damage = 10;
			health = 110;
			powerUsed = 0.2f;
		}
	},
	
	teslaturret = new PowerTurret("waveturret"){
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
					range, (int)(9*Vars.multiplier)).add();
		}
	},
		
	plasmaturret = new Turret("plasmaturret"){
		{
			inaccuracy = 7f;
			formalName = "plasma turret";
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
			inaccuracy = 8f;
			formalName = "chain turret";
			range = 80f;
			reload = 7f;
			bullet = BulletType.chain;
			ammo = Item.uranium;
			health = 430;
			width = height = 2;
			shootCone = 9f;
		}
		
		//TODO specify turret shoot effect in turret instead of doing it manually
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			Vector2 offset = getPlaceOffset();
			
			float len = 8;
			float space = 3.5f;
			
			for(int i = -1; i < 1; i ++){
				Angles.vector.set(len, Mathf.sign(i) * space).rotate(entity.rotation);
				bullet(tile, entity.rotation);
				Effects.effect(Fx.chainshot, tile.worldx() + Angles.x() + offset.x, 
						tile.worldy()+ Angles.y() + offset.y, entity.rotation);
			}
			
			Effects.shake(1f, 1f, tile.worldx(), tile.worldy());
		}
	},
	
	titanturret = new Turret("titancannon"){
		{
			formalName = "titan cannon";
			range = 120f;
			reload = 20f;
			bullet = BulletType.titanshell;
			ammo = Item.uranium;
			health = 800;
			ammoMultiplier = 5;
			width = height = 3;
			rotatespeed = 0.07f;
			shootCone = 9f;
		}
		
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			Vector2 offset = getPlaceOffset();
			
			Angles.translation(entity.rotation, 14f);
			bullet(tile, entity.rotation);
			Effects.effect(Fx.titanshot, tile.worldx() + Angles.x() + offset.x, 
						tile.worldy()+ Angles.y() + offset.y, entity.rotation);
			
			Effects.shake(3f, 3f, tile.worldx(), tile.worldy());
		}
	};
}
