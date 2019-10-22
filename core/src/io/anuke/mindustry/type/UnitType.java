package io.anuke.mindustry.type;

import io.anuke.arc.*;
import io.anuke.arc.audio.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.ctype.UnlockableContent;
import io.anuke.mindustry.entities.type.*;
import io.anuke.mindustry.game.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.ui.*;

public class UnitType extends UnlockableContent{
    public @NonNull TypeID typeID;
    public @NonNull Supplier<? extends BaseUnit> constructor;

    public float health = 60;
    public float hitsize = 7f;
    public float hitsizeTile = 4f;
    public float speed = 0.4f;
    public float range = 0, attackLength = 150f;
    public float rotatespeed = 0.2f;
    public float baseRotateSpeed = 0.1f;
    public float shootCone = 15f;
    public float mass = 1f;
    public boolean flying;
    public boolean targetAir = true;
    public boolean rotateWeapon = false;
    public float drag = 0.1f;
    public float maxVelocity = 5f;
    public float retreatPercent = 0.6f;
    public int itemCapacity = 30;
    public ObjectSet<Item> toMine = ObjectSet.with(Items.lead, Items.copper);
    public float buildPower = 0.3f, minePower = 0.7f;
    public @NonNull Weapon weapon;
    public float weaponOffsetY, engineOffset = 6f, engineSize = 2f;
    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public TextureRegion legRegion, baseRegion, region;

    public <T extends BaseUnit> UnitType(String name, Supplier<T> mainConstructor){
        this(name);
        create(mainConstructor);
    }

    public <T extends BaseUnit> UnitType(String name){
        super(name);
        this.description = Core.bundle.getOrNull("unit." + name + ".description");
    }

    public <T extends BaseUnit> void create(Supplier<T> mainConstructor){
        this.constructor = mainConstructor;
        this.description = Core.bundle.getOrNull("unit." + name + ".description");
        this.typeID = new TypeID(name, mainConstructor);
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
    public void load(){
        weapon.load();
        region = Core.atlas.find(name);
        legRegion = Core.atlas.find(name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }

    public BaseUnit create(Team team){
        BaseUnit unit = constructor.get();
        unit.init(this, team);
        return unit;
    }
}
