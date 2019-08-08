package io.anuke.mindustry.game;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.world.*;

/** Handles tutorial state. */
public class Tutorial{
    private static final int mineCopper = 16;

    private ObjectSet<String> events = new ObjectSet<>();
    private ObjectIntMap<Block> blocksPlaced = new ObjectIntMap<>();
    public TutorialStage stage = TutorialStage.values()[0];

    public Tutorial(){
        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking){
                blocksPlaced.getAndIncrement(event.tile.block(), 0, 1);
            }
        });

        Events.on(LineConfirmEvent.class, event -> events.add("lineconfirm"));
        Events.on(AmmoDeliverEvent.class, event -> events.add("ammo"));
    }

    /** update tutorial state, transition if needed */
    public void update(){
        if(stage.done.get()){
            next();
        }else{
            stage.update();
        }
    }

    /** draw UI overlay */
    public void draw(){
        stage.draw();
    }

    /** Resets tutorial state. */
    public void reset(){
        stage = TutorialStage.values()[0];
        blocksPlaced.clear();
        events.clear();
    }

    /** Goes on to the next tutorial step. */
    public void next(){
        stage = TutorialStage.values()[Mathf.clamp(stage.ordinal() + 1, 0, TutorialStage.values().length)];
        blocksPlaced.clear();
        events.clear();
    }

    public enum TutorialStage{
        intro(
        line -> Core.bundle.format(line, item(Items.copper), mineCopper),
        () -> item(Items.copper) >= mineCopper
        ),
        drill(() -> placed(Blocks.mechanicalDrill, 1)){
            void draw(){
                outline("category-production");
                outline("block-mechanical-drill");
            }
        },
        conveyor(
        line -> Core.bundle.format(line, placed(Blocks.conveyor), 3),
        () -> placed(Blocks.conveyor, 3) && event("lineconfirm")){
            void draw(){
                outline("category-distribution");
                outline("block-conveyor");
            }
        },
        turret(() -> placed(Blocks.duo, 1)){
            void draw(){
                outline("category-turrets");
                outline("block-duo");
            }
        },
        drillturret(() -> event("ammo")){
            void draw(){
                outline("category-production");
                outline("block-mechanical-drill");
            }
        },
        waves(() -> Vars.state.wave > 2 && Vars.state.enemies() <= 0){
            void begin(){
                Vars.state.rules.waveTimer = true;
            }

            void update(){
                if(Vars.state.wave > 2){
                    Vars.state.rules.waveTimer = false;
                }
            }
        };

        protected final String line = Core.bundle.has("tutorial." + name() + ".mobile") && Vars.mobile ? "tutorial." + name() + ".mobile" : "tutorial." + name();
        protected final Function<String, String> text;
        protected final BooleanProvider done;

        TutorialStage(Function<String, String> text, BooleanProvider done){
            this.text = text;
            this.done = done;
        }

        TutorialStage(BooleanProvider done){
            this.text = line -> Core.bundle.get(line);
            this.done = done;
        }

        /** displayed tutorial stage text.*/
        public String text(){
            return text.get(line);
        }

        /** called every frame when this stage is active.*/
        void update(){

        }

        /** called when a stage begins.*/
        void begin(){

        }

        /** called when a stage needs to draw itself, usually over highlighted UI elements. */
        void draw(){

        }

        //utility

        static boolean event(String name){
            return Vars.control.tutorial.events.contains(name);
        }

        static boolean placed(Block block, int amount){
            return placed(block) >= amount;
        }

        static int placed(Block block){
            return Vars.control.tutorial.blocksPlaced.get(block, 0);
        }

        static int item(Item item){
            return Vars.state.teams.get(Vars.defaultTeam).cores.isEmpty() ? 0 : Vars.state.teams.get(Vars.defaultTeam).cores.first().entity.items.get(item);
        }

        static boolean toggled(String name){
            Element element = Core.scene.findVisible(name);
            if(element instanceof Button){
                return ((Button)element).isChecked();
            }
            return false;
        }

        static void outline(String name){
            Element element = Core.scene.findVisible(name);
            if(element != null && !toggled(name)){
                element.localToStageCoordinates(Tmp.v1.setZero());
                float sin = Mathf.sin(11f, UnitScl.dp.scl(4f));
                Lines.stroke(UnitScl.dp.scl(7f), Pal.place);
                Lines.rect(Tmp.v1.x - sin, Tmp.v1.y - sin, element.getWidth() + sin*2, element.getHeight() + sin*2);
                Draw.reset();
            }
        }
    }

}
