package io.anuke.mindustry.world.blocks.types.distribution;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import io.anuke.mindustry.content.fx.Fx;
import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.PowerBlock;
import io.anuke.ucore.core.Effects.Effect;
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

public class Teleporter extends PowerBlock{
	public static final Color[] colorArray = {Color.ROYAL, Color.ORANGE, Color.SCARLET, Color.LIME,
			Color.PURPLE, Color.GOLD, Color.PINK, Color.LIGHT_GRAY};
	public static final int colors = colorArray.length;

	private static ObjectSet<Tile>[] teleporters = new ObjectSet[colors];
	private static Color color = new Color();
	private static byte lastColor = 0;

	private Array<Tile> removal = new Array<>();
	private Array<Tile> returns = new Array<>();

	protected float warmupTime = 80f;
	protected Effect teleportEffect = Fx.none;

	static{
		for(int i = 0; i < colors; i ++){
			teleporters[i] = new ObjectSet<>();
		}
	}
	
	public Teleporter(String name) {
		super(name);
		update = true;
		solid = true;
		health = 80;
		powerCapacity = 30f;
		size = 3;
		itemCapacity = 150;
	}

	@Override
	public void configure(Tile tile, byte data) {
		TeleporterEntity entity = tile.entity();
		if(entity != null){
			entity.color = data;
			entity.inventory.clear();
		}
	}

	@Override
	public void setStats(){
		super.setStats();
	}

	@Override
	public void placed(Tile tile){
		tile.<TeleporterEntity>entity().color = lastColor;
		setConfigure(tile, lastColor);
	}
	
	@Override
	public void draw(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		super.draw(tile);

		Color target = colorArray[entity.color];
		float ss = 0.5f;
		float bs = 0.2f;

		Draw.color(Hue.shift(Hue.multiply(color.set(target), 1, ss), 2, 0));
		Draw.rect("teleporter-top", tile.drawx(), tile.drawy());

		//Draw.color(Palette.portal);
		Draw.color(Hue.shift(Hue.multiply(color.set(target), 1, ss), 2, 0));

		Fill.circle(tile.drawx(), tile.drawy(), 7f + Mathf.absin(Timers.time()+55, 8f, 1f));

        //Draw.color(Palette.portalDark);
		Draw.color(Hue.shift(Hue.multiply(color.set(target), 1, ss), 2, -bs));

		Fill.circle(tile.drawx(), tile.drawy(), 2f + Mathf.absin(Timers.time(), 7f, 3f));

		for(int i = 0; i < 11; i ++){
			Lines.swirl(tile.drawx(), tile.drawy(),
					2f + i/3f + Mathf.sin(Timers.time() - i *75, 20f + i, 3f),
					0.3f + Mathf.sin(Timers.time() + i *33, 10f + i, 0.1f),
					Timers.time() * (1f + Mathf.randomSeedRange(i + 1, 1f)) + Mathf.randomSeedRange(i, 360f));
		}

        //Draw.color(Palette.portalLight);
		Draw.color(Hue.shift(Hue.multiply(color.set(target), 1, ss), 2, bs));

		Lines.stroke(2f);
		Lines.circle(tile.drawx(), tile.drawy(), 7f + Mathf.absin(Timers.time()+55, 8f, 1f));
		Lines.stroke(1f);

		for(int i = 0; i < 11; i ++){
			Lines.swirl(tile.drawx(), tile.drawy(),
					3f + i/3f + Mathf.sin(Timers.time() + i *93, 20f + i, 3f),
					0.2f + Mathf.sin(Timers.time() + i *33, 10f + i, 0.1f),
					Timers.time() * (1f + Mathf.randomSeedRange(i + 1, 1f)) + Mathf.randomSeedRange(i, 360f));
		}

		Draw.reset();
	}
	
	@Override
	public void update(Tile tile){
		TeleporterEntity entity = tile.entity();

		teleporters[entity.color].add(tile);

		if(entity.inventory.totalItems() > 0){
			tryDump(tile);
		}

		if(entity.inventory.totalItems() == itemCapacity && entity.power.amount >= powerCapacity){

		}
	}

	@Override
	public boolean isConfigurable(Tile tile){
		return true;
	}
	
	@Override
	public void buildTable(Tile tile, Table table){
		TeleporterEntity entity = tile.entity();

		ButtonGroup<ImageButton> group = new ButtonGroup<>();
		Table cont = new Table();
		cont.margin(4);
		cont.marginBottom(5);

		cont.add().colspan(4).height(145f);
		cont.row();

		for(int i = 0; i < colors; i ++){
			final int f = i;
			ImageButton button = cont.addImageButton("white", "toggle", 24, () -> {
				lastColor = (byte)f;
				setConfigure(tile, (byte)f);
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
		return entity.inventory.totalItems() < itemCapacity;
	}
	
	@Override
	public TileEntity getEntity(){
		return new TeleporterEntity();
	}
	
	private Array<Tile> findLinks(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		removal.clear();
		returns.clear();
		
		for(Tile other : teleporters[entity.color]){
			if(other != tile){
				if(other.block() instanceof Teleporter){
					if(other.<TeleporterEntity>entity().color != entity.color){
						removal.add(other);
					}else if(other.entity.inventory.totalItems() == 0){
						returns.add(other);
					}
				}else{
					removal.add(other);
				}
			}
		}

		for(Tile remove : removal)
			teleporters[entity.color].remove(remove);
		
		return returns;
	}

	public static class TeleporterEntity extends TileEntity{
		public byte color = 0;
		
		@Override
		public void write(DataOutputStream stream) throws IOException{
			stream.writeByte(color);
		}
		
		@Override
		public void read(DataInputStream stream) throws IOException{
			color = stream.readByte();
		}
	}

}
