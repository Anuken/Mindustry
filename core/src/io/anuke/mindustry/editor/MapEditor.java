package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.ObjectMap;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.editor.DrawOperation.TileOperation;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.io.MapTileData;
import io.anuke.mindustry.io.MapTileData.DataPosition;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.util.Bits;
import io.anuke.ucore.util.Mathf;

public class MapEditor{
	public static final int minMapSize = 128, maxMapSize = 512;
	public static final int[] brushSizes = {1, 2, 3, 4, 5, 9, 15};
	
	private MapTileData map;
	private ObjectMap<String, String> tags = new ObjectMap<>();
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

		byte writeID = (byte)drawBlock.id;
		byte partID = (byte)Blocks.blockpart.id;
		byte rotationTeam = Bits.packByte(drawBlock.rotate ? (byte)rotation : 0, drawBlock.synthetic() ? (byte)drawTeam.ordinal() : 0);

		boolean isfloor = drawBlock instanceof Floor;

		if(drawBlock.isMultiblock()) {

			int offsetx = -(drawBlock.size - 1) / 2;
			int offsety = -(drawBlock.size - 1) / 2;

			for(int i = 0; i < 2; i ++){
				for (int dx = 0; dx < drawBlock.size; dx++) {
					for (int dy = 0; dy < drawBlock.size; dy++) {
						int worldx = dx + offsetx + x;
						int worldy = dy + offsety + y;

						if (Mathf.inBounds(worldx, worldy, map.width(), map.height())) {
							TileDataMarker prev = getPrev(worldx, worldy, false);

							if(i == 1) {
								map.write(worldx, worldy, DataPosition.wall, partID);
								map.write(worldx, worldy, DataPosition.rotationTeam, rotationTeam);
								map.write(worldx, worldy, DataPosition.link, Bits.packByte((byte) (dx + offsetx + 8), (byte) (dy + offsety + 8)));
							}else{
								byte link = map.read(worldx, worldy, DataPosition.link);
								byte block = map.read(worldx, worldy, DataPosition.wall);

								if (link != 0) {
									removeLinked(worldx - (Bits.getLeftByte(link) - 8), worldy - (Bits.getRightByte(link) - 8));
								}else if(Block.getByID(block).isMultiblock()){
									removeLinked(worldx, worldy);
								}
							}

							onWrite(worldx, worldy, prev);
						}
					}
				}
			}

			TileDataMarker prev = getPrev(x, y, false);

			map.write(x, y, DataPosition.wall, writeID);
			map.write(x, y, DataPosition.link, (byte)0);
			map.write(x, y, DataPosition.rotationTeam, rotationTeam);

			onWrite(x, y, prev);
		}else{

			for (int rx = -brushSize; rx <= brushSize; rx++) {
				for (int ry = -brushSize; ry <= brushSize; ry++) {
					if (Mathf.dst(rx, ry) <= brushSize - 0.5f) {
						int wx = x + rx, wy = y + ry;

						if (wx < 0 || wy < 0 || wx >= map.width() || wy >= map.height()) {
							continue;
						}

						TileDataMarker prev = getPrev(wx, wy, true);

						if(!isfloor) {
							byte link = map.read(wx, wy, DataPosition.link);

							if (link != 0) {
								removeLinked(wx - (Bits.getLeftByte(link) - 8), wy - (Bits.getRightByte(link) - 8));
							}
						}

						if(isfloor){
							map.write(wx, wy, DataPosition.floor, writeID);
						}else{
							map.write(wx, wy, DataPosition.wall, writeID);
							map.write(wx, wy, DataPosition.link, (byte)0);
							map.write(wx, wy, DataPosition.rotationTeam, rotationTeam);
						}

						onWrite(x + rx, y + ry, prev);
					}
				}
			}
		}
	}

	private void removeLinked(int x, int y){
		Block block = Block.getByID(map.read(x, y, DataPosition.wall));

		int offsetx = -(block.size-1)/2;
		int offsety = -(block.size-1)/2;
		for(int dx = 0; dx < block.size; dx ++){
			for(int dy = 0; dy < block.size; dy ++){
				int worldx = x + dx + offsetx, worldy = y + dy + offsety;
				if(Mathf.inBounds(worldx, worldy, map.width(), map.height())){
					TileDataMarker prev = getPrev(worldx, worldy, false);

					map.write(worldx, worldy, DataPosition.link, (byte)0);
					map.write(worldx, worldy, DataPosition.rotationTeam, (byte)0);
					map.write(worldx, worldy, DataPosition.wall, (byte)0);

					onWrite(worldx, worldy, prev);
				}
			}
		}
	}

	boolean checkDupes(int x, int y){
		return Vars.ui.editor.getView().checkForDuplicates((short) x, (short) y);
	}

	void onWrite(int x, int y, TileDataMarker previous){
		if(previous == null){
			renderer.updatePoint(x, y);
			return;
		}

		TileDataMarker current = map.new TileDataMarker();
		map.position(x, y);
		map.read(current);

		Vars.ui.editor.getView().addTileOp(new TileOperation((short) x, (short) y, previous, current));
		renderer.updatePoint(x, y);
	}

	TileDataMarker getPrev(int x, int y, boolean checkDupes){
		if(checkDupes && checkDupes(x, y)){
			return null;
		}else{
			TileDataMarker marker = map.newDataMarker();
			map.position(x, y);
			map.read(marker);
			return marker;
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
