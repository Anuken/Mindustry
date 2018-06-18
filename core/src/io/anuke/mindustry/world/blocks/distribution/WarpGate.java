package io.anuke.mindustry.world.blocks.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.Annotations.Remote;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.content.fx.BlockFx;
import io.anuke.mindustry.entities.Player;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.gen.CallBlocks;
import io.anuke.mindustry.net.In;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Liquid;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.PowerBlock;
import io.anuke.ucore.core.Effects;
import io.anuke.ucore.core.Effects.Effect;
import io.anuke.ucore.core.Graphics;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.graphics.Fill;
import io.anuke.ucore.graphics.Hue;
import io.anuke.ucore.graphics.Lines;
import io.anuke.ucore.scene.ui.ButtonGroup;
import io.anuke.ucore.scene.ui.ImageButton;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Mathf;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import static io.anuke.mindustry.Vars.tilesize;

public class WarpGate extends PowerBlock{
	public static final Color[] colorArray = {Color.ROYAL, Color.ORANGE, Color.SCARLET, Color.LIME,
			Color.PURPLE, Color.GOLD, Color.PINK, Color.LIGHT_GRAY};
	public static final int colors = colorArray.length;

	protected int timerTeleport = timers++;

	private static ObjectSet<Tile>[] teleporters = new ObjectSet[colors];
	private static Color color = new Color();
	private static byte lastColor = 0;

	private Array<Tile> removal = new Array<>();
	private Array<Tile> returns = new Array<>();

	protected float warmupTime = 60f;
	//time between teleports
	protected float teleportMax = 400f;
	protected float teleportLiquidUse = 0.3f;
	protected float liquidUse = 0.1f;
	protected float powerUse = 0.3f;
	protected Liquid inputLiquid = Liquids.cryofluid;
	protected Effect activateEffect = BlockFx.teleportActivate;
	protected Effect teleportEffect = BlockFx.teleport;
	protected Effect teleportOutEffect = BlockFx.teleportOut;

	static{
		for(int i = 0; i < colors; i ++){
			teleporters[i] = new ObjectSet<>();
		}
	}
	
	public WarpGate(String name) {
		super(name);
		update = true;
		solid = true;
		health = 80;
		powerCapacity = 300f;
		size = 3;
		itemCapacity = 100;
		hasLiquids = true;
		hasItems = true;
		liquidCapacity = 100f;
		configurable = true;
	}

	@Override
	public void setStats(){
		super.setStats();
	}

	@Override
	public void placed(Tile tile){
		CallBlocks.setTeleporterColor(null, tile, lastColor);
	}
	
	@Override
	public void draw(Tile tile){
		super.draw(tile);

		TeleporterEntity entity = tile.entity();
		float time = entity.time;
		float rad = entity.activeScl;

		if(entity.liquidLackScl > 0.01f){
			Graphics.setAdditiveBlending();
			Draw.color(1f, 0.3f, 0.3f, 0.4f * entity.liquidLackScl);
			Fill.square(tile.drawx(), tile.drawy(), size * tilesize);
			Graphics.setNormalBlending();
		}

		Draw.color(getColor(tile, 0));
		Draw.rect(name+"-top", tile.drawx(), tile.drawy());
		Draw.reset();

		if(rad <= 0.0001f) return;

		Draw.color(getColor(tile, 0));

		Fill.circle(tile.drawx(), tile.drawy(), rad*(7f + Mathf.absin(time+55, 8f, 1f)));

		Draw.color(getColor(tile, -1));

		Fill.circle(tile.drawx(), tile.drawy(), rad*(2f + Mathf.absin(time, 7f, 3f)));

		for(int i = 0; i < 11; i ++){
			Lines.swirl(tile.drawx(), tile.drawy(),
					rad*(2f + i/3f + Mathf.sin(time - i *75, 20f + i, 3f)),
					0.3f + Mathf.sin(time + i *33, 10f + i, 0.1f),
					time * (1f + Mathf.randomSeedRange(i + 1, 1f)) + Mathf.randomSeedRange(i, 360f));
		}

		Draw.color(getColor(tile, 1));

		Lines.stroke(2f);
		Lines.circle(tile.drawx(), tile.drawy(), rad*(7f + Mathf.absin(time+55, 8f, 1f)));
		Lines.stroke(1f);

		for(int i = 0; i < 11; i ++){
			Lines.swirl(tile.drawx(), tile.drawy(),
					rad*(3f + i/3f + Mathf.sin(time + i *93, 20f + i, 3f)),
					0.2f + Mathf.sin(time + i *33, 10f + i, 0.1f),
					time * (1f + Mathf.randomSeedRange(i + 1, 1f)) + Mathf.randomSeedRange(i, 360f));
		}

		Draw.reset();
	}
	
	@Override
	public void update(Tile tile){
		TeleporterEntity entity = tile.entity();

		teleporters[entity.color].add(tile);

		if(entity.items.totalItems() > 0){
			tryDump(tile);
		}

		if(!entity.active){
			entity.activeScl = Mathf.lerpDelta(entity.activeScl, 0f, 0.01f);

			if(entity.power.amount >= powerCapacity){
				Color resultColor = new Color();
				resultColor.set(getColor(tile, 0));

				entity.active = true;
				entity.power.amount = 0f;
				Effects.effect(activateEffect, resultColor, tile.drawx(), tile.drawy());
			}
		}else {
			entity.activeScl = Mathf.lerpDelta(entity.activeScl, 1f, 0.015f);

			float powerUsed = Math.min(powerCapacity, powerUse * Timers.delta());

			if (entity.power.amount >= powerUsed) {
				entity.power.amount -= powerUsed;
				entity.powerLackScl = Mathf.lerpDelta(entity.powerLackScl, 0f, 0.1f);
			}else{
				entity.power.amount = 0f;
				entity.powerLackScl = Mathf.lerpDelta(entity.powerLackScl, 1f, 0.1f);
			}

			if(entity.powerLackScl >= 0.999f){
				catastrophicFailure(tile);
			}

			float liquidUsed = Math.min(liquidCapacity, liquidUse * Timers.delta());

			if (entity.liquids.amount >= liquidUsed) {
				entity.liquids.amount -= liquidUsed;
				entity.liquidLackScl = Mathf.lerpDelta(entity.liquidLackScl, 0f, 0.1f);
			}else{
				entity.liquids.amount = 0f;
				entity.liquidLackScl = Mathf.lerpDelta(entity.liquidLackScl, 1f, 0.1f);
			}

			if(entity.liquidLackScl >= 0.999f){
				catastrophicFailure(tile);
			}

			//TODO draw warning info!

			if (entity.teleporting) {
				entity.speedScl = Mathf.lerpDelta(entity.speedScl, 2f, 0.01f);
				liquidUsed = Math.min(liquidCapacity, teleportLiquidUse * Timers.delta());

				if (entity.liquids.amount >= liquidUsed) {
					entity.liquids.amount -= liquidUsed;
				} else {
					catastrophicFailure(tile);
				}
			} else {
				entity.speedScl = Mathf.lerpDelta(entity.speedScl, 1f, 0.04f);
			}

			entity.time += Timers.delta() * entity.speedScl;

			if (!entity.teleporting && entity.items.totalItems() >= itemCapacity && entity.power.amount >= powerCapacity - 0.01f - powerUse &&
					entity.timer.get(timerTeleport, teleportMax)) {
				Array<Tile> testLinks = findLinks(tile);

				if (testLinks.size == 0) return;

				Color resultColor = new Color();
				resultColor.set(getColor(tile, 0));

				entity.teleporting = true;

				Effects.effect(teleportEffect, resultColor, tile.drawx(), tile.drawy());
				Timers.run(warmupTime, () -> {
					Array<Tile> links = findLinks(tile);

					for (Tile other : links) {
						int canAccept = itemCapacity - other.entity.items.totalItems();
						int total = entity.items.totalItems();
						if (total == 0) break;
						Effects.effect(teleportOutEffect, resultColor, other.drawx(), other.drawy());
						for (int i = 0; i < canAccept && i < total; i++) {
							other.entity.items.addItem(entity.items.takeItem(), 1);
						}
					}
					Effects.effect(teleportOutEffect, resultColor, tile.drawx(), tile.drawy());
					entity.power.amount = 0f;
					entity.teleporting = false;
				});
			}
		}
	}
	
	@Override
	public void buildTable(Tile tile, Table table){
		TeleporterEntity entity = tile.entity();

		//TODO call event for change

		ButtonGroup<ImageButton> group = new ButtonGroup<>();
		Table cont = new Table();

		for(int i = 0; i < colors; i ++){
			final int f = i;
			ImageButton button = cont.addImageButton("white", "toggle", 24, () -> {
				lastColor = (byte)f;
				CallBlocks.setTeleporterColor(null, tile, (byte)f);
			}).size(34, 38).padBottom(-5.1f).group(group).get();
			button.getStyle().imageUpColor = colorArray[f];
			button.setChecked(entity.color == f);

			if(i%4 == 3){
				cont.row();
			}
		}

		table.add(cont);
	}
	
	@Override
	public boolean acceptItem(Item item, Tile tile, Tile source){
		TeleporterEntity entity = tile.entity();
		return entity.items.totalItems() < itemCapacity;
	}
	
	@Override
	public TileEntity getEntity(){
		return new TeleporterEntity();
	}

	@Override
	public boolean acceptLiquid(Tile tile, Tile source, Liquid liquid, float amount) {
		return super.acceptLiquid(tile, source, liquid, amount) && liquid == inputLiquid;
	}

	@Override
	public void onDestroyed(Tile tile) {
		super.onDestroyed(tile);

		TeleporterEntity entity = tile.entity();

		if(entity.activeScl < 0.5f) return;

		//TODO catastrophic failure
	}

	private void catastrophicFailure(Tile tile){
		tile.entity.damage(tile.entity.health + 1);
		//TODO fail gloriously
	}

	private Color getColor(Tile tile, int shift){
		TeleporterEntity entity = tile.entity();

		Color target = colorArray[entity.color];
		float ss = 0.5f;
		float bs = 0.2f;

		return Hue.shift(Hue.multiply(color.set(target), 1, ss), 2, shift * bs + (entity.speedScl - 1f)/3f);
	}
	
	private Array<Tile> findLinks(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		removal.clear();
		returns.clear();
		
		for(Tile other : teleporters[entity.color]){
			if(other != tile){
				if(other.block() instanceof WarpGate){
					TeleporterEntity oe = other.entity();
					if(!oe.active) continue;
					if(oe.color != entity.color){
						removal.add(other);
					}else if(other.entity.items.totalItems() == 0){
						returns.add(other);
					}
				}else{
					removal.add(other);
				}
			}
		}

		for(Tile remove : removal) {
			teleporters[entity.color].remove(remove);
		}
		
		return returns;
	}

	@Remote(targets = Loc.both, called = Loc.both, in = In.blocks, forward = true)
	public static void setTeleporterColor(Player player, Tile tile, byte color){
		TeleporterEntity entity = tile.entity();
		entity.color = color;
	}

	public static class TeleporterEntity extends TileEntity{
		public byte color = 0;
		public boolean teleporting;
		public boolean active;
		public float activeScl = 0f;
		public float speedScl = 1f;
		public float powerLackScl, liquidLackScl;
		public float time;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(color);
			stream.writeBoolean(active);
			stream.writeFloat(activeScl);
			stream.writeFloat(speedScl);
			stream.writeFloat(powerLackScl);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			color = stream.readByte();
			active = stream.readBoolean();
			activeScl = stream.readFloat();
			speedScl = stream.readFloat();
			powerLackScl = stream.readFloat();
		}
	}

}
