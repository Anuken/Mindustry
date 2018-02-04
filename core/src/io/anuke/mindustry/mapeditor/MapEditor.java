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
	public static final int[] validMapSizes = {128, 256, 512};
	public static final int[] brushSizes = {1, 2, 3, 4, 5, 9, 15};
	public static final int maxSpawnpoints = 15;
	public static final Format format = Format.RGBA8888;
	
	private Pixmap[] brushPixmaps = new Pixmap[brushSizes.length];
	
	private Map map;
	
	private MapFilter filter = new MapFilter();
	private Pixmap filterPixmap;
	private Texture filterTexture;
	
	private Pixmap pixmap;
	private Texture texture;
	private int brushSize = 1;
	private Block drawBlock = Blocks.stone;
	
	public MapEditor(){
		for(int i = 0; i < brushSizes.length; i ++){
			int s = brushSizes[i];
			brushPixmaps[i] = new Pixmap(s*2-1, s*2-1, format);
		}
	}
	
	public Map getMap(){
		return map;
	}
	
	public void updateTexture(){
		texture.draw(pixmap, 0, 0);
	}
	
	public MapFilter getFilter(){
		return filter;
	}
	
	public void applyFilterPreview(){
		if(filterPixmap != null && (filterPixmap.getWidth() != pixmap.getWidth() 
				|| filterPixmap.getHeight() != pixmap.getHeight())){
			filterPixmap.dispose();
			filterTexture.dispose();
			filterPixmap = null;
			filterTexture = null;
		}
		
		if(filterPixmap == null){
			filterPixmap = Pixmaps.copy(pixmap);

			filter.process(filterPixmap);
			filterTexture = new Texture(filterPixmap);
		}else{
			filterPixmap.drawPixmap(pixmap, 0, 0);
			filter.process(filterPixmap);
			filterTexture.draw(filterPixmap, 0, 0);
		}
		
	}
	
	public Texture getFilterTexture(){
		return filterTexture;
	}
	
	public void applyFilter(){
		filter.process(pixmap);
		texture.draw(pixmap, 0, 0);
	}
	
	public void beginEdit(Map map){
		drawBlock = Blocks.stone;
		this.map = map;
		this.brushSize = 1;
		if(map.pixmap == null){
			pixmap = new Pixmap(256, 256, format);
			pixmap.setColor(ColorMapper.getColor(drawBlock));
			pixmap.fill();
			texture = new Texture(pixmap);
		}else{
			pixmap = map.pixmap;
			texture = map.texture;
			pixmap.setColor(ColorMapper.getColor(drawBlock));
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

	public int getBrushSize() {
		return brushSize;
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
			x = width - dstWidth;
		}else if (x < 0){
			x = 0;
		}
		
		if (y + dstHeight > height){
			dstHeight = height - y;
		}else if (y < 0){
			dstHeight += y;
			y = 0;
		}
		
		pixmap.fillCircle(dx, dy, brushSize-1);
		
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
	
	public Pixmap pixmap(){
		return pixmap;
	}
	
	public void setPixmap(Pixmap out){
		if(pixmap.getWidth() == out.getWidth() && pixmap.getHeight() == out.getHeight()){
			pixmap.dispose();
			pixmap = out;
			texture.draw(out, 0, 0);
		}else{
			pixmap.dispose();
			texture.dispose();
			pixmap = out;
			texture = new Texture(out);
		}
		pixmap.setColor(ColorMapper.getColor(drawBlock));
		map.pixmap = pixmap;
		map.texture = texture;
	}
	
	public void resize(int width, int height){
		Pixmap out = Pixmaps.resize(pixmap, width, height, ColorMapper.getColor(Blocks.stone));
		pixmap.dispose();
		pixmap = out;
		texture.dispose();
		texture = new Texture(out);
		
		if(filterPixmap != null){
			filterPixmap.dispose();
			filterTexture.dispose();
			filterPixmap = null;
			filterTexture = null;
		}
		pixmap.setColor(ColorMapper.getColor(drawBlock));
	}
}
