package io.anuke.mindustry.type;

import io.anuke.arc.graphics.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.ctype.*;
import io.anuke.mindustry.ui.*;

public class Liquid extends UnlockableContent{
    public final @NonNull Color color;

    /** Color used in bars. */
    public @Nullable Color barColor;
    /** Color used to draw lights. Note that the alpha channel is used to dictate brightness. */
    public Color lightColor = Color.clear.cpy();
    /** 0-1, 0 is completely inflammable, anything above that may catch fire when exposed to heat, 0.5+ is very flammable. */
    public float flammability;
    /** temperature: 0.5 is 'room' temperature, 0 is very cold, 1 is molten hot */
    public float temperature = 0.5f;
    /** how much heat this liquid can store. 0.4=water (decent), anything lower is probably less dense and bad at cooling. */
    public float heatCapacity = 0.5f;
    /** how thick this liquid is. 0.5=water (relatively viscous), 1 would be something like tar (very slow) */
    public float viscosity = 0.5f;
    /** how prone to exploding this liquid is, when heated. 0 = nothing, 1 = nuke */
    public float explosiveness;
    /** The associated status effect. */
    public StatusEffect effect = StatusEffects.none;

    public Liquid(String name, Color color){
        super(name);
        this.color = new Color(color);
    }

    /** For modding only.*/
    public Liquid(String name){
        this(name, new Color(Color.black));
    }

    public boolean canExtinguish(){
        return flammability < 0.1f && temperature <= 0.5f;
    }

    public Color barColor(){
        return barColor == null ? color : barColor;
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayLiquid(table, this);
    }

    @Override
    public String toString(){
        return localizedName;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.liquid;
    }
}
