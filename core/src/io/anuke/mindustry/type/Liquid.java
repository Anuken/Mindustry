package io.anuke.mindustry.type;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.StatusEffects;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.ui.ContentDisplay;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.util.Bundles;

public class Liquid extends UnlockableContent{
    public final Color color;
    public final String name;
    public final String description;

    /**0-1, 0 is completely inflammable, anything above that may catch fire when exposed to heat, 0.5+ is very flammable.*/
    public float flammability;
    /**temperature: 0.5 is 'room' temperature, 0 is very cold, 1 is molten hot*/
    public float temperature = 0.5f;
    /**how much heat this liquid can store. 0.4=water (decent), anything lower is probably less dense and bad at cooling.*/
    public float heatCapacity = 0.5f;
    /**how thick this liquid is. 0.5=water (relatively viscous), 1 would be something like tar (very slow)*/
    public float viscosity = 0.5f;
    /**how prone to exploding this liquid is, when heated. 0 = nothing, 1 = nuke*/
    public float explosiveness;
    /**the burning color of this liquid*/
    public Color flameColor = Color.valueOf("ffb763");
    /**The associated status effect.*/
    public StatusEffect effect = StatusEffects.none;
    /**Pump tier. Controls which pumps can use this liquid.*/
    public int tier;
    /**Displayed icon.*/
    public TextureRegion iconRegion;

    public Liquid(String name, Color color){
        this.name = name;
        this.color = new Color(color);
        this.description = Bundles.getOrNull("liquid." + name + ".description");
    }

    public boolean canExtinguish(){
        return flammability < 0.1f && temperature <= 0.5f;
    }

    @Override
    public void load(){
        iconRegion = Draw.region("liquid-icon-" + name);
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayLiquid(table, this);
    }

    @Override
    public String localizedName(){
        return Bundles.get("liquid." + this.name + ".name");
    }

    @Override
    public TextureRegion getContentIcon(){
        return iconRegion;
    }

    @Override
    public String toString(){
        return localizedName();
    }

    @Override
    public String getContentName(){
        return name;
    }

    @Override
    public ContentType getContentType(){
        return ContentType.liquid;
    }
}
