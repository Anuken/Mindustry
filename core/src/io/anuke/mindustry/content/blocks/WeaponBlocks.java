package io.anuke.mindustry.content.blocks;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.entities.BulletType;
import io.anuke.mindustry.entities.effect.TeslaOrb;
import io.anuke.mindustry.graphics.fx.BulletFx;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.defense.LaserTurret;
import io.anuke.mindustry.world.blocks.types.defense.LiquidTurret;
import io.anuke.mindustry.world.blocks.types.defense.PowerTurret;
import io.anuke.mindustry.world.blocks.types.defense.Turret;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;

public class WeaponBlocks{
	public static Block
	
	doubleturret = new Turret("doubleturret"){
		{
			range = 44;
			reload = 13f;
			bullet = BulletType.stone;
			ammo = Items.stone;
			health = 45;
			shots = 2;
		}
		
		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();

			for(int i : Mathf.signs){
				tr.trns(entity.rotation, 4, -2 * i);
				bullet(tile, entity.rotation);
			}
		}
	},
	
	gatlingturret = new Turret("gatlingturret"){
		{
			range = 65;
			reload = 7f;
			bullet = BulletType.iron;
			ammo = Items.iron;
			health = 65;
		}
	},
	
	flameturret = new Turret("flameturret"){
		{
			range = 45f;
			reload = 5f;
			bullet = BulletType.flame;
			ammo = Items.coal;
			health = 90;
			inaccuracy = 4f;
		}
	},
	
	railgunturret = new Turret("railgunturret"){
		{
			shootsound = "railgun";
			range = 120;
			reload = 50f;
			bullet = BulletType.sniper;
			ammo = Items.steel;
			health = 70;
			shootEffect = BulletFx.railshot;
		}
	},
	
	flakturret = new Turret("flakturret"){
		{
			shootsound = "bigshot";
			rotatespeed = 0.2f;
			range = 120;
			reload = 55f;
			bullet = BulletType.flak;
			shots = 3;
			inaccuracy = 9f;
			ammo = Items.coal;
			ammoMultiplier = 5;
			health = 110;
			shootEffect = BulletFx.mortarshot;
			shootShake = 2f;
			size = 2;
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
	
	teslaturret = new PowerTurret("teslaturret"){
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

			float len = 4f;

			new TeslaOrb(tile.getTeam(), tile.drawx() + Angles.trnsx(entity.rotation, len), tile.drawy() + Angles.trnsy(entity.rotation, len), range, 9).add();
		}
	},

	magmaturret = new LiquidTurret("magmaturret") {
		{
			shootsound = "flame2";
			inaccuracy = 7f;
			range = 90f;
			reload = 7f;
			bullet = BulletType.plasmaflame;
			ammoLiquid = Liquids.lava;
			liquidPerShot = 3f;
			health = 180*3;
			size = 2;
		}
	},
		
	plasmaturret = new Turret("plasmaturret"){
		{
			shootsound = "flame2";
			inaccuracy = 7f;
			range = 60f;
			reload = 3f;
			bullet = BulletType.plasmaflame;
			ammo = Items.coal;
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
			ammo = Items.thorium;
			health = 430;
			size = 2;
			shootCone = 9f;
			ammoMultiplier = 8;
			shots = 2;
			shootEffect = BulletFx.chainshot;
		}

		@Override
		protected void shoot(Tile tile){
			TurretEntity entity = tile.entity();
			
			float len = 8;
			float space = 3.5f;
			
			for(int i = -1; i < 1; i ++){
				tr.trns(entity.rotation, len, Mathf.sign(i) * space);
				bullet(tile, entity.rotation);
				Effects.effect(shootEffect, tile.drawx() + tr.x,
						tile.drawy() + tr.y, entity.rotation);
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
			ammo = Items.thorium;
			health = 800;
			ammoMultiplier = 4;
			size = 3;
			rotatespeed = 0.07f;
			shootCone = 9f;
			shootEffect = BulletFx.titanshot;
			shootShake = 3f;
		}
	},

	fornaxcannon = new PowerTurret("fornaxcannon") {
		{
			shootsound = "blast";
			range = 120f;
			reload = 23f;
			bullet = BulletType.titanshell;
			ammo = Items.thorium;
			health = 800;
			ammoMultiplier = 4;
			size = 3;
			rotatespeed = 0.07f;
			shootCone = 9f;
			shootEffect = BulletFx.titanshot;
			shootShake = 3f;
		}
	},
	missileturret = new PowerTurret("missileturret") {
		{
			shootsound = "blast";
			range = 120f;
			reload = 23f;
			bullet = BulletType.titanshell;
			ammo = Items.thorium;
			health = 800;
			ammoMultiplier = 4;
			size = 2;
			rotatespeed = 0.07f;
			shootCone = 9f;
			shootEffect = BulletFx.titanshot;
			shootShake = 3f;
		}
	};
}
