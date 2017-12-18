package io.anuke.mindustry.mapeditor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;

import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.ColorMapper;
import io.anuke.mindustry.world.Map;
import io.anuke.mindustry.world.blocks.Blocks;
import io.anuke.ucore.graphics.Pixmaps;

public class MapEditor{
	public static final int[] validMapSizes = {64, 128, 256};
	public static final int[] brushSizes = {1, 2, 3, 4, 5, 9, 15};
	
	private Pixmap[] brushPixmaps = new Pixmap[brushSizes.length];
	
	private Map map;
	private Pixmap pixmap;
	private Texture texture;
	private int brushSize = 1;
	private Block drawBlock = Blocks.stone;
	
	public MapEditor(){
		for(int i = 0; i < brushSizes.length; i ++){
			int s = brushSizes[i];
			brushPixmaps[i] = new Pixmap(s*2-1, s*2-1, Format.RGB888);
		}
	}
	
	public void beginEdit(Map map){
		this.map = map;
		this.brushSize = 1;
		if(map.pixmap == null){
			pixmap = new Pixmap(256, 256, Format.RGB888);
			pixmap.setColor(ColorMapper.getColor(Blocks.stone));
			pixmap.fill();
			texture = new Texture(pixmap);
		}else{
			pixmap = map.pixmap;
			pixmap.setColor(ColorMapper.getColor(Blocks.stone));
			texture = map.texture;
		}
		
	}
	
	public Block getDrawBlock(){
		return drawBlock;
	}
	
	public void setDrawBlock(Block block){
		this.drawBlock = block;
		pixmap.setColor(ColorMapper.getColor(block));
	}
	
	public void setBrushSize(int size){
		this.brushSize = size;
	}
	
	public void draw(int dx, int dy){
		if(dx < 0 || dy < 0 || dx >= pixmap.getWidth() || dy >= pixmap.getHeight()){
			return;
		}
		
		Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, texture.getTextureObjectHandle());
		
		int dstWidth = brushSize*2-1;
		int dstHeight = brushSize*2-1;
		int width = pixmap.getWidth(), height = pixmap.getHeight();
		
		int x = dx - dstWidth/2;
		int y = dy - dstHeight/2;
		
		if (x + dstWidth > width){
			//x = width - dstWidth;
			dstWidth = width - x;
		}else if (x < 0){
			dstWidth += x;
			x = 0;
		}
		
		//System.out.println(x + " " + y + " " + dstWidth + " " + dstHeight);

		if (y + dstHeight > height){
			//y = height - dstHeight;
			dstHeight = height - y;
		}else if (y < 0){
			dstHeight += y;
			y = 0;
		}
		
		pixmap.fillCircle(dx, dy, brushSize);
		
		Pixmap dst = brush(brushSize);
		dst.drawPixmap(pixmap, x, y, dstWidth, dstHeight, 0, 0, dstWidth, dstHeight);
		
		Gdx.gl.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, x, y, dstWidth, dstHeight,
				dst.getGLFormat(), dst.getGLType(), dst.getPixels());
	}
	
	private Pixmap brush(int size){
		for(int i = 0; i < brushSizes.length; i ++){
			if(brushSizes[i] == size){
				return brushPixmaps[i];
			}
		}
		return null;
	}
	
	public Texture texture(){
		return texture;
	}
	
	public void resize(int mapSize){
		Pixmap out = Pixmaps.resize(pixmap, mapSize, mapSize);
		pixmap.dispose();
		pixmap = out;
		texture.draw(pixmap, 0, 0);
	}
}
