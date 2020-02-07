package mindustry.type;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import mindustry.annotations.Annotations.*;
import mindustry.ctype.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.ui.*;

//TODO change to UnitType or Shell or something
public class UnitDef extends UnlockableContent{
    //TODO implement
    public @NonNull Prov<? extends UnitController> defaultController = AIController::new;
    public @NonNull Prov<? extends Unitc> constructor;
    public boolean flying;
    public float speed = 1.1f, boostSpeed = 0.75f, rotateSpeed = 0.2f, baseRotateSpeed = 0.1f;
    public float drag = 0.3f, mass = 1f, accel = 0.1f;
    public float health = 200f, range = -1;
    public boolean targetAir = false, targetGround = false;
    public boolean faceTarget = true; //equivalent to turnCursor

    public int itemCapacity = 30;
    public int drillTier = -1;
    public float buildPower = 1f, minePower = 1f;

    public Color engineColor = Pal.boostTo;
    public float engineOffset = 5f, engineSize = 2.5f;

    public float hitsize = 6f, hitsizeTile = 4f;
    public float cellOffsetX = 0f, cellOffsetY = 0f;
    public float lightRadius = 60f, lightOpacity = 0.6f;
    public Color lightColor = Pal.powerLight;
    public boolean drawCell = true, drawItems = true;

    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public Array<Weapon> weapons = new Array<>();
    public TextureRegion baseRegion, legRegion, region, cellRegion;

    public UnitDef(String name, Prov<Unitc> constructor){
        super(name);
        this.constructor = constructor;
    }

    public UnitDef(String name){
        this(name, () -> Nulls.unit);
    }

    public UnitController createController(){
        return defaultController.get();
    }

    public Unitc create(Team team){
        Unitc unit = constructor.get();
        unit.team(team);
        unit.type(this);
        return unit;
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayUnit(table, this);
    }

    @CallSuper
    @Override
    public void init(){
        //set up default range
        if(range < 0){
            for(Weapon weapon : weapons){
                range = Math.max(range, weapon.bullet.range());
            }
        }
    }

    @CallSuper
    @Override
    public void load(){
        weapons.each(Weapon::load);
        region = Core.atlas.find(name);
        legRegion = Core.atlas.find(name + "-leg");
        baseRegion = Core.atlas.find(name + "-base");
        cellRegion = Core.atlas.find(name + "-cell", Core.atlas.find("power-cell"));
    }

    @Override
    public ContentType getContentType(){
        return ContentType.unit;
    }
}
