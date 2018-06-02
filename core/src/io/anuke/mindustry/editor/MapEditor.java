package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.editor.DrawOperation.TileOperation;
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
	private ObjectMap<String, String> tags = new ObjectMap<>();
	private TileDataMarker multiWriter;
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

	public ObjectMap<String, String> getTags() {
		return tags;
	}

	public void beginEdit(MapTileData map, ObjectMap<String, String> tags){
		this.map = map;
		this.brushSize = 1;
		this.tags = tags;

		drawBlock = Blocks.stone;
		multiWriter = map.new TileDataMarker();
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
		boolean isfloor = drawBlock instanceof Floor;
		if(isfloor){
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
							write(worldx, worldy, writer, false);

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
			write(x, y, writer, false);

			renderer.updatePoint(x, y);
		}else{

			for (int rx = -brushSize; rx <= brushSize; rx++) {
				for (int ry = -brushSize; ry <= brushSize; ry++) {
					if (Mathf.dst(rx, ry) <= brushSize - 0.5f) {
						if (x + rx < 0 || y + ry < 0 || x + rx >= map.width() || y + ry >= map.height()) {
							continue;
						}

						byte b1 = map.readAt(x + rx, y + ry, 1);
						byte b2 = map.readAt(x + rx, y + ry, 2);
						byte b3 = map.readAt(x + rx, y + ry, 3);

						if(!isfloor) {
							byte link = map.readAt(x + rx, y + ry, 2);

							if (link != 0) {
								removeLinked(x + rx - (Bits.getLeftByte(link) - 8), y + ry - (Bits.getRightByte(link) - 8));
							}
						}

						writer.wall = b1;
						writer.link = b2;
						writer.rotation = Bits.getLeftByte(b3);
						writer.team = Bits.getRightByte(b3);

						write(x + rx, y + ry, writer, true);
						renderer.updatePoint(x + rx, y + ry);
					}
				}
			}
		}
	}

	private void removeLinked(int x, int y){
		TileDataMarker marker = map.readAt(x, y, multiWriter);
		Block block = Block.getByID(marker.wall);

		int offsetx = -(block.size-1)/2;
		int offsety = -(block.size-1)/2;
		for(int dx = 0; dx < block.size; dx ++){
			for(int dy = 0; dy < block.size; dy ++){
				int worldx = x + dx + offsetx, worldy = y + dy + offsety;
				if(Mathf.inBounds(worldx, worldy, map.width(), map.height())){
					map.readAt(worldx, worldy, marker);
					marker.link = 0;
					marker.wall = 0;
					marker.rotation = 0;
					marker.team = 0;
					write(worldx, worldy, marker, false);

					if(worldx == x && worldy == y){
						renderer.updatePoint(worldx, worldy);
					}
				}
			}
		}
	}

	public void write(int x, int y, TileDataMarker writer, boolean checkDupes){
		boolean dupe = checkDupes && Vars.ui.editor.getView().checkForDuplicates((short) x, (short) y);
		TileDataMarker prev = null;

		if(!dupe) {
			prev = map.new TileDataMarker();
			map.position(x, y);
			map.read(prev);
		}

		map.write(x, y, writer);

		if(!dupe) {
			TileDataMarker current = map.new TileDataMarker();
			map.position(x, y);
			map.read(current);

			Vars.ui.editor.getView().addTileOp(new TileOperation((short) x, (short) y, prev, current));
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
