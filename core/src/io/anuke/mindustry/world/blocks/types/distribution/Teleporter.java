package io.anuke.mindustry.world.blocks.types.distribution;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;

import io.anuke.mindustry.entities.TileEntity;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.blocks.types.Configurable;
import io.anuke.ucore.core.Draw;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Mathf;

//TODO
public class Teleporter extends Block implements Configurable{
	public static final int colors = 4;
	public static final Color[] colorArray = {Color.ROYAL, Color.ORANGE, Color.SCARLET, Color.FOREST, Color.PURPLE, Color.GOLD, Color.PINK};
	
	private static Array<Tile> removal = new Array<>();
	private static Array<Tile> returns = new Array<>();
	private static ObjectSet<Tile>[] teleporters = new ObjectSet[colors];
	
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
	}
	
	@Override
	public void draw(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		super.draw(tile);
		
		Draw.color(colorArray[entity.color]);
		Draw.rect("blank", tile.worldx(), tile.worldy(), 2, 2);
		Draw.color(Color.WHITE);
		Draw.alpha(0.45f + Mathf.absin(Timers.time(), 7f, 0.26f));
		Draw.rect("teleporter-top", tile.worldx(), tile.worldy());
		Draw.reset();
	}
	
	@Override
	public void update(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		teleporters[entity.color].add(tile);
		
		if(entity.totalItems() > 0){
			tryDump(tile);
		}
	}
	
	@Override
	public void buildTable(Tile tile, Table table){
		TeleporterEntity entity = tile.entity();
		
		table.addIButton("icon-arrow-left", Unit.dp.inPixels(10*3), ()->{
			entity.color --;
			if(entity.color < 0)
				entity.color += 4;
		});
		
		table.add().size(40f).units(Unit.dp);
		
		table.addIButton("icon-arrow-right", Unit.dp.inPixels(10*3), ()->{
			entity.color ++;
			entity.color %= 4;
		});
	}
	
	@Override
	public void handleItem(Item item, Tile tile, Tile source){
		Array<Tile> links = findLinks(tile);
		
		if(links.size > 0){
			Tile target = links.get(Mathf.random(0, links.size-1));
			target.entity.addItem(item, 1);
		}
	}
	
	@Override
	public boolean acceptItem(Item item, Tile dest, Tile source){
		Array<Tile> links = findLinks(dest);
		return links.size > 0;
	}
	
	@Override
	public TileEntity getEntity(){
		return new TeleporterEntity();
	}
	
	static Array<Tile> findLinks(Tile tile){
		TeleporterEntity entity = tile.entity();
		
		removal.clear();
		returns.clear();
		
		for(Tile other : teleporters[entity.color]){
			if(other != tile){
				if(other.block() instanceof Teleporter){
					if(other.<TeleporterEntity>entity().color != entity.color){
						removal.add(other);
					}else if(other.entity.totalItems() == 0){
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
