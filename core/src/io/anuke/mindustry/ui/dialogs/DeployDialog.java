package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.collection.ObjectSet.ObjectSetIterator;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.Texture;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Mathf;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.ui.Image;
import io.anuke.arc.scene.ui.TextButton;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
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
        super("", "fulldialog");

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

        Stack stack = new Stack();

        stack.add(new Image(new Texture("sprites/backgrounds/stars.png"){{
            setFilter(TextureFilter.Linear);
        }}){{
            //setColor(Color.fromGray(0.3f));
            //setScale(3f);
        }}.setScaling(Scaling.fill));

        stack.add(new Image(new Texture("sprites/backgrounds/planet-zero.png"){{
            setFilter(TextureFilter.Linear);
        }}){{
            float[] time = {0};
            setColor(Color.fromGray(0.3f));
            setScale(1.5f);
            update(() -> {
                setOrigin(Align.center);
                time[0] += Core.graphics.getDeltaTime() * 10f;
                setTranslation(Mathf.sin(time[0], 60f, 70f), Mathf.cos(time[0], 140f, 80f));
            });
        }}.setScaling(Scaling.fit));

        if(control.saves.getZoneSlot() != null){
            stack.add(new Table(t -> {
                SaveSlot slot = control.saves.getZoneSlot();

                TextButton button = t.addButton(Core.bundle.format("resume", slot.getZone().localizedName()), () -> {

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

                String color = "[lightgray]";

                button.defaults().colspan(2);
                button.row();
                button.add(Core.bundle.format("save.wave", color + slot.getWave()));
                button.row();
                button.label(() -> Core.bundle.format("save.playtime", color + slot.getPlayTime()));
                button.row();

                t.row();

                t.addButton("$abandon", () -> {
                    ui.showConfirm("$warning", "$abandon.text", () -> {
                        slot.delete();
                        setup();
                    });
                }).width(230f).height(50f).padTop(3);
            }));
        }else{
            stack.add(new View());
        }

        stack.add(new ItemsDisplay());

        cont.add(stack).grow();

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
                    Lines.stroke(Unit.dp.scl(3f), node.zone.locked() || child.zone.locked() ? Pal.gray : Pal.accent);
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
