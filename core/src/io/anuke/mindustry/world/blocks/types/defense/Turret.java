package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.resource.AmmoEntry;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.BlockGroup;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.function.BiConsumer;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.Angles;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;
import io.anuke.ucore.util.Translator;

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
	protected String shootsound = "shoot";

    protected int ammoPerShot = 1;
    protected float ammoEjectBack = 1f;
	protected float range = 50f;
	protected float reload = 10f;
	protected float inaccuracy = 0f;
	protected int shots = 1;
	protected float recoil = 1f;
	protected float restitution = 0.02f;
	protected float cooldown = 0.02f;
	protected float rotatespeed = 0.2f;
	protected float shootCone = 5f;
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
		hasInventory = false;
	}
	
	@Override
	public void setStats(){
		super.setStats();
		/*
		if(ammo != null) stats.add("ammo", ammo);
		if(ammo != null) stats.add("ammocapacity", maxammo);
		if(ammo != null) stats.add("ammoitem", ammoMultiplier);*/
		stats.add("range", (int)range);
		stats.add("inaccuracy", (int)inaccuracy);
		//stats.add("damageshot", bullet.damage);
		stats.add("shotssecond", Strings.toFixed(60f/reload, 1));
		stats.add("shots", shots);
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
	public void drawSelect(Tile tile){
		Draw.color(tile.getTeam().color);
		Lines.dashCircle(tile.drawx(), tile.drawy(), range);
		Draw.reset();
	}
	
	@Override
	public void drawPlace(int x, int y, int rotation, boolean valid){
		Draw.color("place");
		Lines.stroke(1f);
		Lines.dashCircle(x * tilesize + getPlaceOffset().x, y * tilesize + getPlaceOffset().y, range);
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
				
				float targetRot = Angles.predictAngle(tile.worldx(), tile.worldy(), 
						entity.target.x, entity.target.y, entity.target.velocity.x, entity.target.velocity.y, speed);
				
				if(Float.isNaN(entity.rotation)){
					entity.rotation = 0;
				}
				entity.rotation = Mathf.slerpDelta(entity.rotation, targetRot,
						rotatespeed);

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
			entity.reload += Timers.delta() * peekAmmo(tile).speedMultiplier;
		}
	}

	protected void shoot(Tile tile, AmmoType ammo){
		TurretEntity entity = tile.entity();

		entity.recoil = recoil;
		entity.heat = 1f;

		useAmmo(tile);

		tr.trns(entity.rotation, size * tilesize / 2);

		bullet(tile, ammo.bullet, entity.rotation + Mathf.range(inaccuracy));

		effects(tile);
	}
	
	protected void bullet(Tile tile, BulletType type, float angle){
		new Bullet(type, tile.entity, tile.getTeam(), tile.drawx() + tr.x, tile.drawy() + tr.y, angle).add();
	}

	protected void effects(Tile tile){
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
		public Array<AmmoEntry> ammo = new Array<>();
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
