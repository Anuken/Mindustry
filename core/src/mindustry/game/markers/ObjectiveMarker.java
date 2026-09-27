package mindustry.game.markers;

import arc.*;
import arc.math.*;
import arc.util.serialization.*;
import arc.util.serialization.Json.*;
import mindustry.core.*;
import mindustry.game.objectives.MapObjectives.*;
import mindustry.graphics.*;
import mindustry.logic.*;

import static mindustry.Vars.*;

/** Marker used for drawing various content to indicate something along with an objective. Mostly used as UI overlay. */
public abstract class ObjectiveMarker implements JsonSerializable{
    /** Whether to display marker in the world. Do not modify directly if added, use control() instead. */
    public @IndexBool int world = 1;
    /** Whether to display marker on the minimap. Do not modify directly if added, use control() instead. */
    public @IndexBool int minimap = -1;
    /** Whether to use the marker as light. Do not modify directly if added, use control() instead. */
    public @IndexBool int light = -1;
    /** Whether to scale marker corresponding to player's zoom level. */
    public boolean autoscale = false;
    /** On which z-sorting layer is marker drawn. */
    protected float drawLayer = Layer.overlayUI;

    public void draw(float scaleFactor){
    }

    public void drawLight(float scaleFactor){
        draw(scaleFactor);
    }

    /** Control marker with world processor code. Ignores NaN (null) values. */
    public void control(LMarkerControl type, double p1, double p2, double p3){
        if(Double.isNaN(p1)) return;

        switch(type){
            case world -> state.markers.updateMarker(state.markers.worldMarkers, this, !Mathf.equal((float)p1, 0f), m -> m.world, (m, i) -> m.world = i);
            case minimap -> state.markers.updateMarker(state.markers.mapMarkers, this, !Mathf.equal((float)p1, 0f), m -> m.minimap, (m, i) -> m.minimap = i);
            case light -> state.markers.updateMarker(state.markers.lightMarkers, this, !Mathf.equal((float)p1, 0f), m -> m.light, (m, i) -> m.light = i);
            case autoscale -> autoscale = !Mathf.equal((float)p1, 0f);
            case drawLayer -> drawLayer = (float)p1;
        }
    }

    public void setText(String text, boolean fetch){
    }

    public void setTexture(Object texture){
    }

    /** @return The localized type-name of this objective, defaulting to the class simple name without the "Marker" prefix. */
    public String typeName(){
        String className = getClass().getSimpleName().replace("Marker", "");
        return Core.bundle == null ? className : Core.bundle.get("marker." + className.toLowerCase() + ".name", className);
    }

    public static String fetchText(String text){
        if(text == null) return "";

        if(text.startsWith("@")){
            String key = text.substring(1);

            String out;
            if(mobile){
                out =
                state.mapLocales.containsProperty(key + ".mobile") ?
                state.mapLocales.getProperty(key + ".mobile") :
                state.mapLocales.containsProperty(key) ?
                state.mapLocales.getProperty(key) :
                Core.bundle.get(key + ".mobile", Core.bundle.get(key));
            }else{
                out =
                state.mapLocales.containsProperty(key) ?
                state.mapLocales.getProperty(key) :
                Core.bundle.get(key);
            }
            return UI.formatIcons(out);
        }else{
            return UI.formatIcons(text);
        }
    }

    @Override
    public void write(Json json, JsonWriter writer){
        json.writeFields(writer, this);
    }

    private void updateField(Jval value, String name){
        Jval sub = value.get(name);
        if(sub != null && sub.isBoolean()) value.put(name, sub.asBool() ? 1 : -1);
    }

    @Override
    public void read(Json json, Jval jsonData){
        updateField(jsonData, "world");
        updateField(jsonData, "minimap");
        updateField(jsonData, "light");

        json.readFields(this, jsonData);
        if(jsonData.has("textureName")) setTexture(jsonData.getString("textureName"));
    }
}
