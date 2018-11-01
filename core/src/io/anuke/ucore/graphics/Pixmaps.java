package io.anuke.ucore.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import io.anuke.ucore.UCore;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**Various pixmap utilities.*/
public class Pixmaps{
	private static ByteBuffer bytes = ByteBuffer.allocateDirect(4);

	public static void flip(Pixmap pixmap){
		Buffer pixels = pixmap.getPixels();
		int numBytes = pixmap.getWidth() * pixmap.getHeight() * 4;
		byte[] lines = new byte[numBytes];
		int numBytesPerLine = pixmap.getWidth() * 4;
		for(int i = 0; i < pixmap.getHeight(); i++){
			pixels.position((pixmap.getHeight() - i - 1) * numBytesPerLine);
			((ByteBuffer)pixels).get(lines, i * numBytesPerLine, numBytesPerLine);
		}
		pixels.clear();
		((ByteBuffer)pixels).put(lines);
	}

	public static Pixmap copy(Pixmap input){
		Pixmap pixmap = new Pixmap(input.getWidth(), input.getHeight(), Format.RGBA8888);
		pixmap.drawPixmap(input, 0, 0);
		return pixmap;
	}

	public static Pixmap scale(Pixmap input, float scale){
		return scale(input, scale, scale);
	}

	public static Pixmap scale(Pixmap input, float scalex, float scaley){
		Pixmap pixmap = new Pixmap((int) (input.getWidth() * scalex), (int) (input.getHeight() * scaley), Format.RGBA8888);
		for(int x = 0; x < pixmap.getWidth(); x++){
			for(int y = 0; y < pixmap.getHeight(); y++){
				pixmap.drawPixel(x, y, input.getPixel((int) (x / scalex), (int) (y / scaley)));
			}
		}
		return pixmap;
	}

	public static Pixmap outline(Pixmap input, Color color){
		Pixmap pixmap = copy(input);
		pixmap.setColor(color);

		for(int x = 0; x < pixmap.getWidth(); x++){
			for(int y = 0; y < pixmap.getHeight(); y++){
				if(empty(input.getPixel(x, y)) &&
						(!empty(input.getPixel(x, y + 1)) || !empty(input.getPixel(x, y - 1)) || !empty(input.getPixel(x - 1, y)) || !empty(input.getPixel(x + 1, y))))
					pixmap.drawPixel(x, y);
			}
		}
		return pixmap;
	}

	public static Pixmap zoom(Pixmap input, int scale){
		Pixmap pixmap = new Pixmap(input.getWidth(), input.getHeight(), Format.RGBA8888);
		for(int x = 0; x < pixmap.getWidth(); x++){
			for(int y = 0; y < pixmap.getHeight(); y++){
				pixmap.drawPixel(x, y, input.getPixel(x / scale + pixmap.getWidth() / 2 / scale, y / scale + pixmap.getHeight() / 2 / scale));
			}
		}
		return pixmap;
	}

	public static Pixmap resize(Pixmap input, int width, int height){
		Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
		pixmap.drawPixmap(input, width / 2 - input.getWidth() / 2, height / 2 - input.getHeight() / 2);

		return pixmap;
	}
	
	public static Pixmap resize(Pixmap input, int width, int height, int backgroundColor){
		Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
		pixmap.setColor(backgroundColor);
		pixmap.fill();
		pixmap.drawPixmap(input, width / 2 - input.getWidth() / 2, height / 2 - input.getHeight() / 2);

		return pixmap;
	}

	public static Pixmap crop(Pixmap input, int x, int y, int width, int height){
		Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
		pixmap.drawPixmap(input, 0, 0, x, y, width, height);
		return pixmap;
	}

	public static Pixmap rotate(Pixmap input, float angle){
		Vector2 vector = new Vector2();
		Pixmap pixmap = new Pixmap(input.getHeight(), input.getWidth(), Format.RGBA8888);

		for(int x = 0; x < input.getWidth(); x++){
			for(int y = 0; y < input.getHeight(); y++){
				vector.set(x - input.getWidth() / 2f + 0.5f, y - input.getHeight() / 2f);
				vector.rotate(-angle);
				int px = (int) (vector.x + input.getWidth() / 2f + 0.01f);
				int py = (int) (vector.y + input.getHeight() / 2f + 0.01f);
				pixmap.drawPixel(px - input.getWidth() / 2 + pixmap.getWidth() / 2, py - input.getHeight() / 2 + pixmap.getHeight() / 2, input.getPixel(x, y));
			}
		}

		return pixmap;
	}

	private static boolean empty(int i){
		return (i & 0x000000ff) == 0;
	}

	public static boolean isDisposed(Pixmap pix){
		return (Boolean) UCore.getPrivate(pix, "disposed");
	}

	public static void traverse(Pixmap input, PixmapTraverser t){
		for(int x = 0; x < input.getWidth(); x++){
			for(int y = 0; y < input.getHeight(); y++){
				t.traverse(x, y);
			}
		}
	}

	public static interface PixmapTraverser{
		public void traverse(int x, int y);
	}

	public static Pixmap huePixmap(int width, int height){
		Pixmap pixmap = new Pixmap(width, height, Format.RGBA8888);
		Color color = new Color(1, 1, 1, 1);

		for(int x = 0; x < width; x++){
			Hue.fromHSB(x / (float) width, 1f, 1, color);
			pixmap.setColor(color);
			for(int y = 0; y < height; y++){
				pixmap.drawPixel(x, y);
			}
		}
		return pixmap;
	}

	public static Texture hueTexture(int width, int height){
		return new Texture(huePixmap(width, height));
	}

	public static Pixmap blankPixmap(){
		Pixmap pixmap = new Pixmap(1, 1, Format.RGBA8888);
		pixmap.setColor(Color.WHITE);
		pixmap.fill();
		return pixmap;
	}

	public static Texture blankTexture(){
		Texture texture = new Texture(blankPixmap());
		texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat);
		return texture;
	}

	public static TextureRegion blankTextureRegion(){
		return new TextureRegion(blankTexture());
	}
	
	/**Platform-safe pixmapIO write.*/
	public static void write(Pixmap pixmap, FileHandle file){
		try{
			ClassReflection.getMethod(ClassReflection.forName("com.badlogic.gdx.graphics.PixmapIO"), 
					"writePNG", FileHandle.class, Pixmap.class)
			.invoke(null, file, pixmap);
		}catch(ReflectionException e){
			throw new RuntimeException(e);
		}
	}

	/** NOTE: REQUIRES BINDING THE TEXTURE FIRST! */
	public static void drawPixel(Texture texture, int x, int y, int color){
		bytes.clear();
		bytes.position(0);
		bytes.putInt(color);
		bytes.position(0);

		Gdx.gl.glTexSubImage2D(texture.glTarget, 0, x, y, 1, 1, 6408, 5121, bytes);
	}
}
