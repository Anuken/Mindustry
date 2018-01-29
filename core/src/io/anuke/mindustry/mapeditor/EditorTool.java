package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntSet;
import static io.anuke.mindustry.Vars.*;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.ColorMapper.BlockPair;

import java.util.Stack;

public enum EditorTool{
	pick{
		public void touched(MapEditor editor, int x, int y){
			BlockPair pair = ColorMapper.get(editor.pixmap().getPixel(x, y));
			if(pair == null) return;
			Block block = pair.dominant();
			editor.setDrawBlock(block);
			ui.editor.updateSelectedBlock();
		}
	},
	pencil{
		{
			edit = true;
		}
		
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
			Pixmap pix = editor.pixmap();
			
			int dest = pix.getPixel(x, y);

			int width = pix.getWidth();

			IntSet set = new IntSet();
			Stack<GridPoint2> points = new Stack<GridPoint2>();
			points.add(new GridPoint2(x, y));

			while( !points.isEmpty()){
				GridPoint2 pos = points.pop();
				set.add(asInt(pos.x, pos.y, width));

				int pcolor = pix.getPixel(pos.x, pos.y);
				if(colorEquals(pcolor, dest)){

					pix.drawPixel(pos.x, pos.y);

					if(pos.x > 0 && !set.contains(asInt(pos.x - 1, pos.y, width))) points.add(new GridPoint2(pos).cpy().add( -1, 0));
					if(pos.y > 0 && !set.contains(asInt(pos.x, pos.y - 1, width))) points.add(new GridPoint2(pos).cpy().add(0, -1));
					if(pos.x < pix.getWidth() - 1 && !set.contains(asInt(pos.x + 1, pos.y, width))) points.add(new GridPoint2(pos).cpy().add(1, 0));
					if(pos.y < pix.getHeight() - 1 && !set.contains(asInt(pos.x, pos.y + 1, width))) points.add(new GridPoint2(pos).cpy().add(0, 1));
				}
			}
			
			editor.updateTexture();
		}
		
		int asInt(int x, int y, int width){
			return x+y*width;
		}
		
		boolean colorEquals(int a, int b){
			return a == b;
		}
	},
	zoom;
	boolean edit;
	
	public void touched(MapEditor editor, int x, int y){
		
	}
}
