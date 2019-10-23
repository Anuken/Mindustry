package io.anuke.mindustry.type;

import io.anuke.arc.Core;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.ArcAnnotate.*;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.ctype.UnlockableContent;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.ui.ContentDisplay;

public class Mech extends UnlockableContent{
    public boolean flying;
    public float speed = 1.1f;
    public float maxSpeed = 10f;
    public float boostSpeed = 0.75f;
    public float drag = 0.4f;
    public float mass = 1f;
    public float shake = 0f;
    public float health = 200f;

    public float hitsize = 6f;
    public float cellTrnsY = 0f;
    public float mineSpeed = 1f;
    public int drillPower = -1;
    public float buildPower = 1f;
    public Color engineColor = Pal.boostTo;
    public int itemCapacity = 30;
    public boolean turnCursor = true;
    public boolean canHeal = false;
    public float compoundSpeed, compoundSpeedBoost;

    public float weaponOffsetX, weaponOffsetY, engineOffset = 5f, engineSize = 2.5f;
    public @NonNull Weapon weapon;

    public TextureRegion baseRegion, legRegion, region;

    public Mech(String name, boolean flying){
        super(name);
        this.flying = flying;
        this.description = Core.bundle.get("mech." + name + ".description");
    }

    public Mech(String name){
        this(name, false);
    }

    public String localizedName(){
        return Core.bundle.get("mech." + name + ".name");
    }

    public void updateAlt(Player player){
    }

    public void draw(Player player){
    }

    public float getExtraArmor(Player player){
        return 0f;
    }

    public float spreadX(Player player){
        return 0f;
    }

    public float getRotationAlpha(Player player){
        return 1f;
    }

    public boolean canShoot(Player player){
        return true;
    }

    public void onLand(Player player){
    }

    @Override
    public void init(){
        super.init();

        for(int i = 0; i < 500; i++){
            compoundSpeed *= (1f - drag);
            compoundSpeed += speed;
        }

        for(int i = 0; i < 500; i++){
            compoundSpeedBoost *= (1f - drag);
            compoundSpeedBoost += boostSpeed;
        }
    }

    @Override
    public void displayInfo(Table table){
        ContentDisplay.displayMech(table, this);
    }

    @Override
    public ContentType getContentType(){
        return ContentType.mech;
    }

    @Override
    public void load(){
        weapon.load();
        if(!flying){
            legRegion = Core.atlas.find(name + "-leg");
            baseRegion = Core.atlas.find(name + "-base");
        }

        region = Core.atlas.find(name);
    }

    @Override
    public String toString(){
        return localizedName();
    }
}
