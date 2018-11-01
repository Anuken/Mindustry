package io.anuke.ucore.util;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ObjectMap;


/**Utility class that improves on TextureAtlas - 
 * faster texture lookups with an ObjectMap and automatic error textures.*/
public class Atlas extends TextureAtlas{
	ObjectMap<String, AtlasRegion> regionmap = new ObjectMap<>();
	ObjectMap<Texture, Pixmap> pixmaps = new ObjectMap<>();
	AtlasRegion error;
	
	public Atlas(String filename){
		this(Gdx.files.internal("sprites/"+filename));
	}
	
	public Atlas(FileHandle file){
		super(file);
		for(AtlasRegion r : super.getRegions()){
			regionmap.put(r.name, r);
		}
		error = findRegion("error");
	}
	
	public Pixmap getPixmapOf(String name){
		return getPixmapOf(findRegion(name));
	}
	
	/**Used for getting the pixmap of a certain region.
	 * Note that it is internally stored and disposed when needed.
	 * @param region the region to use.
	 * @return the pixmap of the region.*/
	public Pixmap getPixmapOf(TextureRegion region){
		Texture texture = region.getTexture();
		if(pixmaps.containsKey(texture)) return pixmaps.get(texture);
		texture.getTextureData().prepare();
		Pixmap pixmap = texture.getTextureData().consumePixmap();
		pixmaps.put(texture, pixmap);
		return pixmap;
	}
	
	@Override
	public AtlasRegion addRegion (String name, Texture texture, int x, int y, int width, int height) {
		AtlasRegion aregion = super.addRegion(name, texture, x, y, width, height);
		regionmap.put(name, aregion);
		return aregion;
	}
	
	/**Sets the error region to the specified region.*/
	public void setErrorRegion(AtlasRegion region){
		error = region;
	}

	/**Note that this returns the 'error' region if this region is not found.*/
	@Override
	public AtlasRegion findRegion(String name){
		AtlasRegion r = regionmap.get(name);
		if(r == null && error == null && !(name.equals("error"))) throw new IllegalArgumentException("The region \"" + name + "\" does not exist!");
		if(r == null) return error;
		return r;
	}

	public float regionHeight(String name){
		return findRegion(name).getRegionHeight();
	}

	public float regionWidth(String name){
		return findRegion(name).getRegionWidth();
	}
	
	/** @return whether or not the specified region is found.*/
	public boolean hasRegion(String s){
		return regionmap.containsKey(s);
	}
	
	public void dispose(){
		super.dispose();
		for(Pixmap pixmap : pixmaps.values()){
			pixmap.dispose();
		}
	}

	public TextureRegion getRegion(String name){
		return findRegion(name);
	}
}
