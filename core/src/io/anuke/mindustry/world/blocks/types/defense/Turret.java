package io.anuke.mindustry.world.blocks.types.defense;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.entities.*;
import io.anuke.mindustry.graphics.Layer;
import io.anuke.mindustry.graphics.fx.Fx;
import io.anuke.mindustry.resource.AmmoType;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.*;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.util.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;

public class Turret extends Block{
	protected static final int targetInterval = 15;
	
	protected final int timerTarget = timers++;

    protected int maxammo = 100;
    //TODO implement this!
    /**A value of 'null' means this turret does not need ammo.*/
    protected AmmoType[] ammoTypes;
    protected ObjectMap<Item, AmmoType> ammoMap = new ObjectMap<>();

    protected int ammoPerShot = 1;
    protected float ammoEjectBack = 1f;
	protected float range = 50f;
	protected float reload = 10f;
	protected float inaccuracy = 0f;
	protected int shots = 1;
	protected float recoil = 1f;
	protected float restitution = 0.02f;
	protected float rotatespeed = 0.2f;
	protected float shootCone = 5f;
	protected float shootShake = 0f;
	protected Translator tr = new Translator();
	protected Translator tr2 = new Translator();
	protected String base = null; //name of the region to draw under turret, usually null

	protected Effect shootEffect = Fx.none;
	protected Effect smokeEffect = Fx.none;
	protected Effect ammoUseEffect = Fx.none;
    protected String shootsound = "shoot";

	public Turret(String name) {
		super(name);
		update = true;
		solid = true;
		layer = Layer.turret;
		group = BlockGroup.turrets;
		hasInventory = false;
	}

	@Override
	public void init(){
	    super.init();

	    if(ammoTypes != null) {
            for (AmmoType type : ammoTypes) {
                if (ammoMap.containsKey(type.item)) {
                    throw new RuntimeException("Turret \"" + name + "\" has two conflicting ammo entries on item type " + type.item + "!");
                } else {
                    ammoMap.put(type.item, type);
                }
            }
        }
	}

	@Override
	public void setBars(){
		bars.replace(new BlockBar(BarType.inventory, true, tile -> (float)tile.<TurretEntity>entity().totalAmmo / maxammo));
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

		Draw.rect(name(), tile.drawx() + tr2.x, tile.drawy() + tr2.y, entity.rotation - 90);
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
		Lines.dashCircle(x * tilesize, y * tilesize, range);
	}

    @Override
    public void handleItem(Item item, Tile tile, Tile source) {
        TurretEntity entity = tile.entity();

        AmmoType type = ammoMap.get(item);
        entity.totalAmmo += type.quantityMultiplier;

        //find ammo entry by type
        for(int i = 0; i < entity.ammo.size; i ++){
            AmmoEntry entry = entity.ammo.get(i);

            //if found, put it to the right
            if(entry.type == type){
                entry.amount += type.quantityMultiplier;
                entity.ammo.swap(i, entity.ammo.size-1);
                return;
            }
        }

        //must not be found
        AmmoEntry entry = new AmmoEntry(type, type.quantityMultiplier);
        entity.ammo.add(entry);

    }

    @Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
        TurretEntity entity = tile.entity();

        return ammoMap != null && ammoMap.get(item) != null && entity.totalAmmo + ammoMap.get(item).quantityMultiplier <= maxammo;
	}
	
	@Override
	public void update(Tile tile){
		TurretEntity entity = tile.entity();
		
		if(entity.target != null && entity.target.isDead())
			entity.target = null;

		entity.recoil = Mathf.lerpDelta(entity.recoil, 0f, restitution);
		
		if(hasAmmo(tile)){
			
			if(entity.timer.get(timerTarget, targetInterval)){
				entity.target = Units.getClosestEnemy(tile.getTeam(),
						tile.drawx(), tile.drawy(), range, e -> !e.isDead());
			}
			
			if(entity.target != null){
			    AmmoType type = peekAmmo(tile);
				
				float targetRot = Angles.predictAngle(tile.worldx(), tile.worldy(), 
						entity.target.x, entity.target.y, entity.target.velocity.x, entity.target.velocity.y, type.bullet.speed);
				
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
        ejectEffects(tile);
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

		useAmmo(tile);

		tr.trns(entity.rotation, size * tilesize / 2);

		bullet(tile, ammo.bullet, entity.rotation + Mathf.range(inaccuracy));

		Effects.effect(shootEffect, tile.drawx() + tr.x,
				tile.drawy() + tr.y, entity.rotation);

		if (shootShake > 0) {
			Effects.shake(shootShake, shootShake, tile.entity);
		}
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
		TurretEntity entity = tile.entity();

		Effects.effect(ammoUseEffect, tile.drawx() - Angles.trnsx(entity.rotation, ammoEjectBack),
				tile.drawy() - Angles.trnsy(entity.rotation, ammoEjectBack), entity.rotation);
	}

	@Override
	public TileEntity getEntity(){
		return new TurretEntity();
	}

	public static class AmmoEntry{
		public final AmmoType type;
		public int amount;

        public AmmoEntry(AmmoType type, int amount) {
            this.type = type;
            this.amount = amount;
        }
    }
	
	public static class TurretEntity extends TileEntity{
		public TileEntity blockTarget;
		public Array<AmmoEntry> ammo = new Array<>();
		public int totalAmmo;
		public float reload;
		public float rotation = 90;
		public float recoil = 0f;
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
