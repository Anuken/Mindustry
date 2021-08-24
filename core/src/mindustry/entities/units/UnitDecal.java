package mindustry.entities.units;

import arc.graphics.*;
import mindustry.graphics.*;

/** A sprite drawn in addition to the base unit sprites. */
public class UnitDecal{
    public String region = "error";
    public float x, y, rotation;
    public float layer = Layer.flyingUnit + 1f;
    public float xScale = 1f, yScale = 1f;
    public Color color = Color.white;

    public UnitDecal(String region, float x, float y, float rotation, float layer, Color color){
        this.region = region;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.layer = layer;
        this.color = color;
    }

    public UnitDecal(){
    }
}
