package io.anuke.mindustry.world.blocks;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.effect.Fx;
import io.anuke.mindustry.entities.effect.TeslaOrb;
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
			formalName = "turret";
			range = 52;
			reload = 15f;
			bullet = BulletType.stone;
			ammo = Item.stone;
			fullDescription = "A basic, cheap turret. Uses stone for ammo. Has slightly more range than the double-turret.";
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
			fullDescription = "A slightly more powerful version of the turret. Uses stone for ammo. Does significantly more damage, but has a lower range. Shoots two bullets.";
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
			fullDescription = "A standard all-around turret. Uses iron for ammo. Has a fast fire rate with decent damage.";
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
			shots = 7;
			inaccuracy = 30f;
			shotDelayScale = 0.7f;
			fullDescription = "A standard turret. Uses iron for ammo. Shoots a spread of 7 bullets. "
					+ "Lower range, but higher damage output than the gattling turret.";
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
			fullDescription = "Advanced close-range turret. Uses coal for ammo. Has very low range, but very high damage and damage. "
					+ "Good for close quarters. Recommended to be used behind walls.";
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
			shootEffect = Fx.railshot;
			fullDescription = "Advanced long-range turret. Uses steel for ammo. Very high damage, but low fire rate. "
					+ "Expensive to use, but can be placed far away from enemy lines due to its range.";
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
			fullDescription = "Advanced splash-damage turret. Uses coal for ammo. "
					+ "Very slow fire rate and bullets, but very high single-target and splash damage. "
					+ "Useful for large crowds of enemies.";
			shootEffect = Fx.mortarshot;
			shootShake = 2f;
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
			fullDescription = "Advanced single-target turret. Uses power. Good medium-range all-around turret. "
					+ "Single-target only. Never misses.";
		}
	},
	
	teslaturret = new PowerTurret("waveturret"){
		{
			formalName = "tesla turret";
			range = 70;
			reload = 15f;
			bullet = BulletType.shell;
			health = 140;
			fullDescription = "Advanced multi-target turret. Uses power. Medium range. Never misses."
					+ "Average to low damage, but can hit multiple enemies simultaneously with chain lighting.";
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
			fullDescription = "Highly advanced version of the flamer turret. Uses coal as ammo. "
					+ "Very high damage, low to medium range.";
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
			ammoMultiplier = 8;
			shots = 2;
			fullDescription = "The ultimate rapid-fire turret. Uses uranium as ammo. Shoots large slugs at a high fire rate. "
					+ "Medium range. Spans multiple tiles. Extremely tough.";
			shootEffect = Fx.chainshot;
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
				Effects.effect(shootEffect, tile.worldx() + Angles.x() + offset.x, 
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
			ammoMultiplier = 4;
			width = height = 3;
			rotatespeed = 0.07f;
			shootCone = 9f;
			fullDescription = "The ultimate long-range turret. Uses uranium as ammo. Shoots large splash-damage shells at a medium rate of fire. "
					+ "Long range. Spans multiple tiles. Extremely tough.";
			shootEffect = Fx.titanshot;
			shootShake = 3f;
		}
	};
}
