package io.anuke.mindustry.editor;

import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.IntSet;
import io.anuke.mindustry.io.MapTileData.TileDataMarker;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.Floor;

import static io.anuke.mindustry.Vars.ui;

public enum EditorTool{
	pick{
		public void touched(MapEditor editor, int x, int y){
			TileDataMarker writer = editor.getMap().readAt(x, y);
			Block block = Block.getByID(writer.wall == 0 ? writer.floor : writer.wall);
			editor.setDrawBlock(block);
			ui.editor.updateSelectedBlock();
		}
	},
	pencil{
		{
			edit = true;
		}

		@Override
		public void touched(MapEditor editor, int x, int y){
			editor.draw(x, y);
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
			boolean floor = editor.getDrawBlock() instanceof Floor;

			TileDataMarker writer = editor.getMap().readAt(x, y);

			byte dest = floor ? writer.floor : writer.wall;

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

				writer = editor.getMap().readAt(px, py);

				if((floor ? writer.floor : writer.wall) == dest){
					if(floor)
						writer.floor = (byte)editor.getDrawBlock().id;
					else
						writer.wall = (byte)editor.getDrawBlock().id;

					editor.write(px, py, writer, false);
					editor.renderer().updatePoint(px, py);

					if(px > 0 && !set.contains(asInt(px - 1, py, width))) points.add(asInt(px - 1, py, width));
					if(py > 0 && !set.contains(asInt(px, py - 1, width))) points.add(asInt(px, py - 1, width));
					if(px < width - 1 && !set.contains(asInt(px + 1, py, width))) points.add(asInt(px + 1, py, width));
					if(py < height - 1 && !set.contains(asInt(px, py + 1, width))) points.add(asInt(px, py + 1, width));
				}
			}
		}
		
		int asInt(int x, int y, int width){
			return x+y*width;
		}
	},
	zoom;
	boolean edit;
	
	public void touched(MapEditor editor, int x, int y){
		
	}
}
