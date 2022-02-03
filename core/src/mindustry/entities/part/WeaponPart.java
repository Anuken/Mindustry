package mindustry.entities.part;

import arc.graphics.g2d.*;
import arc.struct.*;

public abstract class WeaponPart{
    public static final PartParams params = new PartParams();

    /** If true, turret shading is used. Don't touch this, it is set up in unit/block init()! */
    public boolean turretShading;

    public abstract void draw(PartParams params);
    public abstract void load(String name);
    public void getOutlines(Seq<TextureRegion> out){}

    /** Parameters for drawing a part in draw(). */
    public static class PartParams{
        //TODO document
        public float warmup, reload, heat;
        public float x, y, rotation;

        public PartParams set(float warmup, float reload, float heat, float x, float y, float rotation){
            this.warmup = warmup;
            this.reload = reload;
            this.heat = heat;
            this.x = x;
            this.y = y;
            this.rotation = rotation;
            return this;
        }
    }
}
