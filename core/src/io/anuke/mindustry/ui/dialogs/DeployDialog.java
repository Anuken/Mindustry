package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Zones;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.io.SaveIO.SaveException;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.ui.ItemsDisplay;
import io.anuke.mindustry.ui.TreeLayout;
import io.anuke.mindustry.ui.TreeLayout.TreeNode;

import static io.anuke.mindustry.Vars.*;

public class DeployDialog extends FloatingDialog{
    private final float nodeSize = Unit.dp.scl(210f);
    private ObjectSet<ZoneNode> nodes = new ObjectSet<>();
    private ZoneInfoDialog info = new ZoneInfoDialog();

    public DeployDialog(){
        super("");

        ZoneNode root = new ZoneNode(Zones.groundZero, null);

        TreeLayout layout = new TreeLayout();
        layout.gapBetweenLevels = layout.gapBetweenNodes = Unit.dp.scl(50f);
        layout.layout(root);

        addCloseButton();
        buttons.addImageTextButton("$techtree", "icon-tree", 16 * 2, () -> ui.tech.show()).size(230f, 64f);

        shown(this::setup);
    }

    public void setup(){
        cont.clear();
        titleTable.remove();
        margin(0f).marginBottom(8);

        cont.stack(control.saves.getZoneSlot() == null ? new View() : new Table(){{
            SaveSlot slot = control.saves.getZoneSlot();

            TextButton[] b = {null};

            TextButton button = addButton(Core.bundle.format("resume", slot.getZone().localizedName()), () -> {
                if(b[0].childrenPressed()) return;

                hide();
                ui.loadAnd(() -> {
                    try{
                        control.saves.getZoneSlot().load();
                        state.set(State.playing);
                    }catch(SaveException e){ //make sure to handle any save load errors!
                        e.printStackTrace();
                        if(control.saves.getZoneSlot() != null) control.saves.getZoneSlot().delete();
                        ui.showInfo("$save.corrupted");
                        show();
                    }
                });
            }).size(200f).get();
            b[0] = button;

            String color = "[lightgray]";

            button.defaults().colspan(2);
            button.row();
            button.add(Core.bundle.format("save.wave", color + slot.getWave()));
            button.row();
            button.label(() -> Core.bundle.format("save.playtime", color + slot.getPlayTime()));
            button.row();
            button.add().grow();
            button.row();

            button.addButton("$abandon", () -> {
                ui.showConfirm("$warning", "$abandon.text", () -> {
                    slot.delete();
                    setup();
                });
            }).growX().height(50f).pad(-12).padTop(10);

        }}, new ItemsDisplay()).grow();
    }

    boolean hidden(Zone zone){
        for(Zone other : zone.zoneRequirements){
            if(!data.isUnlocked(other)){
                return true;
            }
        }
        return false;
    }

    @Override
    protected void drawBackground(float x, float y){
        drawDefaultBackground(x, y);
    }



    void buildButton(Zone zone, TextButton button){
        button.setDisabled(() -> hidden(zone));
        button.clicked(() -> info.show(zone));

        if(zone.unlocked()){
            //button.table(title -> {
            button.addImage("icon-zone").padRight(3);
            button.labelWrap(zone.localizedName()).width(140).growX();
            //});

            //if(data.getWaveScore(zone) > 0){
            //    button.add(Core.bundle.format("bestwave", data.getWaveScore(zone)));
            //}

            /*
            button.add("$launch").color(Color.LIGHT_GRAY).pad(4);
            button.row();
            button.table(req -> {
                for(ItemStack stack : zone.deployCost){
                    req.addImage(stack.item.region).size(8 * 3);
                    req.add(stack.amount + "").left();
                }
            }).pad(3).growX();*/
        }else{
            button.addImage("icon-zone-locked");
            button.row();
            button.add("$locked");

            /*else{
            button.addImage("icon-zone-locked");
            button.row();
            button.add("$locked").padBottom(6);

            if(!hidden(zone)){
                button.row();

                button.table(req -> {
                    req.defaults().left();

                    if(zone.zoneRequirements.length > 0){
                        req.table(r -> {
                            r.add("$complete").colspan(2).left();
                            r.row();
                            for(Zone other : zone.zoneRequirements){
                                r.addImage("icon-zone").padRight(4);
                                r.add(other.localizedName()).color(Color.LIGHT_GRAY);
                                r.addImage(data.isCompleted(other) ? "icon-check-2" : "icon-cancel-2")
                                .color(data.isCompleted(other) ? Color.LIGHT_GRAY : Color.SCARLET).padLeft(3);
                                r.row();
                            }
                        });
                    }

                    req.row();

                    if(zone.itemRequirements.length > 0){
                        req.table(r -> {
                            for(ItemStack stack : zone.itemRequirements){
                                r.addImage(stack.item.region).size(8 * 3).padRight(4);
                                r.add(Math.min(data.getItem(stack.item), stack.amount) + "/" + stack.amount)
                                .color(stack.amount > data.getItem(stack.item) ? Color.SCARLET : Color.LIGHT_GRAY).left();
                                r.row();
                            }
                        }).padTop(10);
                    }

                    req.row();

                    if(zone.blockRequirements.length > 0){
                        req.table(r -> {
                            r.add("$research.list").colspan(2).left();
                            r.row();
                            for(Block block : zone.blockRequirements){
                                r.addImage(block.icon(Icon.small)).size(8 * 3).padRight(4);
                                r.add(block.formalName).color(Color.LIGHT_GRAY);
                                r.addImage(data.isUnlocked(block) ? "icon-check-2" : "icon-cancel-2")
                                .color(data.isUnlocked(block) ? Color.LIGHT_GRAY : Color.SCARLET).padLeft(3);
                                r.row();
                            }

                        }).padTop(10);
                    }
                }).growX();
            }
        }*/
        }
    }

    //should be static variables of View, but that's impossible
    static float panX = 0, panY = -200;

    class View extends Group{

        {
            for(ZoneNode node : nodes){
                TextButton button = new TextButton("", "node");
                button.setSize(node.width, node.height);
                button.update(() -> {
                    button.setPosition(node.x + panX + width/2f, node.y + panY + height/2f, Align.center);
                });
                button.clearChildren();
                buildButton(node.zone, button);
                addChild(button);
            }

            dragged((x, y) -> {
                panX += x;
                panY += y;
            });
        }

        @Override
        public void draw(){
            float offsetX = panX + width/2f + x, offsetY = panY + height/2f + y;

            for(ZoneNode node : nodes){
                for(ZoneNode child : node.children){
                    Lines.stroke(Unit.dp.scl(3f), node.zone.locked() || child.zone.locked() ? Pal.locked : Pal.accent);
                    Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                }
            }

            Draw.reset();
            super.draw();
        }
    }

    class ZoneNode extends TreeNode<ZoneNode>{
        final Array<Zone> arr = new Array<>();
        final Zone zone;

        ZoneNode(Zone zone, ZoneNode parent){
            this.zone = zone;
            this.parent = parent;
            this.width = this.height = nodeSize;
            this.height /= 2f;
            nodes.add(this);

            arr.selectFrom(content.zones(), other -> Structs.contains(other.zoneRequirements, zone));

            children = new ZoneNode[arr.size];
            for(int i = 0; i < children.length; i++){
                children[i] = new ZoneNode(arr.get(i), this);
            }
        }
    }
}
