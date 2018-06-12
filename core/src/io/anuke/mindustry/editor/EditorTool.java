package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.mindustry.content.blocks.Blocks;
import io.anuke.mindustry.io.MapTileData.DataPosition;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.Floor;
import io.anuke.ucore.util.Bits;

import static io.anuke.mindustry.Vars.ui;

public enum EditorTool{
	pick{
		public void touched(MapEditor editor, int x, int y){
			byte bf = editor.getMap().read(x, y, DataPosition.floor);
			byte bw = editor.getMap().read(x, y, DataPosition.wall);
			byte link = editor.getMap().read(x, y, DataPosition.link);

			if(link != 0){
				x -= (Bits.getLeftByte(link) - 8);
				y -= (Bits.getRightByte(link) - 8);
				bf = editor.getMap().read(x, y, DataPosition.floor);
				bw = editor.getMap().read(x, y, DataPosition.wall);
			}

			Block block = Block.getByID(bw == 0 ? bf : bw);
			editor.setDrawBlock(block);
			ui.editor.updateSelectedBlock();
		}
	},
	pencil{
		{
			edit = true;
			draggable = true;
		}

		@Override
		public void touched(MapEditor editor, int x, int y){
			editor.draw(x, y);
		}
	},
	eraser{
		{
			edit = true;
			draggable = true;
		}

		@Override
		public void touched(MapEditor editor, int x, int y){
			editor.draw(x, y, Blocks.air);
		}
	},
	elevation{
		{
			edit = true;
			draggable = true;
		}

		@Override
		public void touched(MapEditor editor, int x, int y){
			editor.elevate(x, y);
		}
	},
	line{
		{

		}
	},
	fill{
		{
			edit = true;
		}
		
		public void touched(MapEditor editor, int x, int y){
			if(editor.getDrawBlock().isMultiblock()){
				//don't fill multiblocks, thanks
				pencil.touched(editor, x, y);
				return;
			}

			boolean floor = editor.getDrawBlock() instanceof Floor;

			byte bf = editor.getMap().read(x, y, DataPosition.floor);
			byte bw = editor.getMap().read(x, y, DataPosition.wall);
			boolean synth = editor.getDrawBlock().synthetic();
			byte brt = Bits.packByte((byte)editor.getDrawRotation(), (byte)editor.getDrawTeam().ordinal());

			byte dest = floor ? bf: bw;
			byte draw = (byte)editor.getDrawBlock().id;

			int width = editor.getMap().width();
			int height = editor.getMap().height();

			IntSet set = new IntSet();
			IntArray points = new IntArray();
			points.add(asInt(x, y, editor.getMap().width()));

			while(points.size != 0){
				int pos = points.pop();
				int px = pos % width;
				int py = pos / width;
				set.add(pos);

				byte nbf = editor.getMap().read(px, py, DataPosition.floor);
				byte nbw = editor.getMap().read(px, py, DataPosition.wall);

				if((floor ? nbf : nbw) == dest){
					TileDataMarker prev = editor.getPrev(px, py, false);

					if(floor) {
						editor.getMap().write(px, py, DataPosition.floor, draw);
					}else {
						editor.getMap().write(px, py, DataPosition.wall, draw);
					}

					if(synth){
						editor.getMap().write(px, py, DataPosition.rotationTeam, brt);
					}

					if(px > 0 && !set.contains(asInt(px - 1, py, width))) points.add(asInt(px - 1, py, width));
					if(py > 0 && !set.contains(asInt(px, py - 1, width))) points.add(asInt(px, py - 1, width));
					if(px < width - 1 && !set.contains(asInt(px + 1, py, width))) points.add(asInt(px + 1, py, width));
					if(py < height - 1 && !set.contains(asInt(px, py + 1, width))) points.add(asInt(px, py + 1, width));

					editor.onWrite(px, py, prev);
				}
			}
		}
		
		int asInt(int x, int y, int width){
			return x+y*width;
		}
	},
	zoom;

	boolean edit, draggable;
	
	public void touched(MapEditor editor, int x, int y){
		
	}
}
