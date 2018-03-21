package io.anuke.mindustry.editor;

import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.util.Mathf;

public class MapEditor{
	public static final int minMapSize = 128, maxMapSize = 512;
	public static final int[] brushSizes = {1, 2, 3, 4, 5, 9, 15};
	
	private MapTileData map;
	private MapRenderer renderer = new MapRenderer(this);

	private int brushSize = 1;
	private Block drawBlock = Blocks.stone;
	
	public MapEditor(){

	}
	
	public MapTileData getMap(){
		return map;
	}
	
	public void beginEdit(MapTileData map){
		drawBlock = Blocks.stone;
		this.map = map;
		this.brushSize = 1;
		renderer.resize(map.width(), map.height());
	}
	
	public Block getDrawBlock(){
		return drawBlock;
	}
	
	public void setDrawBlock(Block block){
		this.drawBlock = block;
	}
	
	public void setBrushSize(int size){
		this.brushSize = size;
	}

	public int getBrushSize() {
		return brushSize;
	}

	public void draw(int dx, int dy){
		if(dx < 0 || dy < 0 || dx >= map.width() || dy >= map.height()){
			return;
		}

		TileDataMarker writer = map.readAt(dx, dy);
		if(drawBlock instanceof Floor){
			writer.floor = (byte)drawBlock.id;
		}else{
			writer.wall = (byte)drawBlock.id;
		}

		for(int rx = -brushSize + 1; rx <= brushSize - 1; rx ++){
			for(int ry = -brushSize + 1; ry <= brushSize - 1; ry ++){
				if(Mathf.dst(rx, ry) <= brushSize){
					if(dx + rx < 0 || dy + ry < 0 || dx + rx >= map.width() || dy + ry >= map.height()){
						continue;
					}
					map.write(dx + rx, dy + ry, writer);
					renderer.updatePoint(dx + rx, dy + ry);
				}
			}
		}

	}

	public MapRenderer renderer() {
		return renderer;
	}

	public void resize(int width, int height){
		map = new MapTileData(width, height);
		renderer.resize(width, height);
	}
}
