package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.struct.*;
import arc.func.*;
import arc.graphics.Color;
import arc.graphics.g2d.*;
import arc.math.Mathf;
import arc.scene.ui.layout.*;
import arc.util.Time;
import arc.util.ArcAnnotate.*;
import mindustry.content.*;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.entities.type.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.ui.*;

public class UnitType extends UnlockableContent{
    public @NonNull TypeID typeID;
    public @NonNull Prov<? extends BaseUnit> constructor;

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

    /** draw the health and team indicator */
    public boolean drawCell = true;
    /** draw the items on its back */
    public boolean drawItems = true;
    /** light emitted with lighting map rule enabled */
    public float lightEmitted = 50f;

    public TextureRegion legRegion, baseRegion, region;

    public <T extends BaseUnit> UnitType(String name, Prov<T> mainConstructor){
        this(name);
        create(mainConstructor);
    }

    public UnitType(String name){
        super(name);
    }

    public <T extends BaseUnit> void create(Prov<T> mainConstructor){
        this.constructor = mainConstructor;
        this.description = Core.bundle.getOrNull("unit." + name + ".description");
        this.typeID = new TypeID(name, mainConstructor);
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayUnit(table, this);
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

    public void drawStats(Unit unit){
        if(drawCell){
            float health = unit.healthf();
            Draw.color(Color.black, unit.getTeam().color, health + Mathf.absin(Time.time(), Math.max(health * 5f, 1f), 1f - health));
            Draw.rect(unit.getPowerCellRegion(), unit.x, unit.y, unit.rotation - 90);
            Draw.color();
        }

        if(drawItems){
            unit.drawBackItems();
        }

        if(lightEmitted > 0f){
           unit.drawLight(lightEmitted);
        }
    }

    public void draw(Unit unit){
    }

    public void drawShadow(Unit unit, float offsetX, float offsetY){
        Draw.rect(unit.getIconRegion(), unit.x + offsetX, unit.y + offsetY, unit.rotation - 90);
    }
}
