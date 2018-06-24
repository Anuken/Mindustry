package io.anuke.mindustry.world.blocks.defense.turrets;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.Predict;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.Unit;
import io.anuke.mindustry.entities.Units;
import io.anuke.mindustry.entities.bullet.Bullet;
import io.anuke.mindustry.entities.bullet.BulletType;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.AmmoEntry;
import io.anuke.mindustry.type.AmmoType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.meta.BlockGroup;
import io.anuke.mindustry.world.meta.BlockStat;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.BiConsumer;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;

public abstract class Turret extends Block{
	protected static final int targetInterval = 15;
	
	protected final int timerTarget = timers++;

	protected Color heatColor = Palette.turretHeat;
	protected Effect shootEffect = Fx.none;
	protected Effect smokeEffect = Fx.none;
	protected Effect ammoUseEffect = Fx.none;

    protected int ammoPerShot = 1;
    protected float ammoEjectBack = 1f;
	protected float range = 50f;
	protected float reload = 10f;
	protected float inaccuracy = 0f;
	protected int shots = 1;
	protected float recoil = 1f;
	protected float restitution = 0.02f;
	protected float cooldown = 0.02f;
	protected float rotatespeed = 5f; //in degrees per tick
	protected float shootCone = 8f;
	protected float shootShake = 0f;
	protected Translator tr = new Translator();
	protected Translator tr2 = new Translator();
	protected String base = null; //name of the region to draw under turret, usually null
    protected BiConsumer<Tile, TurretEntity> drawer = (tile, entity) -> Draw.rect(name, tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
	protected BiConsumer<Tile, TurretEntity> heatDrawer = (tile, entity) ->{
		Graphics.setAdditiveBlending();
		Draw.color(heatColor);
		Draw.alpha(entity.heat);
		Draw.rect(name + "-heat", tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
		Graphics.setNormalBlending();
	};

	public Turret(String name) {
		super(name);
		update = true;
		solid = true;
		layer = Layer.turret;
		group = BlockGroup.turrets;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		/*
		if(ammo != null) stats.add("ammo", ammo);
		if(ammo != null) stats.add("ammocapacity", maxAmmo);
		if(ammo != null) stats.add("ammoitem", ammoMultiplier);*/
		stats.add(BlockStat.shootRange, (int)range);
		stats.add(BlockStat.inaccuracy, (int)inaccuracy);
		stats.add(BlockStat.reload, Strings.toFixed(60f/reload, 1));
		stats.add(BlockStat.shots, shots);
	}
	
	@Override
	public void draw(Tile tile){
		if(base == null) {
			Draw.rect("block-" + size, tile.drawx(), tile.drawy());
			if(Draw.hasRegion("block-" + size + "-top")) {
				Draw.color(tile.getTeam().color, Color.WHITE, 0.45f);
				Draw.rect("block-" + size + "-top", tile.drawx(), tile.drawy());
				Draw.color();
			}
		}else{
			Draw.rect(base, tile.drawx(), tile.drawy());
		}
	}
	
	@Override
	public void drawLayer(Tile tile){
		TurretEntity entity = tile.entity();

		tr2.trns(entity.rotation, -entity.recoil);

		drawer.accept(tile, entity);

		if(Draw.hasRegion(name + "-heat")){
			heatDrawer.accept(tile, entity);
		}

		Draw.color();
	}

	@Override
    public TextureRegion[] getBlockIcon(){
	    if(blockIcon == null){
	        blockIcon = Draw.hasRegion(name) ? new TextureRegion[]{Draw.region("block-" + size), Draw.region(name)} : new TextureRegion[0];
        }
        return blockIcon;
    }
	
	@Override
	public void drawSelect(Tile tile){
		Draw.color(tile.getTeam().color);
		Lines.dashCircle(tile.drawx(), tile.drawy(), range);
		Draw.reset();
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		Draw.color(Palette.place);
		Lines.stroke(1f);
		Lines.dashCircle(x * tilesize + offset(), y * tilesize + offset(), range);
	}

	@Override
	public void update(Tile tile){
		TurretEntity entity = tile.entity();
		
		if(entity.target != null && entity.target.isDead())
			entity.target = null;

		entity.recoil = Mathf.lerpDelta(entity.recoil, 0f, restitution);
		entity.heat = Mathf.lerpDelta(entity.heat, 0f, cooldown);
		
		if(hasAmmo(tile)){
			
			if(entity.timer.get(timerTarget, targetInterval)){
				entity.target = Units.getClosestEnemy(tile.getTeam(),
						tile.drawx(), tile.drawy(), range, e -> !e.isDead());
			}
			
			if(entity.target != null){
			    AmmoType type = peekAmmo(tile);
			    float speed = type.bullet.speed;
			    if(speed < 0.1f) speed = 9999999f;
				
				float targetRot = Predict.intercept(entity, entity.target, speed)
						.sub(tile.drawx(), tile.drawy()).angle();
				
				if(Float.isNaN(entity.rotation)){
					entity.rotation = 0;
				}

				entity.rotation = Angles.moveToward(entity.rotation, targetRot, rotatespeed * Timers.delta());

				if(Angles.angleDist(entity.rotation, targetRot) < shootCone){
					updateShooting(tile);
				}
			}
		}
	}

	/**Consume ammo and return a type.*/
	public AmmoType useAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        AmmoEntry entry = entity.ammo.peek();
        entry.amount -= ammoPerShot;
        if(entry.amount == 0) entity.ammo.pop();
        entity.totalAmmo -= ammoPerShot;
        Timers.run(reload/2f, () -> ejectEffects(tile));
        return entry.type;
    }

    /**Get the ammo type that will be returned if useAmmo is called.*/
    public AmmoType peekAmmo(Tile tile){
        TurretEntity entity = tile.entity();
        return entity.ammo.peek().type;
    }

    /**Returns whether the turret has ammo.*/
	public boolean hasAmmo(Tile tile){
		TurretEntity entity = tile.entity();
		return entity.ammo.size > 0 && entity.ammo.peek().amount >= ammoPerShot;
	}
	
	protected void updateShooting(Tile tile){
		TurretEntity entity = tile.entity();

		if(entity.reload >= reload) {
		    AmmoType type = peekAmmo(tile);

            shoot(tile, type);

            entity.reload = 0f;
        }else{
			entity.reload += Timers.delta() * peekAmmo(tile).reloadMultiplier;
		}
	}

	protected void shoot(Tile tile, AmmoType ammo){
		TurretEntity entity = tile.entity();

		entity.recoil = recoil;
		entity.heat = 1f;

		AmmoType type = peekAmmo(tile);
		useAmmo(tile);

		tr.trns(entity.rotation, size * tilesize / 2);

		bullet(tile, ammo.bullet, entity.rotation + Mathf.range(inaccuracy + type.inaccuracy));

		effects(tile);
	}
	
	protected void bullet(Tile tile, BulletType type, float angle){
		Bullet.create(type, tile.entity, tile.getTeam(), tile.drawx() + tr.x, tile.drawy() + tr.y, angle);
	}

	protected void effects(Tile tile){
		Effect shootEffect = this.shootEffect == Fx.none ? peekAmmo(tile).shootEffect : this.shootEffect;
		Effect smokeEffect = this.smokeEffect == Fx.none ? peekAmmo(tile).smokeEffect : this.smokeEffect;

		TurretEntity entity = tile.entity();

		Effects.effect(shootEffect, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);
		Effects.effect(smokeEffect, tile.drawx() + tr.x, tile.drawy() + tr.y, entity.rotation);

		if (shootShake > 0) {
			Effects.shake(shootShake, shootShake, tile.entity);
		}

		entity.recoil = recoil;
	}

	protected void ejectEffects(Tile tile){
		if(!isTurret(tile)) return;
		TurretEntity entity = tile.entity();

		Effects.effect(ammoUseEffect, tile.drawx() - Angles.trnsx(entity.rotation, ammoEjectBack),
				tile.drawy() - Angles.trnsy(entity.rotation, ammoEjectBack), entity.rotation);
	}

	protected boolean isTurret(Tile tile){
		return (tile.entity instanceof TurretEntity);
	}

	@Override
	public TileEntity getEntity(){
		return new TurretEntity();
	}
	
	public static class TurretEntity extends TileEntity{
		public TileEntity blockTarget;
		public Array<AmmoEntry> ammo = new ThreadArray<>();
		public int totalAmmo;
		public float reload;
		public float rotation = 90;
		public float recoil = 0f;
		public float heat;
		public int shots;
		public Unit target;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
		    stream.writeByte(ammo.size);
		    for(AmmoEntry entry : ammo){
                stream.writeByte(entry.type.id);
                stream.writeShort(entry.amount);
            }
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			byte amount = stream.readByte();
			for(int i = 0; i < amount; i ++){
			    AmmoType type = AmmoType.getByID(stream.readByte());
			    short ta = stream.readShort();
			    ammo.add(new AmmoEntry(type, ta));
			    totalAmmo += ta;
            }
		}
	}
}
