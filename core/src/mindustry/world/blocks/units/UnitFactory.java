package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.style.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.ArcAnnotate.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.annotations.Annotations.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class UnitFactory extends Block{
    public float payloadSpeed = 0.5f;
    public @Load(value = "@-top", fallback = "factory-top") TextureRegion topRegion;
    public @Load(value = "@-out", fallback = "factory-out") TextureRegion outRegion;
    public int[] capacities;

    public UnitPlan[] plans = new UnitPlan[0];

    public UnitFactory(String name){
        super(name);
        update = true;
        hasPower = true;
        hasItems = true;
        solid = true;
        flags = EnumSet.of(BlockFlag.producer, BlockFlag.unitModifier);
        unitCapModifier = 4;
        configurable = true;
        outputsPayload = true;
        rotate = true;

        config(Integer.class, (tile, i) -> {
            ((UnitFactoryEntity)tile).currentPlan = i < 0 || i >= plans.length ? -1 : i;
            ((UnitFactoryEntity)tile).progress = 0;
        });

        consumes.add(new ConsumeItemDynamic(e -> {
            UnitFactoryEntity entity = (UnitFactoryEntity)e;

            if(entity.currentPlan != -1){
                return plans[entity.currentPlan].requirements;
            }

            return ItemStack.empty;
        }));
    }

    @Remote(called = Loc.server)
    public static void onUnitFactorySpawn(Tile tile){
        if(!(tile.entity instanceof UnitFactoryEntity)) return;
        tile.<UnitFactoryEntity>ent().spawned();
    }

    @Override
    public void init(){
        super.init();

        capacities = new int[Vars.content.items().size];
        for(UnitPlan plan : plans){
            for(ItemStack stack : plan.requirements){
                capacities[stack.item.id] = Math.max(capacities[stack.item.id], stack.amount * 2);
            }
        }
    }

    @Override
    public void setBars(){
        super.setBars();
        bars.add("progress", entity -> new Bar("bar.progress", Pal.ammo, ((UnitFactoryEntity)entity)::fraction));
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(BlockStat.itemCapacity);
    }

    @Override
    public TextureRegion[] generateIcons(){
        return new TextureRegion[]{Core.atlas.find(name), Core.atlas.find(name + "-out"), Core.atlas.find(name + "-top")};
    }

    @Override
    public void drawRequestRegion(BuildRequest req, Eachable<BuildRequest> list){
        Draw.rect(region, req.drawx(), req.drawy(), region.getWidth() * req.animScale * Draw.scl, region.getHeight() * req.animScale * Draw.scl);
        Draw.rect(outRegion, req.drawx(), req.drawy(), outRegion.getWidth() * req.animScale * Draw.scl, outRegion.getHeight() * req.animScale * Draw.scl, req.rotation * 90);
        Draw.rect(topRegion, req.drawx(), req.drawy(), outRegion.getWidth() * req.animScale * Draw.scl, outRegion.getHeight() * req.animScale * Draw.scl);
    }

    public static class UnitPlan{
        public UnitType unit;
        public ItemStack[] requirements;
        public float time;

        public UnitPlan(UnitType unit, float time, ItemStack[] requirements){
            this.unit = unit;
            this.time = time;
            this.requirements = requirements;
        }

        UnitPlan(){}
    }

    public class UnitFactoryEntity extends TileEntity{
        public int currentPlan = -1;
        public float progress, time, speedScl, payloadPos;
        public @Nullable UnitPayload payload;

        public float fraction(){
            return currentPlan == -1 ? 0 : progress / plans[currentPlan].time;
        }

        @Override
        public void buildConfiguration(Table table){
            Array<UnitType> units = Array.with(plans).map(u -> u.unit);

            ItemSelection.buildTable(table, units, () -> currentPlan == -1 ? null : plans[currentPlan].unit, unit -> configure(units.indexOf(unit)));
        }

        @Override
        public void display(Table table){
            super.display(table);

            TextureRegionDrawable reg = new TextureRegionDrawable();

            table.row();
            table.table(t -> {
                t.image().update(i -> {
                    i.setDrawable(currentPlan == -1 ? Icon.cancel : reg.set(plans[currentPlan].unit.icon(Cicon.medium)));
                    i.setScaling(Scaling.fit);
                    i.setColor(currentPlan == -1 ? Color.lightGray : Color.white);
                }).size(32).padBottom(-4).padRight(2);
                t.label(() -> currentPlan == -1 ? "$none" : plans[currentPlan].unit.localizedName).color(Color.lightGray);
            });
        }

        @Override
        public Object config(){
            return currentPlan;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotation() * 90);

            if(currentPlan != -1){
                UnitPlan plan = plans[currentPlan];

                Draw.draw(Layer.blockOver, () -> {
                    TextureRegion region = plan.unit.icon(Cicon.full);

                    Shaders.build.region = region;
                    Shaders.build.progress = progress / plan.time;
                    Shaders.build.color.set(Pal.accent);
                    Shaders.build.color.a = speedScl;
                    Shaders.build.time = -time / 20f;

                    Draw.shader(Shaders.build);
                    Draw.rect(region, x, y, rotation() * 90 - 90);
                    Draw.shader();

                    Draw.color(Pal.accent);
                    Draw.alpha(speedScl);

                    Lines.lineAngleCenter(x + Mathf.sin(time, 20f, Vars.tilesize / 2f * size - 2f), y, 90, size * Vars.tilesize - 4f);

                    Draw.reset();
                });
            }

            Draw.z(Layer.blockOver);

            if(payload != null){
                payload.draw(
                    x + Angles.trnsx(rotation() * 90, payloadPos),
                    y + Angles.trnsy(rotation() * 90, payloadPos),
                    rotation() * 90
                );
            }

            Draw.z(Layer.blockOver + 0.1f);

            Draw.rect(topRegion, x, y);
        }

        public void spawned(){
            progress = 0f;

            if(!net.client() && payload != null){
                Unitc unit = payload.unit;
                unit.set(x, y);

                unit.rotation(rotation() * 90);
                unit.vel().trns(rotation() * 90, payloadSpeed * 2f).add(Mathf.range(0.1f), Mathf.range(0.1f));
                unit.trns(Tmp.v1.trns(rotation() * 90, size * tilesize/2f));
                unit.trns(unit.vel());
                unit.add();
                Events.fire(new UnitCreateEvent(unit));
            }

            payload = null;
        }

        @Override
        public void updateTile(){
            if(currentPlan < 0 || currentPlan >= plans.length){
                currentPlan = -1;
            }

            if((consValid() || tile.isEnemyCheat()) && currentPlan != -1){
                time += delta() * efficiency() * speedScl * Vars.state.rules.unitBuildSpeedMultiplier;
                progress += delta() * efficiency() * Vars.state.rules.unitBuildSpeedMultiplier;
                speedScl = Mathf.lerpDelta(speedScl, 1f, 0.05f);
            }else{
                speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);
            }

            if(payload != null){
                payloadPos += edelta() * payloadSpeed;
                if(payloadPos >= size * tilesize/2f){
                    payloadPos = size * tilesize/2f;

                    Tile front = frontLarge();
                    if(front != null && front.entity != null && front.block().outputsPayload){
                        if(movePayload(payload)){
                            payload = null;
                        }
                    }else if(front != null && !front.solid()){
                        //create unit
                        Call.onUnitFactorySpawn(tile);
                    }
                }
            }

            if(currentPlan != -1 && payload == null){
                UnitPlan plan = plans[currentPlan];

                if(progress >= plan.time && Units.canCreate(team)){
                    progress = 0f;

                    payloadPos = 0f;
                    payload = new UnitPayload(plan.unit.create(team));
                    consume();
                }

                progress = Mathf.clamp(progress, 0, plan.time);
            }else{
                progress = 0f;
            }
        }

        @Override
        public int getMaximumAccepted(Item item){
            return capacities[item.id];
        }

        @Override
        public boolean acceptItem(Tilec source, Item item){
            return currentPlan != -1 && items.get(item) < getMaximumAccepted(item) &&
                Structs.contains(plans[currentPlan].requirements, stack -> stack.item == item);
        }

        @Override
        public byte version(){
            return 1;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.s(currentPlan);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            currentPlan = read.s();
        }
    }
}
