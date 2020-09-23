package mindustry.game;

import arc.*;
import arc.struct.*;
import arc.func.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.game.EventType.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.*;

import static mindustry.Vars.*;

/** Handles tutorial state. */
public class Tutorial{
    private static final int mineCopper = 18;
    private static final int blocksToBreak = 3, blockOffset = -6;

    ObjectSet<String> events = new ObjectSet<>();
    ObjectIntMap<Block> blocksPlaced = new ObjectIntMap<>();
    int sentence;
    public TutorialStage stage = TutorialStage.values()[0];

    public Tutorial(){
        Events.on(BlockBuildEndEvent.class, event -> {
            if(!event.breaking){
                blocksPlaced.increment(event.tile.block(), 1);
            }
        });

        Events.on(LineConfirmEvent.class, event -> events.add("lineconfirm"));
        Events.on(TurretAmmoDeliverEvent.class, event -> events.add("ammo"));
        Events.on(CoreItemDeliverEvent.class, event -> events.add("coreitem"));
        Events.on(BlockInfoEvent.class, event -> events.add("blockinfo"));
        Events.on(DepositEvent.class, event -> events.add("deposit"));
        Events.on(WithdrawEvent.class, event -> events.add("withdraw"));

        Events.on(ClientLoadEvent.class, e -> {
            for(TutorialStage stage : TutorialStage.values()){
                stage.load();
            }
        });
    }

    /** update tutorial state, transition if needed */
    public void update(){
        if(stage.done.get() && !canNext()){
            next();
        }else{
            stage.update();
        }
    }

    /** draw UI overlay */
    public void draw(){
        if(!Core.scene.hasDialog()){
            stage.draw();
        }
    }

    /** Resets tutorial state. */
    public void reset(){
        stage = TutorialStage.values()[0];
        stage.begin();
        blocksPlaced.clear();
        events.clear();
        sentence = 0;
    }

    /** Goes on to the next tutorial step. */
    public void next(){
        stage = TutorialStage.values()[Mathf.clamp(stage.ordinal() + 1, 0, TutorialStage.values().length)];
        stage.begin();
        blocksPlaced.clear();
        events.clear();
        sentence = 0;
    }

    public boolean canNext(){
        return sentence + 1 < stage.sentences.size;
    }

    public void nextSentence(){
        if(canNext()){
            sentence ++;
        }
    }

    public boolean canPrev(){
        return sentence > 0;
    }

    public void prevSentence(){
        if(canPrev()){
            sentence --;
        }
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
                outline("confirmplace");
            }
        },
        blockinfo(() -> event("blockinfo")){
            void draw(){
                outline("category-production");
                outline("block-mechanical-drill");
                outline("blockinfo");
            }
        },
        conveyor(() -> placed(Blocks.conveyor, 2) && event("lineconfirm") && event("coreitem")){
            void draw(){
                outline("category-distribution");
                outline("block-conveyor");
            }
        },
        turret(() -> placed(Blocks.duo, 1)){
            void draw(){
                outline("category-turret");
                outline("block-duo");
            }
        },
        drillturret(() -> event("ammo")),
        pause(() -> state.isPaused()){
            void draw(){
                if(mobile){
                    outline("pause");
                }
            }
        },
        unpause(() -> !state.isPaused()){
            void draw(){
                if(mobile){
                    outline("pause");
                }
            }
        },
        breaking(TutorialStage::blocksBroken){
            void begin(){
                placeBlocks();
            }

            void draw(){
                if(mobile){
                    outline("breakmode");
                }
            }
        },
        withdraw(() -> event("withdraw")){
            void begin(){
                state.teams.playerCores().first().items.add(Items.copper, 10);
            }
        },
        deposit(() -> event("deposit")),
        waves(() -> state.wave > 2 && state.enemies <= 0 && !spawner.isSpawning()){
            void begin(){
                state.rules.waveTimer = true;
                logic.runWave();
            }

            void update(){
                if(state.wave > 2){
                    state.rules.waveTimer = false;
                }
            }
        },
        launch(() -> false){
            void begin(){
                state.rules.waveTimer = false;
                state.wave = 5;

                //end tutorial, never show it again
                Events.fire(Trigger.tutorialComplete);
                Core.settings.put("playedtutorial", true);
            }

            void draw(){
                outline("waves");
            }
        },;

        protected String line = "";
        protected final Func<String, String> text;
        protected Seq<String> sentences;
        protected final Boolp done;

        TutorialStage(Func<String, String> text, Boolp done){
            this.text = text;
            this.done = done;
        }

        TutorialStage(Boolp done){
            this(line -> line, done);
        }

        /** displayed tutorial stage text.*/
        public String text(){
            if(sentences == null){
               load();
            }
            String line = sentences.get(control.tutorial.sentence);
            return line.contains("{") ? text.get(line) : line;
        }

        void load(){
            this.line = Core.bundle.has("tutorial." + name() + ".mobile") && mobile ? "tutorial." + name() + ".mobile" : "tutorial." + name();
            this.sentences = Seq.select(Core.bundle.get(line).split("\n"), s -> !s.isEmpty());
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

        static void placeBlocks(){
            Building core = state.teams.playerCores().first();
            for(int i = 0; i < blocksToBreak; i++){
                world.tile(core.tile().x + blockOffset, core.tile().y + i).remove();
                world.tile(core.tile().x + blockOffset, core.tile().y + i).setBlock(Blocks.scrapWall, state.rules.defaultTeam);
            }
        }

        static boolean blocksBroken(){
            Building core = state.teams.playerCores().first();

            for(int i = 0; i < blocksToBreak; i++){
                if(world.tile(core.tile.x + blockOffset, core.tile.y + i).block() == Blocks.scrapWall){
                    return false;
                }
            }
            return true;
        }

        static boolean event(String name){
            return control.tutorial.events.contains(name);
        }

        static boolean placed(Block block, int amount){
            return placed(block) >= amount;
        }

        static int placed(Block block){
            return control.tutorial.blocksPlaced.get(block, 0);
        }

        static int item(Item item){
            return state.rules.defaultTeam.data().noCores() ? 0 : state.rules.defaultTeam.core().items.get(item);
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
                float sin = Mathf.sin(11f, Scl.scl(4f));
                Lines.stroke(Scl.scl(7f), Pal.place);
                Lines.rect(Tmp.v1.x - sin, Tmp.v1.y - sin, element.getWidth() + sin*2, element.getHeight() + sin*2);

                float size = Math.max(element.getWidth(), element.getHeight()) + Mathf.absin(11f/2f, Scl.scl(18f));
                float angle = Angles.angle(Core.graphics.getWidth()/2f, Core.graphics.getHeight()/2f, Tmp.v1.x + element.getWidth()/2f, Tmp.v1.y + element.getHeight()/2f);
                Tmp.v2.trns(angle + 180f, size*1.4f);
                float fs = Scl.scl(40f);
                float fs2 = Scl.scl(56f);

                Draw.color(Pal.gray);
                Drawf.tri(Tmp.v1.x + element.getWidth()/2f + Tmp.v2.x, Tmp.v1.y + element.getHeight()/2f + Tmp.v2.y, fs2, fs2, angle);
                Draw.color(Pal.place);
                Tmp.v2.setLength(Tmp.v2.len() - Scl.scl(4));
                Drawf.tri(Tmp.v1.x + element.getWidth()/2f + Tmp.v2.x, Tmp.v1.y + element.getHeight()/2f + Tmp.v2.y, fs, fs, angle);
                Draw.reset();
            }
        }
    }

}
