package io.anuke.ucore.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.Disposable;

import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Graphics;

/**A framebuffer wrapper.*/
public class Surface implements Disposable{
	private FrameBuffer buffer;
	private int scale = 1;
	private boolean linear = false;
	private int bind = 0;
	private boolean fixedSize = false;
	private int fixedWidth, fixedHeight;
	
	public Surface(int scale, int bind){
		this.scale = scale;
		this.bind = bind;
		resize();
	}

	public Surface setFixedSize(int width, int height){
		fixedSize = true;
		this.fixedWidth = width;
		this.fixedHeight = height;
		resize();
		return this;
	}
	
	public void resize(){
		if(buffer != null){
			buffer.dispose();
			buffer = null;
		}

		if(!fixedSize) {
			int scale = this.scale == -1 ? Core.cameraScale : this.scale;

			buffer = new FrameBuffer(Format.RGBA8888, Gdx.graphics.getBackBufferWidth() / scale, Gdx.graphics.getBackBufferHeight() / scale, false);
		}else{
			buffer = new FrameBuffer(Format.RGBA8888, fixedWidth, fixedHeight, false);
		}

		if(!linear)
			buffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
	}
	
	public Texture texture(){
		return buffer.getColorBufferTexture();
	}
	
	public void setLinear(boolean linear){
		this.linear = linear;
	}
	
	public void setScale(int scale){
		this.scale = scale;
		resize();
	}
	
	public void begin(){
		begin(true);
	}
	
	public void begin(boolean clear){
		buffer.begin();
		if(bind != 0) buffer.getColorBufferTexture().bind(bind);
		
		if(clear)
			Graphics.clear(1f, 1f, 1f, 0f);
	}
	
	//TODO bind all textures to 0 as well, maybe?
	public void end(boolean render){
		buffer.end();
		if(bind != 0) buffer.getColorBufferTexture().bind(0);
	}
	
	@Override
	public void dispose(){
		buffer.dispose();
	}
}
