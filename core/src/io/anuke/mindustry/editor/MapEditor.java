package io.anuke.mindustry.editor;

import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.Floor;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Mathf;

public class MapEditor{
	public static final int minMapSize = 128, maxMapSize = 512;
	public static final int[] brushSizes = {1, 2, 3, 4, 5, 9, 15};
	
	private MapTileData map;
	private MapRenderer renderer = new MapRenderer(this);

	private int brushSize = 1;
	private int rotation;
	private Block drawBlock = Blocks.stone;
	private Team drawTeam = Team.none;
	
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

	public int getDrawRotation(){
		return rotation;
	}

	public void setDrawRotation(int rotation){
		this.rotation = rotation;
	}

	public void setDrawTeam(Team team){
		this.drawTeam = team;
	}

	public Team getDrawTeam() {
		return drawTeam;
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

	public void draw(int x, int y){
		if(x < 0 || y < 0 || x >= map.width() || y >= map.height()){
			return;
		}

		TileDataMarker writer = map.readAt(x, y);
		if(drawBlock instanceof Floor){
			writer.floor = (byte)drawBlock.id;
		}else{
			writer.wall = (byte)drawBlock.id;
		}

		if(drawBlock.hasEntity()){
			writer.team = (byte)drawTeam.ordinal();
		}

		if(drawBlock.rotate){
			writer.rotation = (byte)rotation;
		}

		if(drawBlock.isMultiblock()){

			int offsetx = -(drawBlock.size-1)/2;
			int offsety = -(drawBlock.size-1)/2;

			for(int dx = 0; dx < drawBlock.size; dx ++){
				for(int dy = 0; dy < drawBlock.size; dy ++){
					int worldx = dx + offsetx + x;
					int worldy = dy + offsety + y;
					if(!(worldx == x && worldy == y)){

						if(Mathf.inBounds(worldx, worldy, map.width(), map.height())){
							map.readAt(worldx, worldy);
							byte floor = writer.floor;

							if(writer.link != 0){
								removeLinked(worldx - (Bits.getLeftByte(writer.link) - 8), worldy - (Bits.getRightByte(writer.link) - 8));
							}

							writer.wall = (byte)Blocks.blockpart.id;
							writer.team = (byte)drawTeam.ordinal();
							writer.floor = floor;
							writer.link = Bits.packByte((byte) (dx + offsetx + 8), (byte) (dy + offsety + 8));
							map.write(worldx, worldy, writer);

							renderer.updatePoint(worldx, worldy);
						}
					}
				}
			}

			map.readAt(x, y);
			if(drawBlock instanceof Floor){
				writer.floor = (byte)drawBlock.id;
			}else{
				writer.wall = (byte)drawBlock.id;
			}

			writer.team = (byte)drawTeam.ordinal();
			writer.rotation = 0;
			writer.link = 0;
			map.write(x, y, writer);

			renderer.updatePoint(x, y);
		}else{

			for (int rx = -brushSize; rx <= brushSize; rx++) {
				for (int ry = -brushSize; ry <= brushSize; ry++) {
					if (Mathf.dst(rx, ry) <= brushSize - 0.5f) {
						if (x + rx < 0 || y + ry < 0 || x + rx >= map.width() || y + ry >= map.height()) {
							continue;
						}
						map.write(x + rx, y + ry, writer);
						renderer.updatePoint(x + rx, y + ry);
					}
				}
			}
		}
	}

	private void removeLinked(int x, int y){
		TileDataMarker marker = map.readAt(x, y);
		Block block = Block.getByID(marker.wall);

		int offsetx = -(block.size-1)/2;
		int offsety = -(block.size-1)/2;
		for(int dx = 0; dx < block.size; dx ++){
			for(int dy = 0; dy < block.size; dy ++){
				int worldx = x + dx + offsetx, worldy = y + dy + offsety;
				if(Mathf.inBounds(worldx, worldy, map.width(), map.height())){
					map.readAt(worldx, worldy);
					marker.link = 0;
					marker.wall = 0;
					marker.rotation = 0;
					marker.team = 0;
					map.write(worldx, worldy, marker);

					if(worldx == x && worldy == y){
						renderer.updatePoint(worldx, worldy);
					}
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
