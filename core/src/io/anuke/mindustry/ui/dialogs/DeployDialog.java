package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.collection.ObjectSet.ObjectSetIterator;
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
import io.anuke.mindustry.type.Zone.ZoneRequirement;
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
        buttons.addImageTextButton("$techtree", "icon-tree", iconsize, () -> ui.tech.show()).size(230f, 64f);

        shown(this::setup);
    }

    public void setup(){
        cont.clear();
        titleTable.remove();
        margin(0f).marginBottom(8);

        if(!Core.settings.getBool("zone-info", false)){
            Core.app.post(() -> ui.showInfoText("TEMPORARY GUIDE ON HOW TO PLAY ZONES", "- deploy to zones by selecting them here\n- most zones require items to deploy\n- once you survive a set amount of waves, you can launch all the resources in your core\n- use these items to research in the tech tree or uncover new zones"));

            Core.settings.put("zone-info", true);
            Core.settings.save();
        }

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
                        Core.app.post(() -> ui.showInfo("$save.corrupted"));
                        show();
                    }
                });
            }).size(230f).get();
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

        //set up direct and indirect children
        for(ZoneNode node : nodes){
            node.allChildren.clear();
            node.allChildren.addAll(node.children);
            for(ZoneNode other : new ObjectSetIterator<>(nodes)){
                if(Structs.contains(other.zone.zoneRequirements, req -> req.zone == node.zone)){
                    node.allChildren.add(other);
                }
            }
        }
    }

    boolean hidden(Zone zone){
        for(ZoneRequirement other : zone.zoneRequirements){
            if(!data.isUnlocked(other.zone)){
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
            button.addImage("icon-terrain").size(iconsize).padRight(3);
            button.labelWrap(zone.localizedName()).width(140).growX();
        }else{
            button.addImage("icon-locked");
            button.row();
            button.add("$locked");
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
                    button.setPosition(node.x + panX + width / 2f, node.y + panY + height / 2f, Align.center);
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
            float offsetX = panX + width / 2f + x, offsetY = panY + height / 2f + y;

            for(ZoneNode node : nodes){
                for(ZoneNode child : node.allChildren){
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
        final Array<ZoneNode> allChildren = new Array<>();
        final Zone zone;

        ZoneNode(Zone zone, ZoneNode parent){
            this.zone = zone;
            this.parent = parent;
            this.width = this.height = nodeSize;
            this.height /= 2f;
            nodes.add(this);

            arr.selectFrom(content.zones(), other -> other.zoneRequirements.length > 0 && other.zoneRequirements[0].zone == zone);

            children = new ZoneNode[arr.size];
            for(int i = 0; i < children.length; i++){
                children[i] = new ZoneNode(arr.get(i), this);
            }
        }
    }
}
