package io.anuke.mindustry.mapeditor;

import java.util.Stack;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.IntSet;

import io.anuke.mindustry.Vars;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;

public enum EditorTool{
	pencil{
		public void touched(MapEditor editor, int x, int y){
			editor.draw(x, y);
		}
	},
	fill{
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
	pick{
		public void touched(MapEditor editor, int x, int y){
			Block block = ColorMapper.get(editor.pixmap().getPixel(x, y)).dominant();
			editor.setDrawBlock(block);
			Vars.ui.getEditor().updateSelectedBlock();
		}
	},
	zoom;
	
	public void touched(MapEditor editor, int x, int y){
		
	}
}
