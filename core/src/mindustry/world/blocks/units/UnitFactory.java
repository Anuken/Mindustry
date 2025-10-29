package mindustry.world.blocks.units;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ai.*;
import mindustry.ctype.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.io.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.payloads.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class UnitFactory extends UnitBlock{
    public int[] capacities = {};

    public Seq<UnitPlan> plans = new Seq<>(4);

    public UnitFactory(String name){
        super(name);
        update = true;
        hasPower = true;
        hasItems = true;
        solid = true;
        configurable = true;
        clearOnDoubleTap = true;
        outputsPayload = true;
        rotate = true;
        regionRotated1 = 1;
        commandable = true;
        ambientSound = Sounds.respawning;

        config(Integer.class, (UnitFactoryBuild build, Integer i) -> {
            if(!configurable) return;

            if(build.currentPlan == i) return;
            build.currentPlan = i < 0 || i >= plans.size ? -1 : i;
            build.progress = 0;
            if(build.command != null && (build.unit() == null || !build.unit().commands.contains(build.command))){
                build.command = null;
            }
        });

        config(UnitType.class, (UnitFactoryBuild build, UnitType val) -> {
            if(!configurable) return;

            int next = plans.indexOf(p -> p.unit == val);
            if(build.currentPlan == next) return;
            build.currentPlan = next;
            build.progress = 0;
            if(build.command != null && !val.commands.contains(build.command)){
                build.command = null;
            }
        });

        config(UnitCommand.class, (UnitFactoryBuild build, UnitCommand command) -> build.command = command);
        configClear((UnitFactoryBuild build) -> build.command = null);

        consume(new ConsumeItemDynamic((UnitFactoryBuild e) -> e.currentPlan != -1 ? plans.get(Math.min(e.currentPlan, plans.size - 1)).requirements : ItemStack.empty));
    }

    @Override
    public void init(){
        initCapacities();
        super.init();
    }

    @Override
    public void afterPatch(){
        initCapacities();
        super.afterPatch();
    }

    public void initCapacities(){
        capacities = new int[Vars.content.items().size];
        itemCapacity = 10; //unit factories can't control their own capacity externally, setting the value does nothing
        for(UnitPlan plan : plans){
            for(ItemStack stack : plan.requirements){
                capacities[stack.item.id] = Math.max(capacities[stack.item.id], stack.amount * 2);
                itemCapacity = Math.max(itemCapacity, stack.amount * 2);
            }
        }

        consumeBuilder.each(c -> c.multiplier = b -> state.rules.unitCost(b.team));
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("progress", (UnitFactoryBuild e) -> new Bar("bar.progress", Pal.ammo, e::fraction));

        addBar("units", (UnitFactoryBuild e) ->
        new Bar(
            () -> e.unit() == null ? "[lightgray]" + Iconc.cancel :
                Core.bundle.format("bar.unitcap",
                    Fonts.getUnicodeStr(e.unit().name),
                    e.team.data().countType(e.unit()),
                    e.unit() == null ? Units.getStringCap(e.team) : (e.unit().useUnitCap ? Units.getStringCap(e.team) : "∞")
                ),
            () -> Pal.power,
            () -> e.unit() == null ? 0f : (e.unit().useUnitCap ? (float)e.team.data().countType(e.unit()) / Units.getCap(e.team) : 1f)
        ));
    }

    @Override
    public boolean outputsItems(){
        return false;
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.remove(Stat.itemCapacity);

        stats.add(Stat.output, table -> {
            table.row();

            for(var plan : plans){
                table.table(Styles.grayPanel, t -> {

                    if(plan.unit.isBanned()){
                        t.image(Icon.cancel).color(Pal.remove).size(40);
                        return;
                    }

                    if(plan.unit.unlockedNow()){
                        t.image(plan.unit.uiIcon).size(40).pad(10f).left().scaling(Scaling.fit).with(i -> StatValues.withTooltip(i, plan.unit));
                        t.table(info -> {
                            info.add(plan.unit.localizedName).left();
                            info.row();
                            info.add(Strings.autoFixed(plan.time / 60f, 1) + " " + Core.bundle.get("unit.seconds")).color(Color.lightGray);
                        }).left();

                        t.table(req -> {
                            req.right();
                            for(int i = 0; i < plan.requirements.length; i++){
                                if(i % 6 == 0){
                                    req.row();
                                }

                                ItemStack stack = plan.requirements[i];
                                req.add(StatValues.displayItem(stack.item, stack.amount, plan.time, true)).pad(5);
                            }
                        }).right().grow().pad(10f);
                    }else{
                        t.image(Icon.lock).color(Pal.darkerGray).size(40);
                    }
                }).growX().pad(5);
                table.row();
            }
        });
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{region, outRegion, topRegion};
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        Draw.rect(region, plan.drawx(), plan.drawy());
        Draw.rect(outRegion, plan.drawx(), plan.drawy(), plan.rotation * 90);
        Draw.rect(topRegion, plan.drawx(), plan.drawy());
    }

    @Override
    public void getPlanConfigs(Seq<UnlockableContent> options){
        for(var plan : plans){
            if(!plan.unit.isBanned()){
                options.add(plan.unit);
            }
        }
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

    public class UnitFactoryBuild extends UnitBuild{
        public @Nullable Vec2 commandPos;
        public @Nullable UnitCommand command;
        public int currentPlan = -1;

        public float fraction(){
            return currentPlan == -1 ? 0 : progress / plans.get(currentPlan).time;
        }

        public boolean canSetCommand(){
            var output = unit();
            return output != null && output.commands.size > 1 && output.allowChangeCommands &&
                //to avoid cluttering UI, don't show command selection for "standard" units that only have two commands.
                !(output.commands.size == 2 && output.commands.get(1) == UnitCommand.enterPayloadCommand);
        }

        @Override
        public void created(){
            //auto-set to the first plan, it's better than nothing.
            if(currentPlan == -1){
                currentPlan = plans.indexOf(u -> u.unit.unlockedNow());
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            if(plans.size > 1 && currentPlan != -1 && currentPlan < plans.size){
                drawItemSelection(plans.get(currentPlan).unit);
            }
        }

        @Override
        public Vec2 getCommandPosition(){
            return commandPos;
        }

        @Override
        public void onCommand(Vec2 target){
            commandPos = target;
        }

        @Override
        public Object senseObject(LAccess sensor){
            if(sensor == LAccess.config) return currentPlan == -1 ? null : plans.get(currentPlan).unit;
            return super.senseObject(sensor);
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.progress) return Mathf.clamp(fraction());
            if(sensor == LAccess.itemCapacity) return Mathf.round(itemCapacity * state.rules.unitCost(team));
            return super.sense(sensor);
        }

        @Override
        public void buildConfiguration(Table table){
            Seq<UnitType> units = Seq.with(plans).map(u -> u.unit).retainAll(u -> u.unlockedNow() && !u.isBanned());

            if(units.any()){
                ItemSelection.buildTable(UnitFactory.this, table, units, () -> currentPlan == -1 ? null : plans.get(currentPlan).unit, unit -> configure(plans.indexOf(u -> u.unit == unit)), selectionRows, selectionColumns);

                table.row();

                Table commands = new Table();
                commands.top().left();

                Runnable rebuildCommands = () -> {
                    commands.clear();
                    commands.background(null);
                    var unit = unit();
                    if(unit != null && canSetCommand()){
                        commands.background(Styles.black6);
                        var group = new ButtonGroup<ImageButton>();
                        group.setMinCheckCount(0);
                        int i = 0, columns = Mathf.clamp(units.size, 2, selectionColumns);
                        var list = unit.commands;

                        commands.image(Tex.whiteui, Pal.gray).height(4f).growX().colspan(columns).row();

                        for(var item : list){
                            ImageButton button = commands.button(item.getIcon(), Styles.clearNoneTogglei, 40f, () -> {
                                configure(item);
                            }).tooltip(item.localized()).group(group).get();

                            button.update(() -> button.setChecked(command == item || (command == null && unit.defaultCommand == item)));

                            if(++i % columns == 0){
                                commands.row();
                            }
                        }

                        if(list.size < columns){
                            for(int j = 0; j < (columns - list.size); j++){
                                commands.add().size(40f);
                            }
                        }
                    }
                };

                rebuildCommands.run();

                //Since the menu gets hidden when a new unit is selected, this is unnecessary.
                /*
                UnitType[] lastUnit = {unit()};

                commands.update(() -> {
                    if(lastUnit[0] != unit()){
                        lastUnit[0] = unit();
                        rebuildCommands.run();
                    }
                });*/

                table.row();

                table.add(commands).fillX().left();

            }else{
                table.table(Styles.black3, t -> t.add("@none").color(Color.lightGray));
            }
        }

        @Override
        public boolean acceptPayload(Building source, Payload payload){
            return false;
        }

        @Override
        public void display(Table table){
            super.display(table);

            TextureRegionDrawable reg = new TextureRegionDrawable();

            table.row();
            table.table(t -> {
                t.left();
                t.image().update(i -> {
                    i.setDrawable(currentPlan == -1 ? Icon.cancel : reg.set(plans.get(currentPlan).unit.uiIcon));
                    i.setScaling(Scaling.fit);
                    i.setColor(currentPlan == -1 ? Color.lightGray : Color.white);
                }).size(32).padBottom(-4).padRight(2);
                t.label(() -> currentPlan == -1 ? "@none" : plans.get(currentPlan).unit.localizedName).wrap().width(230f).color(Color.lightGray);
            }).left();
        }

        @Override
        public Object config(){
            return currentPlan;
        }

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            Draw.rect(outRegion, x, y, rotdeg());

            if(currentPlan != -1){
                UnitPlan plan = plans.get(currentPlan);
                Draw.draw(Layer.blockOver, () -> Drawf.construct(this, plan.unit, rotdeg() - 90f, progress / plan.time, speedScl, time));
            }

            Draw.z(Layer.blockOver);

            payRotation = rotdeg();
            drawPayload();

            Draw.z(Layer.blockOver + 0.1f);

            Draw.rect(topRegion, x, y);
        }

        @Override
        public void updateTile(){
            if(!configurable){
                currentPlan = 0;
            }

            if(currentPlan < 0 || currentPlan >= plans.size){
                currentPlan = -1;
            }

            if(efficiency > 0 && currentPlan != -1){
                time += edelta() * speedScl * Vars.state.rules.unitBuildSpeed(team);
                progress += edelta() * Vars.state.rules.unitBuildSpeed(team);
                speedScl = Mathf.lerpDelta(speedScl, 1f, 0.05f);
            }else{
                speedScl = Mathf.lerpDelta(speedScl, 0f, 0.05f);
            }

            moveOutPayload();

            if(currentPlan != -1 && payload == null){
                UnitPlan plan = plans.get(currentPlan);

                //make sure to reset plan when the unit got banned after placement
                if(plan.unit.isBanned()){
                    currentPlan = -1;
                    return;
                }

                if(progress >= plan.time){
                    progress %= 1f;

                    Unit unit = plan.unit.create(team);
                    if(unit.isCommandable()){
                        if(commandPos != null){
                            unit.command().commandPosition(commandPos);
                        }

                        unit.command().command(command == null && unit.type.defaultCommand != null ? unit.type.defaultCommand : command);
                    }

                    payload = new UnitPayload(unit);
                    payVector.setZero();
                    consume();
                    Events.fire(new UnitCreateEvent(payload.unit, this));
                }

                progress = Mathf.clamp(progress, 0, plan.time);
            }else{
                progress = 0f;
            }
        }

        @Override
        public boolean shouldConsume(){
            if(currentPlan == -1) return false;
            return enabled && payload == null;
        }

        @Override
        public int getMaximumAccepted(Item item){
            return Mathf.round(capacities[item.id] * state.rules.unitCost(team));
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            return currentPlan != -1 && items.get(item) < getMaximumAccepted(item) &&
                Structs.contains(plans.get(currentPlan).requirements, stack -> stack.item == item);
        }

        public @Nullable UnitType unit(){
            return currentPlan == - 1 ? null : plans.get(currentPlan).unit;
        }

        @Override
        public byte version(){
            return 3;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(progress);
            write.s(currentPlan);
            TypeIO.writeVecNullable(write, commandPos);
            TypeIO.writeCommand(write, command);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            progress = read.f();
            currentPlan = read.s();
            if(revision >= 2){
                commandPos = TypeIO.readVecNullable(read);
            }

            if(revision >= 3){
                command = TypeIO.readCommand(read);
            }
        }
    }
}
