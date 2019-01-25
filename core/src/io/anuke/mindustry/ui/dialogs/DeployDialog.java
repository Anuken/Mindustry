package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectIntMap;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.style.TextureRegionDrawable;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.ScrollPane;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Zones;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.game.Saves.SaveSlot;
import io.anuke.mindustry.io.SaveIO.SaveException;
import io.anuke.mindustry.type.Item;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.type.ItemType;
import io.anuke.mindustry.type.Zone;
import io.anuke.mindustry.ui.TreeLayout;
import io.anuke.mindustry.ui.TreeLayout.TreeNode;
import io.anuke.mindustry.ui.dialogs.TechTreeDialog.TechTreeNode;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;

import static io.anuke.mindustry.Vars.*;

public class DeployDialog extends FloatingDialog{
    private static final float nodeSize = 250f;
    private ZoneNode root;
    private ObjectSet<ZoneNode> nodes = new ObjectSet<>();

    public DeployDialog(){
        super("");

        root = new ZoneNode(Zones.groundZero, null);

        TreeLayout layout = new TreeLayout();
        layout.gapBetweenLevels = 40f;
        layout.gapBetweenNodes = 40f;
        layout.layout(root);

        cont.setFillParent(true);
        cont.add(new View()).grow();

        shown(this::setup);
    }

    public void setup(){
        buttons.clear();
        cont.clear();

        addCloseButton();
        buttons.addImageTextButton("$techtree", "icon-tree", 16 * 2, () -> ui.tech.show()).size(230f, 64f);

        cont.stack(new Table(){{
            top().left().margin(10);

            ObjectIntMap<Item> items = data.items();
            for(Item item : content.items()){
                if(item.type == ItemType.material && data.isUnlocked(item)){
                    label(() -> items.get(item, 0) + "").left();
                    addImage(item.region).size(8*4).pad(4);
                    add("[LIGHT_GRAY]" + item.localizedName()).left();
                    row();
                }
            }

        }}, new ScrollPane(new Table(){{

            if(control.saves.getZoneSlot() == null){

                int i = 0;
                for(Zone zone : content.zones()){
                    table(t -> {
                        TextButton button = t.addButton("", () -> {
                            if(!data.isUnlocked(zone)){
                                data.removeItems(zone.itemRequirements);
                                data.unlockContent(zone);
                                setup();
                            }else{
                                data.removeItems(zone.deployCost);
                                hide();
                                world.playZone(zone);
                            }
                        }).size(250f).disabled(b -> !canUnlock(zone)).get();

                        button.clearChildren();

                        if(data.isUnlocked(zone)){
                            button.table(title -> {
                                title.addImage("icon-zone").padRight(3);
                                title.add(zone.localizedName());
                            });
                            button.row();

                            if(data.getWaveScore(zone) > 0){
                                button.add(Core.bundle.format("bestwave", data.getWaveScore(zone)));
                            }

                            button.row();

                            button.add("$launch").color(Color.LIGHT_GRAY).pad(4);
                            button.row();
                            button.table(req -> {
                                for(ItemStack stack : zone.deployCost){
                                    req.addImage(stack.item.region).size(8 * 3);
                                    req.add(stack.amount + "").left();
                                }
                            }).pad(3).growX();
                        }else{
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
                        }
                    }).pad(4);

                    if(++i % 2 == 0){
                        row();
                    }
                }
            }else{
                SaveSlot slot = control.saves.getZoneSlot();

                TextButton b[] = {null};

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
            }
        }})).grow();
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

    boolean canUnlock(Zone zone){
        if(data.isUnlocked(zone)){
            return true;
        }

        for(Zone other : zone.zoneRequirements){
            if(!data.isCompleted(other)){
                return false;
            }
        }

        for(Block other : zone.blockRequirements){
            if(!data.isUnlocked(other)){
                return false;
            }
        }

        return data.hasItems(zone.itemRequirements);
    }

    static Array<Zone> arr = new Array<>();

    class View extends Group{
        float panX = 0, panY = -200;

        {

            for(ZoneNode node : nodes){
                ImageButton button = new ImageButton("", "node");

                button.setSize(nodeSize, nodeSize);
                button.update(() -> {
                    button.setPosition(node.x + panX + width/2f, node.y + panY + height/2f, Align.center);
                    button.getStyle().up = Core.scene.skin.getDrawable(!locked(node.node) ? "content-background" : "content-background-locked");
                    ((TextureRegionDrawable)button.getStyle().imageUp)
                            .setRegion(node.visible ? node.node.block.icon(Icon.medium) : Core.atlas.find("icon-tree-locked"));
                    button.getImage().setColor(!locked(node.node) ? Color.WHITE : Color.GRAY);
                });
                addChild(button);
            }

            dragged((x, y) -> {
                panX += x;
                panY += y;
            });
        }
    }

    class ZoneNode extends TreeNode<ZoneNode>{
        final Zone zone;

        ZoneNode(Zone zone, ZoneNode parent){
            this.zone = zone;
            this.parent = parent;
            this.width = this.height = nodeSize;
            nodes.add(this);

            arr.selectFrom(content.zones(), other -> Structs.contains(other.zoneRequirements, zone));

            children = new ZoneNode[arr.size];
            for(int i = 0; i < children.length; i++){
                children[i] = new ZoneNode(arr.get(i), this);
            }
        }
    }
}
