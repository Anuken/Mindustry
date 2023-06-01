package mindustry.ui.fragments;

import arc.*;
import arc.func.*;
import arc.input.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.input.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.payloads.PayloadBlock.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class HintsFragment{
    private static final Boolp isTutorial = () -> Vars.state.rules.sector == SectorPresets.groundZero.sector;
    private static final float foutTime = 0.6f;

    /** All hints to be displayed in the game. */
    public Seq<Hint> hints = new Seq<>().add(DefaultHint.values()).as();

    @Nullable Hint current;
    Group group = new WidgetGroup();
    ObjectSet<String> events = new ObjectSet<>();
    ObjectSet<Block> placedBlocks = new ObjectSet<>();
    Table last;

    public void build(Group parent){
        group.setFillParent(true);
        group.touchable = Touchable.childrenOnly;
        group.visibility = () -> Core.settings.getBool("hints", true) && ui.hudfrag.shown;
        group.update(() -> {
            if(current != null){
                //current got completed
                if(current.complete()){
                    complete();
                }else if(!current.show()){ //current became hidden
                    hide();
                }
            }else if(hints.size > 0){
                //check one hint each frame to see if it should be shown.
                Hint hint = hints.find(Hint::show);
                if(hint != null && hint.complete()){
                    hints.remove(hint);
                }else if(hint != null && !renderer.isCutscene() && state.isGame() && control.saves.getTotalPlaytime() > 8000){
                    display(hint);
                }else{
                    //moused over a derelict structure
                    var build = world.buildWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
                    if(build != null && build.team == Team.derelict){
                        events.add("derelictmouse");
                    }
                }
            }
        });
        parent.addChild(group);

        checkNext();

        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking && event.unit == player.unit()){
                placedBlocks.add(event.tile.block());
            }

            if(event.breaking){
                events.add("break");
            }
        });

        Events.on(ResetEvent.class, e -> {
            placedBlocks.clear();
            events.clear();
        });

        Events.on(BuildingCommandEvent.class, e -> {
            if(e.building instanceof PayloadBlockBuild<?>){
                events.add("factorycontrol");
            }
        });
    }

    void checkNext(){
        if(current != null) return;

        hints.removeAll(h -> !h.valid() || h.finished() || (h.show() && h.complete()));
        hints.sort(Hint::order);

        Hint first = hints.find(Hint::show);
        if(first != null && !renderer.isCutscene() && state.isGame()){
            hints.remove(first);
            display(first);
        }
    }

    void display(Hint hint){
        if(current != null) return;

        group.fill(t -> {
            last = t;
            t.left();
            t.table(Styles.black5, cont -> {
                cont.actions(Actions.alpha(0f), Actions.alpha(1f, 1f, Interp.smooth));
                cont.margin(6f).add(hint.text()).width(Vars.mobile ? 270f : 400f).left().labelAlign(Align.left).wrap();
            });
            t.row();
            t.button("@hint.skip", Styles.nonet, () -> {
                if(current != null){
                    complete();
                }
            }).size(112f, 40f).left();
        });

        this.current = hint;
    }

    /** Completes and hides the current hint. */
    void complete(){
        if(current == null) return;

        current.finish();
        hints.remove(current);

        hide();
    }

    /** Hides the current hint, but does not complete it. */
    void hide(){
        //hide previous child if found
        if(last != null){
            last.actions(Actions.parallel(Actions.alpha(0f, foutTime, Interp.smooth), Actions.translateBy(0f, Scl.scl(-200f), foutTime, Interp.smooth)), Actions.remove());
        }
        //check for next hint to display immediately
        current = null;
        last = null;
        checkNext();
    }

    public boolean shown(){
        return current != null;
    }

    static boolean isSerpulo(){
        return !state.rules.hasEnv(Env.scorching);
    }

    public enum DefaultHint implements Hint{
        desktopMove(visibleDesktop, () -> Core.input.axis(Binding.move_x) != 0 || Core.input.axis(Binding.move_y) != 0),
        zoom(visibleDesktop, () -> Core.input.axis(KeyCode.scroll) != 0),
        breaking(() -> isTutorial.get() && state.rules.defaultTeam.data().getCount(Blocks.conveyor) > 5, () -> ui.hints.events.contains("break")),
        desktopShoot(visibleDesktop, () -> isSerpulo() && Vars.state.enemies > 0, () -> player.shooting),
        depositItems(() -> player.unit().hasItem(), () -> !player.unit().hasItem()),
        desktopPause(visibleDesktop, () -> isTutorial.get() && !Vars.net.active() && state.wave >= 2, () -> Core.input.keyTap(Binding.pause)),
        unitControl(() -> isSerpulo() && state.rules.defaultTeam.data().units.size > 2 && !net.active() && !player.dead(), () -> !player.dead() && !player.unit().spawnedByCore),
        unitSelectControl(() -> isSerpulo() && state.rules.defaultTeam.data().units.size > 3 && !net.active() && !player.dead(), () -> control.input.commandMode && control.input.selectedUnits.size > 0),
        respawn(visibleMobile, () -> !player.dead() && !player.unit().spawnedByCore, () -> !player.dead() && player.unit().spawnedByCore),
        launch(() -> isTutorial.get() && state.rules.sector.isCaptured(), () -> ui.planet.isShown()),
        schematicSelect(visibleDesktop, () -> ui.hints.placedBlocks.contains(Blocks.router) || ui.hints.placedBlocks.contains(Blocks.ductRouter), () -> Core.input.keyRelease(Binding.schematic_select) || Core.input.keyTap(Binding.pick)),
        conveyorPathfind(() -> control.input.block == Blocks.titaniumConveyor, () -> Core.input.keyRelease(Binding.diagonal_placement) || (mobile && Core.settings.getBool("swapdiagonal"))),
        boost(visibleDesktop, () -> !player.dead() && player.unit().type.canBoost, () -> Core.input.keyDown(Binding.boost)),
        blockInfo(() -> !(state.isCampaign() && state.rules.sector == SectorPresets.groundZero.sector && state.wave < 3), () -> ui.content.isShown()),
        derelict(() -> ui.hints.events.contains("derelictmouse") && !isTutorial.get(), () -> false),
        payloadPickup(() -> isSerpulo() && !player.unit().dead && player.unit() instanceof Payloadc p && p.payloads().isEmpty(), () -> player.unit() instanceof Payloadc p && p.payloads().any()),
        payloadDrop(() -> !player.unit().dead && player.unit() instanceof Payloadc p && p.payloads().any(), () -> player.unit() instanceof Payloadc p && p.payloads().isEmpty()),
        waveFire(() -> Groups.fire.size() > 0 && Blocks.wave.unlockedNow(), () -> indexer.getFlagged(state.rules.defaultTeam, BlockFlag.extinguisher).size > 0),
        generator(() -> control.input.block == Blocks.combustionGenerator, () -> ui.hints.placedBlocks.contains(Blocks.combustionGenerator)),
        rebuildSelect(() -> state.rules.defaultTeam.data().plans.size >= 10, () -> control.input.isRebuildSelecting()),
        guardian(() -> state.boss() != null && isSerpulo() && state.boss().armor >= 4, () -> state.boss() == null),
        factoryControl(() -> !(state.isCampaign() && state.rules.sector.preset == SectorPresets.onset) &&
            state.rules.defaultTeam.data().getBuildings(Blocks.tankFabricator).size + state.rules.defaultTeam.data().getBuildings(Blocks.groundFactory).size > 0, () -> ui.hints.events.contains("factorycontrol")),
        coreUpgrade(() -> state.isCampaign() && state.rules.sector.planet == Planets.serpulo && Blocks.coreFoundation.unlocked()
            && state.rules.defaultTeam.core() != null
            && state.rules.defaultTeam.core().block == Blocks.coreShard
            && state.rules.defaultTeam.core().items.has(Blocks.coreFoundation.requirements),
            () -> ui.hints.placedBlocks.contains(Blocks.coreFoundation)),
        presetLaunch(() -> state.isCampaign()
            && state.getSector().preset == null,
            () -> state.isCampaign() && state.getSector().preset == SectorPresets.frozenForest),
        presetDifficulty(() -> state.isCampaign()
            && state.getSector().preset == null
            && state.getSector().threat >= 0.5f
            && !SectorPresets.tarFields.sector.isCaptured(), //appear only when the player hasn't progressed much in the game yet
            () -> state.isCampaign() && state.getSector().preset != null),
        coreIncinerate(() -> state.isCampaign() && state.rules.defaultTeam.core() != null && state.rules.defaultTeam.core().items.get(Items.copper) >= state.rules.defaultTeam.core().storageCapacity - 10, () -> false)
        ;

        @Nullable
        String text;
        int visibility = visibleAll;
        Hint[] dependencies = {};
        boolean finished, cached;
        Boolp complete, shown = () -> true;

        DefaultHint(Boolp complete){
            this.complete = complete;
        }

        DefaultHint(int visiblity, Boolp complete){
            this(complete);
            this.visibility = visiblity;
        }

        DefaultHint(Boolp shown, Boolp complete){
            this(complete);
            this.shown = shown;
        }

        DefaultHint(int visiblity, Boolp shown, Boolp complete){
            this(complete);
            this.shown = shown;
            this.visibility = visiblity;
        }

        @Override
        public boolean finished(){
            if(!cached){
                cached = true;
                finished = Core.settings.getBool(name() + "-hint-done", false);
            }
            return finished;
        }

        @Override
        public void finish(){
            Core.settings.put(name() + "-hint-done", finished = true);
        }

        @Override
        public String text(){
            if(text == null){
                text = Vars.mobile && Core.bundle.has("hint." + name() + ".mobile") ? Core.bundle.get("hint." + name() + ".mobile") : Core.bundle.get("hint." + name());
                if(!Vars.mobile) text = text.replace("tap", "click").replace("Tap", "Click");
            }
            return text;
        }

        @Override
        public boolean complete(){
            return complete.get();
        }

        @Override
        public boolean show(){
            return shown.get() && (dependencies.length == 0 || !Structs.contains(dependencies, d -> !d.finished()));
        }

        @Override
        public int order(){
            return ordinal();
        }

        @Override
        public boolean valid(){
            return (Vars.mobile && (visibility & visibleMobile) != 0) || (!Vars.mobile && (visibility & visibleDesktop) != 0);
        }
    }

    /** Hint interface for defining any sort of message appearing at the left. */
    public interface Hint{
        int visibleDesktop = 1, visibleMobile = 2, visibleAll = visibleDesktop | visibleMobile;

        /** Hint name for preference storage. */
        String name();
        /** Displayed text. */
        String text();
        /** @return true if hint objective is complete */
        boolean complete();
        /** @return whether the hint is ready to be shown */
        boolean show();
        /** @return order integer, determines priority */
        int order();
        /** @return whether this hint should be processed, used for platform splits */
        boolean valid();

        /** finishes the hint - it should not be shown again */
        default void finish(){
            Core.settings.put(name() + "-hint-done", true);
        }

        /** @return whether the hint is finished - if true, it should not be shown again */
        default boolean finished(){
            return Core.settings.getBool(name() + "-hint-done", false);
        }
    }
}
