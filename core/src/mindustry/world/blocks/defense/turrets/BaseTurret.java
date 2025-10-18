package mindustry.world.blocks.defense.turrets;

import arc.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class BaseTurret extends Block{
    public float range = 80f;
    public float placeOverlapMargin = 8 * 7f;
    public float rotateSpeed = 5;
    public float fogRadiusMultiplier = 1f;
    public boolean disableOverlapCheck = false;
    /** How much time to start shooting after placement. */
    public float activationTime = 0f;
    /** Color of inactive region drawn on top (if found) */
    public Color inactiveColor = Color.white;

    /** Effect displayed when coolant is used. */
    public Effect coolEffect = Fx.fuelburn;
    /** How much reload is lowered by for each unit of liquid of heat capacity. */
    public float coolantMultiplier = 5f;
    /** If not null, this consumer will be used for coolant. */
    public @Nullable ConsumeLiquidBase coolant;

    public BaseTurret(String name){
        super(name);

        update = true;
        solid = true;
        outlineIcon = true;
        attacks = true;
        priority = TargetPriority.turret;
        group = BlockGroup.turrets;
        flags = EnumSet.of(BlockFlag.turret);
    }

    @Override
    public void init(){
        if(coolant == null){
            coolant = findConsumer(c -> c instanceof ConsumeCoolant);
        }

        //just makes things a little more convenient
        if(coolant != null){
            //TODO coolant fix
            coolant.update = false;
            coolant.booster = true;
            coolant.optional = true;

            //json parsing does not add to consumes
            if(!hasConsumer(coolant)) consume(coolant);
        }

        if(!disableOverlapCheck){
            placeOverlapRange = Math.max(placeOverlapRange, range + placeOverlapMargin);
        }
        fogRadius = Math.max(Mathf.round(range / tilesize * fogRadiusMultiplier), fogRadius);
        super.init();
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);

        if(fogRadiusMultiplier < 0.99f && state.rules.fog){
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * fogRadiusMultiplier, Pal.lightishGray);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.shootRange, range / tilesize, StatUnit.blocks);
        if(activationTime > 0) stats.add(Stat.activationTime, activationTime / 60f, StatUnit.seconds);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(activationTime > 0){
            addBar("activationtimer", (BaseTurretBuild entity) ->
            new Bar(() ->
            (entity.activationTimer > 0)? Core.bundle.format("bar.activationtimer", Mathf.ceil(entity.activationTimer / 60f)) : Core.bundle.get("bar.activated"),
            () -> (entity.activationTimer > 0)?  Pal.lightOrange : Pal.techBlue,
            () -> 1 - entity.activationTimer / activationTime));
        }
    }

    public class BaseTurretBuild extends Building implements Ranged, RotBlock{
        public float rotation = 90;
        public float activationTimer = activationTime;

        @Override
        public float range(){
            return range;
        }

        @Override
        public float buildRotation(){
            return rotation;
        }

        @Override
        public void drawSelect(){
            Drawf.dashCircle(x, y, range(), team.color);
        }

        public float estimateDps(){
            return 0f;
        }

        @Override
        public BlockStatus status() {
            return (activationTimer <= 0)? super.status() : BlockStatus.inactive;
        }
    }
}
