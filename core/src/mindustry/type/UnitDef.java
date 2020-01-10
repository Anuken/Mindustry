package mindustry.type;

import arc.audio.*;
import arc.graphics.*;
import arc.struct.*;
import mindustry.ctype.*;
import mindustry.gen.*;
import mindustry.graphics.*;

public abstract class UnitDef extends UnlockableContent{
    public boolean flying;
    public float speed = 1.1f, boostSpeed = 0.75f;
    public float drag = 0.4f, mass = 1f;
    public float health = 200f;

    public int itemCapacity = 30;
    public int drillTier = -1;
    public float buildPower = 1f, minePower = 1f;

    public Color engineColor = Pal.boostTo;
    public float engineOffset = 5f, engineSize = 2.5f;

    public float hitsize = 6f, hitsizeTile = 4f;
    public float cellOffsetX = 0f, cellOffsetY = 0f;
    public float lightRadius = 60f;
    public Color lightColor = Pal.powerLight;
    public boolean drawCell = true, drawItems = true;

    public ObjectSet<StatusEffect> immunities = new ObjectSet<>();
    public Sound deathSound = Sounds.bang;

    public Array<Weapon> weapons = new Array<>();

    public UnitDef(String name){
        super(name);
    }
}
