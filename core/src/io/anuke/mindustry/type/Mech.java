package io.anuke.mindustry.type;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import io.anuke.mindustry.content.Weapons;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.ui.ContentDisplay;
import io.anuke.ucore.graphics.Draw;
import io.anuke.ucore.scene.ui.layout.Table;

//TODO merge unit type with mech
public class Mech extends Upgrade implements UnlockableContent{
    public boolean flying;

    public float speed = 1.1f;
    public float maxSpeed = 1.1f;
    public float boostSpeed = 0.75f;
    public float drag = 0.4f;
    public float mass = 1f;
    public float armor = 1f;

    public float mineSpeed = 1f;
    public int drillPower = -1;
    public float carryWeight = 10f;
    public float buildPower = 1f;
    public boolean canRepair = false;
    public Color trailColor = Color.valueOf("ffd37f");

    public float weaponOffsetX, weaponOffsetY;

    public Weapon weapon = Weapons.blaster;

    public int itemCapacity = 30;
    public int ammoCapacity = 100;

    public TextureRegion baseRegion, legRegion, region, iconRegion;

    public Mech(String name, boolean flying){
        super(name);
        this.flying = flying;
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayMech(table, this);
    }

    @Override
    public TextureRegion getContentIcon(){
        return iconRegion;
    }

    @Override
    public String getContentName(){
        return name;
    }

    @Override
    public String getContentTypeName(){
        return "mech";
    }

    @Override
    public void load(){
        if(!flying){
            legRegion = Draw.region(name + "-leg");
            baseRegion = Draw.region(name + "-base");
        }

        region = Draw.region(name);
        iconRegion = Draw.region("mech-icon-" + name);
    }
}
