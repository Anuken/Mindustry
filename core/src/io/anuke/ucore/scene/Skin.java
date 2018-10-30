/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package io.anuke.ucore.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont.BitmapFontData;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasSprite;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.utils.Json.ReadOnlySerializer;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;

import io.anuke.ucore.util.Atlas;
import io.anuke.ucore.scene.style.*;

/** A skin stores resources for UI widgets to use (texture regions, ninepatches, fonts, colors, etc). Resources are named and can
 * be looked up by name and type. Resources can be described in JSON. Skin provides useful conversions, such as allowing access to
 * regions in the atlas as ninepatches, sprites, drawables, etc. The get* methods return an instance of the object in the skin.
 * The new* methods return a copy of an instance in the skin.
 * <p>
 * See the <a href="https://github.com/libgdx/libgdx/wiki/Skin">documentation</a> for more.
 * @author Nathan Sweet */
public class Skin implements Disposable {
	ObjectMap<Class, ObjectMap<String, Object>> resources = new ObjectMap<>();
	Atlas atlas;

	/** Creates an empty skin. */
	public Skin () {}

	/** Creates a skin containing the resources in the specified skin JSON file. If a file in the same directory with a ".atlas"
	 * extension exists, it is loaded as a {@link TextureAtlas} and the texture regions added to the skin. The atlas is
	 * automatically disposed when the skin is disposed. */
	public Skin (FileHandle skinFile) {
		FileHandle atlasFile = skinFile.sibling(skinFile.nameWithoutExtension() + ".atlas");
		if (atlasFile.exists()) {
			atlas = new Atlas(atlasFile);
			addRegions(atlas);
		}

		load(skinFile);
	}

	/** Creates a skin containing the resources in the specified skin JSON file and the texture regions from the specified atlas.
	 * The atlas is automatically disposed when the skin is disposed. */
	public Skin (FileHandle skinFile, Atlas atlas) {
		this.atlas = atlas;
		addRegions(atlas);
		load(skinFile);
	}

	/** Creates a skin containing the texture regions from the specified atlas. The atlas is automatically disposed when the skin
	 * is disposed. */
	public Skin (Atlas atlas) {
		this.atlas = atlas;
		addRegions(atlas);
	}
	
	public BitmapFont font(){
		return getFont("default-font");
	}

	/** Adds all resources in the specified skin JSON file. */
	public void load (FileHandle skinFile) {
		try {
			getJsonLoader(skinFile).fromJson(Skin.class, skinFile);
		} catch (SerializationException ex) {
			throw new SerializationException("Error reading file: " + skinFile, ex);
		}
	}

	/** Adds all named texture regions from the atlas. The atlas will not be automatically disposed when the skin is disposed. */
	public void addRegions (TextureAtlas atlas) {
		Array<AtlasRegion> regions = atlas.getRegions();
		for (int i = 0, n = regions.size; i < n; i++) {
			AtlasRegion region = regions.get(i);
			String name = region.name;
			if (region.index != -1) {
			    name += "_" + region.index;
			}
			add(name, region, TextureRegion.class);
		}
	}

	public void add (String name, Object resource) {
		add(name, resource, resource.getClass());
	}

	public void add (String name, Object resource, Class type) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		if (resource == null) throw new IllegalArgumentException("resource cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(type);
		if (typeResources == null) {
			typeResources = new ObjectMap<>(type == TextureRegion.class || type == Drawable.class || type == Sprite.class ? 256 : 64);
			resources.put(type, typeResources);
		}
		typeResources.put(name, resource);
	}

	public void remove (String name, Class type) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(type);
		typeResources.remove(name);
	}

	/** Returns a resource named "default" for the specified type.
	 * @throws GdxRuntimeException if the resource was not found. */
	public <T> T get (Class<T> type) {
		return get("default", type);
	}

	/** Returns a named resource of the specified type.
	 * @throws GdxRuntimeException if the resource was not found. */
	public <T> T get (String name, Class<T> type) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");

		if (type == Drawable.class) return (T)getDrawable(name);
		if (type == TextureRegion.class) return (T)getRegion(name);
		if (type == NinePatch.class) return (T)getPatch(name);
		if (type == Sprite.class) return (T)getSprite(name);

		ObjectMap<String, Object> typeResources = resources.get(type);
		if (typeResources == null) throw new GdxRuntimeException("No " + type.getName() + " registered with name: " + name);
		Object resource = typeResources.get(name);
		if (resource == null) throw new GdxRuntimeException("No " + type.getName() + " registered with name: " + name);
		return (T)resource;
	}

	/** Returns a named resource of the specified type.
	 * @return null if not found. */
	public <T> T optional (String name, Class<T> type) {
		if (name == null) throw new IllegalArgumentException("name cannot be null.");
		if (type == null) throw new IllegalArgumentException("type cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(type);
		if (typeResources == null) return null;
		return (T)typeResources.get(name);
	}

	public boolean has (String name, Class type) {
		ObjectMap<String, Object> typeResources = resources.get(type);
		if (typeResources == null) return false;
		return typeResources.containsKey(name);
	}

	/** Returns the name to resource mapping for the specified type, or null if no resources of that type exist. */
	public <T> ObjectMap<String, T> getAll (Class<T> type) {
		return (ObjectMap<String, T>)resources.get(type);
	}

	public Color getColor (String name) {
		return get(name, Color.class);
	}

	public BitmapFont getFont (String name) {
		return get(name, BitmapFont.class);
	}

	/** Returns a registered texture region. If no region is found but a texture exists with the name, a region is created from the
	 * texture and stored in the skin. */
	public TextureRegion getRegion (String name) {
		TextureRegion region = optional(name, TextureRegion.class);
		if (region != null) return region;

		Texture texture = optional(name, Texture.class);
		if (texture == null) throw new GdxRuntimeException("No TextureRegion or Texture registered with name: " + name);
		region = new TextureRegion(texture);
		add(name, region, TextureRegion.class);
		return region;
	}

	/** @return an array with the {@link TextureRegion} that have an index != -1, or null if none are found. */
	public Array<TextureRegion> getRegions (String regionName) {
		Array<TextureRegion> regions = null;
		int i = 0;
		TextureRegion region = optional(regionName + "_" + (i++), TextureRegion.class);
		if (region != null) {
			regions = new Array<TextureRegion>();
			while (region != null) {
				regions.add(region);
				region = optional(regionName + "_" + (i++), TextureRegion.class);
			}
		}
		return regions;
	}

	/** Returns a registered tiled drawable. If no tiled drawable is found but a region exists with the name, a tiled drawable is
	 * created from the region and stored in the skin. */
	public TiledDrawable getTiledDrawable (String name) {
		TiledDrawable tiled = optional(name, TiledDrawable.class);
		if (tiled != null) return tiled;

		tiled = new TiledDrawable(getRegion(name));
		tiled.setName(name);
		add(name, tiled, TiledDrawable.class);
		return tiled;
	}

	/** Returns a registered ninepatch. If no ninepatch is found but a region exists with the name, a ninepatch is created from the
	 * region and stored in the skin. If the region is an {@link AtlasRegion} then the {@link AtlasRegion#splits} are used,
	 * otherwise the ninepatch will have the region as the center patch. */
	public NinePatch getPatch (String name) {
		NinePatch patch = optional(name, NinePatch.class);
		if (patch != null) return patch;

		try {
			TextureRegion region = getRegion(name);
			if (region instanceof AtlasRegion) {
				int[] splits = ((AtlasRegion)region).splits;
				if (splits != null) {
					patch = new NinePatch(region, splits[0], splits[1], splits[2], splits[3]);
					int[] pads = ((AtlasRegion)region).pads;
					if (pads != null) patch.setPadding(pads[0], pads[1], pads[2], pads[3]);
				}
			}
			if (patch == null) patch = new NinePatch(region);
			add(name, patch, NinePatch.class);
			return patch;
		} catch (GdxRuntimeException ex) {
			throw new GdxRuntimeException("No NinePatch, TextureRegion, or Texture registered with name: " + name);
		}
	}

	/** Returns a registered sprite. If no sprite is found but a region exists with the name, a sprite is created from the region
	 * and stored in the skin. If the region is an {@link AtlasRegion} then an {@link AtlasSprite} is used if the region has been
	 * whitespace stripped or packed rotated 90 degrees. */
	public Sprite getSprite (String name) {
		Sprite sprite = optional(name, Sprite.class);
		if (sprite != null) return sprite;

		try {
			TextureRegion textureRegion = getRegion(name);
			if (textureRegion instanceof AtlasRegion) {
				AtlasRegion region = (AtlasRegion)textureRegion;
				if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight)
					sprite = new AtlasSprite(region);
			}
			if (sprite == null) sprite = new Sprite(textureRegion);
			add(name, sprite, Sprite.class);
			return sprite;
		} catch (GdxRuntimeException ex) {
			throw new GdxRuntimeException("No NinePatch, TextureRegion, or Texture registered with name: " + name);
		}
	}

	/** Returns a registered drawable. If no drawable is found but a region, ninepatch, or sprite exists with the name, then the
	 * appropriate drawable is created and stored in the skin. */
	public Drawable getDrawable (String name) {
		Drawable drawable = optional(name, Drawable.class);
		if (drawable != null) return drawable;

		// Use texture or texture region. If it has splits, use ninepatch. If it has rotation or whitespace stripping, use sprite.
		try {
			TextureRegion textureRegion = getRegion(name);
			if (textureRegion instanceof AtlasRegion) {
				AtlasRegion region = (AtlasRegion)textureRegion;
				if (region.splits != null)
					drawable = (new ScaledNinePatchDrawable(getPatch(name)));
				else if (region.rotate || region.packedWidth != region.originalWidth || region.packedHeight != region.originalHeight)
					drawable = new SpriteDrawable(getSprite(name));
			}
			if (drawable == null) drawable = new TextureRegionDrawable(textureRegion);
		} catch (GdxRuntimeException ignored) {
		}

		// Check for explicit registration of ninepatch, sprite, or tiled drawable.
		if (drawable == null) {
			NinePatch patch = optional(name, NinePatch.class);
			if (patch != null)
				drawable = new NinePatchDrawable(patch);
			else {
				Sprite sprite = optional(name, Sprite.class);
				if (sprite != null)
					drawable = new SpriteDrawable(sprite);
				else
					throw new GdxRuntimeException(
						"No Drawable, NinePatch, TextureRegion, Texture, or Sprite registered with name: " + name);
			}
		}

		if (drawable instanceof BaseDrawable) ((BaseDrawable)drawable).setName(name);

		add(name, drawable, Drawable.class);
		return drawable;
	}

	/** Returns the name of the specified style object, or null if it is not in the skin. This compares potentially every style
	 * object in the skin of the same type as the specified style, which may be a somewhat expensive operation. */
	public String find (Object resource) {
		if (resource == null) throw new IllegalArgumentException("style cannot be null.");
		ObjectMap<String, Object> typeResources = resources.get(resource.getClass());
		if (typeResources == null) return null;
		return typeResources.findKey(resource, true);
	}

	/** Returns a copy of a drawable found in the skin via {@link #getDrawable(String)}. */
	public Drawable newDrawable (String name) {
		return newDrawable(getDrawable(name));
	}

	/** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
	public Drawable newDrawable (String name, float r, float g, float b, float a) {
		return newDrawable(getDrawable(name), new Color(r, g, b, a));
	}

	/** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
	public Drawable newDrawable (String name, Color tint) {
		return newDrawable(getDrawable(name), tint);
	}

	/** Returns a copy of the specified drawable. */
	public Drawable newDrawable (Drawable drawable) {
		if (drawable instanceof TiledDrawable) return new TiledDrawable((TiledDrawable)drawable);
		if (drawable instanceof TextureRegionDrawable) return new TextureRegionDrawable((TextureRegionDrawable)drawable);
		if (drawable instanceof NinePatchDrawable) return new NinePatchDrawable((NinePatchDrawable)drawable);
		if (drawable instanceof SpriteDrawable) return new SpriteDrawable((SpriteDrawable)drawable);
		throw new GdxRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass());
	}

	/** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
	public Drawable newDrawable (Drawable drawable, float r, float g, float b, float a) {
		return newDrawable(drawable, new Color(r, g, b, a));
	}

	/** Returns a tinted copy of a drawable found in the skin via {@link #getDrawable(String)}. */
	public Drawable newDrawable (Drawable drawable, Color tint) {
		Drawable newDrawable;
		if (drawable instanceof TextureRegionDrawable)
			newDrawable = ((TextureRegionDrawable)drawable).tint(tint);
		else if (drawable instanceof NinePatchDrawable)
			newDrawable = ((NinePatchDrawable)drawable).tint(tint);
		else if (drawable instanceof SpriteDrawable)
			newDrawable = ((SpriteDrawable)drawable).tint(tint);
		else
			throw new GdxRuntimeException("Unable to copy, unknown drawable type: " + drawable.getClass());

		if (newDrawable instanceof BaseDrawable) {
			BaseDrawable named = (BaseDrawable)newDrawable;
			if (drawable instanceof BaseDrawable)
				named.setName(((BaseDrawable)drawable).getName() + " (" + tint + ")");
			else
				named.setName(" (" + tint + ")");
		}

		return newDrawable;
	}

	/** Sets the style on the actor to disabled or enabled. This is done by appending "-disabled" to the style name when enabled is
	 * false, and removing "-disabled" from the style name when enabled is true. A method named "getStyle" is called the actor via
	 * reflection and the name of that style is found in the skin. If the actor doesn't have a "getStyle" method or the style was
	 * not found in the skin, no exception is thrown and the actor is left unchanged. */
	public void setEnabled (Element actor, boolean enabled) {
		// Get current style.
		Method method = findMethod(actor.getClass(), "getStyle");
		if (method == null) return;
		Object style;
		try {
			style = method.invoke(actor);
		} catch (Exception ignored) {
			return;
		}
		// Determine new style.
		String name = find(style);
		if (name == null) return;
		name = name.replace("-disabled", "") + (enabled ? "" : "-disabled");
		style = get(name, style.getClass());
		// Set new style.
		method = findMethod(actor.getClass(), "setStyle");
		if (method == null) return;
		try {
			method.invoke(actor, style);
		} catch (Exception ignored) {
		}
	}

	/** Returns the {@link TextureAtlas} passed to this skin constructor, or null. */
	public Atlas getAtlas () {
		return atlas;
	}

	/** Disposes the {@link TextureAtlas} and all {@link Disposable} resources in the skin. */
	public void dispose () {
		if (atlas != null) atlas.dispose();
		for (ObjectMap<String, Object> entry : resources.values()) {
			for (Object resource : entry.values())
				if (resource instanceof Disposable) ((Disposable)resource).dispose();
		}
	}

	protected Json getJsonLoader (final FileHandle skinFile) {
		final Skin skin = this;

		final Json json = new Json() {
			public <T> T readValue (Class<T> type, Class elementType, JsonValue jsonData) {
				// If the JSON is a string but the type is not, look up the actual value by name.
				if (jsonData.isString() && !ClassReflection.isAssignableFrom(CharSequence.class, type))
					return get(jsonData.asString(), type);
				return super.readValue(type, elementType, jsonData);
			}
		};
		json.setTypeName(null);
		json.setUsePrototypes(false);

		json.setSerializer(Skin.class, new ReadOnlySerializer<Skin>() {
			public Skin read (Json json, JsonValue typeToValueMap, Class ignored) {
				for (JsonValue valueMap = typeToValueMap.child; valueMap != null; valueMap = valueMap.next) {
					try {
						readNamedObjects(json, ClassReflection.forName(valueMap.name()), valueMap);
					} catch (ReflectionException ex) {
						throw new SerializationException(ex);
					}
				}
				return skin;
			}

			private void readNamedObjects (Json json, Class type, JsonValue valueMap) {
				Class addType = type == TintedDrawable.class ? Drawable.class : type;
				for (JsonValue valueEntry = valueMap.child; valueEntry != null; valueEntry = valueEntry.next) {
					Object object = json.readValue(type, valueEntry);
					if (object == null) continue;
					try {
						add(valueEntry.name, object, addType);
						if (addType != Drawable.class && ClassReflection.isAssignableFrom(Drawable.class, addType))
							add(valueEntry.name, object, Drawable.class);
					} catch (Exception ex) {
						throw new SerializationException(
							"Error reading " + ClassReflection.getSimpleName(type) + ": " + valueEntry.name, ex);
					}
				}
			}
		});

		json.setSerializer(BitmapFont.class, new ReadOnlySerializer<BitmapFont>() {
			public BitmapFont read (Json json, JsonValue jsonData, Class type) {
				String path = json.readValue("file", String.class, jsonData);
				int scaledSize = json.readValue("scaledSize", int.class, -1, jsonData);
				Boolean flip = json.readValue("flip", Boolean.class, false, jsonData);
				Boolean markupEnabled = json.readValue("markupEnabled", Boolean.class, false, jsonData);
				
				float scale = json.readValue("scale", float.class, -1f, jsonData);

				FileHandle fontFile = skinFile.parent().child(path);
				if (!fontFile.exists()) fontFile = Gdx.files.internal(path);
				if (!fontFile.exists()) throw new SerializationException("Font file not found: " + fontFile);

				// Use a region with the same name as the font, else use a PNG file in the same directory as the FNT file.
				String regionName = fontFile.nameWithoutExtension();
				try {
					BitmapFont font;
					Array<TextureRegion> regions = skin.getRegions(regionName);
					if (regions != null)
						font = new BitmapFont(new BitmapFontData(fontFile, flip), regions, true);
					else {
						TextureRegion region = skin.optional(regionName, TextureRegion.class);
						if (region != null)
							font = new BitmapFont(fontFile, region, flip);
						else {
							FileHandle imageFile = fontFile.parent().child(regionName + ".png");
							if (imageFile.exists())
								font = new BitmapFont(fontFile, imageFile, flip);
							else
								font = new BitmapFont(fontFile, flip);
						}
					}
					font.getData().markupEnabled = markupEnabled;
					// Scaled size is the desired cap height to scale the font to.
					if (scaledSize != -1) font.getData().setScale(scaledSize / font.getCapHeight());
					if(scale > 0) font.getData().setScale(scale);
					return font;
				} catch (RuntimeException ex) {
					throw new SerializationException("Error loading bitmap font: " + fontFile, ex);
				}
			}
		});

		json.setSerializer(Color.class, new ReadOnlySerializer<Color>() {
			public Color read (Json json, JsonValue jsonData, Class type) {
				if (jsonData.isString()) return get(jsonData.asString(), Color.class);
				String hex = json.readValue("hex", String.class, (String)null, jsonData);
				if (hex != null) return Color.valueOf(hex);
				float r = json.readValue("r", float.class, 0f, jsonData);
				float g = json.readValue("g", float.class, 0f, jsonData);
				float b = json.readValue("b", float.class, 0f, jsonData);
				float a = json.readValue("a", float.class, 1f, jsonData);
				return new Color(r, g, b, a);
			}
		});

		json.setSerializer(TintedDrawable.class, new ReadOnlySerializer() {
			public Object read (Json json, JsonValue jsonData, Class type) {
				String name = json.readValue("name", String.class, jsonData);
				Color color = json.readValue("color", Color.class, jsonData);
				Drawable drawable = newDrawable(name, color);
				if (drawable instanceof BaseDrawable) {
					BaseDrawable named = (BaseDrawable)drawable;
					named.setName(jsonData.name + " (" + name + ", " + color + ")");
				}
				return drawable;
			}
		});

		return json;
	}

	static private Method findMethod (Class type, String name) {
		Method[] methods = ClassReflection.getMethods(type);
		for (int i = 0, n = methods.length; i < n; i++) {
			Method method = methods[i];
			if (method.getName().equals(name)) return method;
		}
		return null;
	}

	/** @author Nathan Sweet */
	static public class TintedDrawable {
		public String name;
		public Color color;
	}
}
