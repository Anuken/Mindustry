package io.anuke.mindustry.world.blocks.types.power;

import com.badlogic.gdx.graphics.Color;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.entities.effect.DamageArea;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.content.fx.ExplosionFx;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.world.BarType;
import io.anuke.mindustry.world.BlockBar;
import io.anuke.mindustry.world.Tile;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.util.Mathf;
import io.anuke.ucore.util.Strings;
import io.anuke.ucore.util.Translator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;

public class NuclearReactor extends LiquidBurnerGenerator {
	protected final int timerFuel = timers++;

	protected final Translator tr = new Translator();

	protected Item generateItem;
	protected Color coolColor = new Color(1, 1, 1, 0f);
	protected Color hotColor = Color.valueOf("ff9575a3");
	protected int fuelUseTime = 130; //time to consume 1 fuel
	protected float powerMultiplier = 0.45f; //power per frame, depends on full capacity
	protected float heating = 0.007f; //heating per frame
	protected float coolantPower = 0.015f; //how much heat decreases per coolant unit
	protected float smokeThreshold = 0.3f; //threshold at which block starts smoking
	protected int explosionRadius = 19;
	protected int explosionDamage = 135;
	protected float flashThreshold = 0.46f; //heat threshold at which the lights start flashing

	public NuclearReactor(String name) {
		super(name);
		generateItem = Items.thorium;
		itemCapacity = 30;
		liquidCapacity = 50;
		powerCapacity = 80f;
	}

	@Override
	public void setBars(){
		super.setBars();
		bars.replace(new BlockBar(BarType.inventory, true, tile -> (float)tile.entity.items.getItem(generateItem) / itemCapacity));
		bars.add(new BlockBar(BarType.heat, true, tile -> tile.<NuclearReactorEntity>entity().heat));
	}

	@Override
	public void setStats(){
		super.setStats();
		stats.add("inputitem", generateItem);
		stats.add("maxpowergenerationsecond", Strings.toFixed(powerMultiplier*60f, 2));

		stats.remove("powerliquid");
		stats.remove("maxliquidsecond");
	}
	
	@Override
	public void update(Tile tile){
		NuclearReactorEntity entity = tile.entity();
		
		int fuel = entity.items.getItem(generateItem);
		float fullness = (float)fuel / itemCapacity;
		
		if(fuel > 0){
			entity.heat += fullness * heating * Math.min(Timers.delta(), 4f);
			entity.power.amount += powerMultiplier * fullness * Timers.delta();
			entity.power.amount = Mathf.clamp(entity.power.amount, 0f, powerCapacity);
			if(entity.timer.get(timerFuel, fuelUseTime)){
				entity.items.removeItem(generateItem, 1);
			}
		}
		
		if(entity.liquids.amount > 0){
			//TODO steam when cooling large amounts?
			float liquidPower = 1f;

			if(liquidPower > 0){ //is coolant
				float pow = coolantPower * entity.liquids.liquid.heatCapacity;
				float maxCool = Math.min(entity.liquids.amount, entity.heat / pow); //max that can be cooled in terms of liquid
				entity.heat -= maxCool * pow;
				entity.liquids.amount -= maxCool;
			}else{ //is heater
				//TODO
			}
		}
		
		if(entity.heat > smokeThreshold){
			float smoke = 1.0f + (entity.heat - smokeThreshold) / (1f - smokeThreshold); //ranges from 1.0 to 2.0
			if(Mathf.chance(smoke / 20.0 * Timers.delta())){
				Effects.effect(BlockFx.reactorsmoke, tile.worldx() + Mathf.range(size * tilesize / 2f),
						tile.worldy() + Mathf.random(size * tilesize / 2f));
			}
		}

		entity.heat = Mathf.clamp(entity.heat);
		
		if(entity.heat >= 1f){
			entity.damage((int)entity.health);
		}else{
			distributePower(tile);
		}
	}
	
	@Override
	public void drawLiquidCenter(Tile tile){
		Draw.rect(name + "-center", tile.drawx(), tile.drawy());
	}
	
	@Override
	public void onDestroyed(Tile tile){
		super.onDestroyed(tile);
		
		NuclearReactorEntity entity = tile.entity();
		
		int fuel = entity.items.getItem(generateItem);
		
		if(fuel < 5 && entity.heat < 0.5f) return;
		
		Effects.shake(6f, 16f, tile.worldx(), tile.worldy());
		Effects.effect(ExplosionFx.nuclearShockwave, tile.worldx(), tile.worldy());
		for(int i = 0; i < 6; i ++){
			Timers.run(Mathf.random(40), () -> {
				Effects.effect(BlockFx.nuclearcloud, tile.worldx(), tile.worldy());
			});
		}
		
		DamageArea.damage(tile.worldx(), tile.worldy(), explosionRadius * tilesize, explosionDamage * 4);
		
		
		for(int i = 0; i < 20; i ++){
			Timers.run(Mathf.random(50), ()->{
				tr.rnd(Mathf.random(40f));
				Effects.effect(ExplosionFx.explosion, tr.x + tile.worldx(), tr.y + tile.worldy());
			});
		}
		
		for(int i = 0; i < 70; i ++){
			Timers.run(Mathf.random(80), ()->{
				tr.rnd(Mathf.random(120f));
				Effects.effect(BlockFx.nuclearsmoke, tr.x + tile.worldx(), tr.y + tile.worldy());
			});
		}
	}

	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		return item == generateItem && tile.entity.items.getItem(generateItem) < itemCapacity;
	}

	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount){
		return tile.entity.liquids.amount + amount < liquidCapacity
				&& (tile.entity.liquids.liquid == liquid || tile.entity.liquids.amount <= 0.001f);
	}

	@Override
	public void draw(Tile tile){
		super.draw(tile);
		
		NuclearReactorEntity entity = tile.entity();
		
		Draw.color(coolColor, hotColor, entity.heat);
		Draw.rect("white", tile.drawx(), tile.drawy(), size * tilesize, size * tilesize);
		
		if(entity.heat > flashThreshold){
			float flash = 1f + ((entity.heat - flashThreshold) / (1f - flashThreshold)) * 5.4f;
			entity.flash += flash * Timers.delta();
			Draw.color(Color.RED, Color.YELLOW, Mathf.absin(entity.flash, 9f, 1f));
			Draw.alpha(0.6f);
			Draw.rect(name + "-lights", tile.drawx(), tile.drawy());
		}
		
		Draw.reset();
	}
	
	@Override
	public TileEntity getEntity(){
		return new NuclearReactorEntity();
	}
	
	public static class NuclearReactorEntity extends GeneratorEntity {
		public float heat;
		public float flash;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			super.write(stream);
			stream.writeFloat(heat);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			super.read(stream);
			heat = stream.readFloat();
		}
	}
}
