package io.anuke.mindustry.entities.units;

import io.anuke.arc.Core;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.function.Supplier;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Strings;
import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Weapons;
import io.anuke.mindustry.entities.traits.TypeTrait;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.type.ContentType;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.Weapon;
import io.anuke.mindustry.ui.ContentDisplay;

public class UnitType extends UnlockableContent{
    protected final Supplier<? extends BaseUnit> constructor;

    public final String name;
    public final String description;
    public float health = 60;
    public float hitsize = 7f;
    public float hitsizeTile = 4f;
    public float speed = 0.4f;
    public float range = 0;
    public float rotatespeed = 0.2f;
    public float baseRotateSpeed = 0.1f;
    public float mass = 1f;
    public boolean isFlying;
    public boolean targetAir = true;
    public float drag = 0.1f;
    public float maxVelocity = 5f;
    public float retreatPercent = 0.2f;
    public float armor = 0f;
    public float carryWeight = 1f;
    public int itemCapacity = 30;
    public ObjectSet<Item> toMine = ObjectSet.with(Items.lead, Items.copper);
    public float buildPower = 0.3f, minePower = 0.7f;
    public Weapon weapon = Weapons.blaster;
    public float weaponOffsetX, weaponOffsetY;
    public Color trailColor = Color.valueOf("ffa665");

    public TextureRegion iconRegion, legRegion, baseRegion, region;

    public <T extends BaseUnit> UnitType(String name, Class<T> type, Supplier<T> mainConstructor){
        this.name = name;
        this.constructor = mainConstructor;
        this.description = Core.bundle.getOrNull("unit." + name + ".description");

        TypeTrait.registerType(type, mainConstructor);

        if(!Core.bundle.has("unit." + this.name + ".name")){
            Log.err("Warning: unit '" + name + "' is missing a localized name. Add the follow to bundle.properties:");
            Log.err("unit." + this.name + ".name=" + Strings.capitalize(name.replace('-', '_')));
        }
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayUnit(table, this);
    }

    @Override
    public String localizedName(){
        return Core.bundle.get("unit." + name + ".name");
    }

    @Override
    public TextureRegion getContentIcon(){
        return iconRegion;
    }

    @Override
    public void load(){
        iconRegion = Core.atlas.find("unit-icon-" + name);
        region = Core.atlas.find(name);

        if(!isFlying){
            legRegion = Core.atlas.find(name + "-leg");
            baseRegion = Core.atlas.find(name + "-base");
        }
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    @Override
    public String getContentName(){
        return name;
    }

    public BaseUnit create(Team team){
        BaseUnit unit = constructor.get();
        unit.init(this, team);
        return unit;
    }
}
