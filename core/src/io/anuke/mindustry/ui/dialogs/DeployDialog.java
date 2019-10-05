package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.collection.ObjectSet.*;
import io.anuke.arc.function.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.scene.utils.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.core.GameState.*;
import io.anuke.mindustry.game.Saves.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.io.SaveIO.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.TreeLayout.*;

import static io.anuke.mindustry.Vars.*;

public class DeployDialog extends FloatingDialog{
    private final float nodeSize = Scl.scl(230f);
    private ObjectSet<ZoneNode> nodes = new ObjectSet<>();
    private ZoneInfoDialog info = new ZoneInfoDialog();
    private Rectangle bounds = new Rectangle();

    public DeployDialog(){
        super("", Styles.fullDialog);

        ZoneNode root = new ZoneNode(Zones.groundZero, null);

        TreeLayout layout = new TreeLayout();
        layout.gapBetweenLevels = layout.gapBetweenNodes = Scl.scl(60f);
        layout.gapBetweenNodes = Scl.scl(120f);
        layout.layout(root);
        bounds.set(layout.getBounds());
        bounds.y += nodeSize*0.4f;

        addCloseButton();
        buttons.addImageTextButton("$techtree", Icon.tree, () -> ui.tech.show()).size(230f, 64f);

        shown(this::setup);
    }

    public void setup(){
        platform.updateRPC();

        cont.clear();
        titleTable.remove();
        margin(0f).marginBottom(8);

        Stack stack = new Stack();

        stack.add(new Image(new Texture("sprites/backgrounds/stars.png"){{
            setFilter(TextureFilter.Linear);
        }}).setScaling(Scaling.fill));

        stack.add(new Image(new Texture("sprites/backgrounds/planet-zero.png"){{
            setFilter(TextureFilter.Linear);
        }}){{
            float[] time = {0};
            setColor(Color.fromGray(0.3f));
            setScale(1.5f);
            update(() -> {
                setOrigin(Align.center);
                time[0] += Core.graphics.getDeltaTime() * 10f;
                setTranslation(Mathf.sin(time[0], 60f, 70f) + panX / 30f, Mathf.cos(time[0], 140f, 80f) + (panY + 200) / 30f);
            });
        }}.setScaling(Scaling.fit));

        if(control.saves.getZoneSlot() != null){
            float size = 250f;

            stack.add(new Table(t -> {
                SaveSlot slot = control.saves.getZoneSlot();

                Stack sub = new Stack();

                if(slot.getZone() != null){
                    sub.add(new Table(f -> f.margin(4f).add(new Image()).color(Color.fromGray(0.1f)).grow()));

                    sub.add(new Table(f -> f.margin(4f).add(new Image(slot.getZone().preview).setScaling(Scaling.fit)).update(img -> {
                        TextureRegionDrawable draw = (TextureRegionDrawable)img.getDrawable();
                        if(draw.getRegion().getTexture().isDisposed()){
                            draw.setRegion(slot.getZone().preview);
                        }

                        Texture text = slot.previewTexture();
                        if(draw.getRegion() == slot.getZone().preview && text != null){
                            draw.setRegion(new TextureRegion(text));
                        }
                    }).color(Color.darkGray).grow()));
                }

                TextButton button = Elements.newButton(Core.bundle.format("resume", slot.getZone().localizedName()), Styles.squaret, () -> {
                    control.saves.getZoneSlot().cautiousLoad(() -> {
                        hide();
                        ui.loadAnd(() -> {
                            logic.reset();
                            net.reset();
                            try{
                                slot.load();
                                state.set(State.playing);
                            }catch(SaveException e){ //make sure to handle any save load errors!
                                e.printStackTrace();
                                if(control.saves.getZoneSlot() != null) control.saves.getZoneSlot().delete();
                                Core.app.post(() -> ui.showInfo("$save.corrupted"));
                                show();
                            }
                        });
                    });
                });

                sub.add(button);

                t.add(sub).size(size);

                String color = "[lightgray]";

                button.defaults().colspan(2);
                button.row();
                button.add(Core.bundle.format("save", color + slot.getWave()));
                button.row();
                button.label(() -> Core.bundle.format("save.playtime", color + slot.getPlayTime()));
                button.row();

                t.row();

                t.addButton("$abandon", () -> {
                    ui.showConfirm("$warning", "$abandon.text", () -> {
                        slot.delete();
                        setup();
                    });
                }).width(size).height(50f).padTop(3);
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
                if(other.zone.requirements.contains(req -> req.zone() == node.zone)){
                    node.allChildren.add(other);
                }
            }
        }
    }

    boolean hidden(Zone zone){
        return zone.requirements.contains(o -> o.zone() != null && o.zone().locked());
    }

    void buildButton(Zone zone, Button button){
        button.setDisabled(() -> hidden(zone));
        button.clicked(() -> info.show(zone));

        if(zone.unlocked() && !hidden(zone)){
            button.labelWrap(zone.localizedName()).style(Styles.outlineLabel).width(140).growX().get().setAlignment(Align.center);
        }else{
            Consumer<Element> flasher = zone.canUnlock() && !hidden(zone) ? e -> e.update(() -> e.getColor().set(Color.white).lerp(Pal.accent, Mathf.absin(3f, 1f))) : e -> {};
            flasher.accept(button.addImage(Icon.locked).get());
            button.row();
            flasher.accept(button.add("$locked").get());
        }
    }

    //should be static variables of View, but that's impossible
    static float panX = 0, panY = -200;

    class View extends Group{

        {
            for(ZoneNode node : nodes){
                Stack stack = new Stack();
                Tmp.v1.set(node.width, node.height);
                if(node.zone.preview != null){
                    Tmp.v1.set(Scaling.fit.apply(node.zone.preview.getWidth(), node.zone.preview.getHeight(), node.width, node.height));
                }

                stack.setSize(Tmp.v1.x, Tmp.v1.y);
                stack.add(new Table(t -> t.margin(4f).add(new Image(node.zone.preview).setScaling(Scaling.stretch)).color(node.zone.unlocked() ? Color.darkGray : Color.fromGray(0.2f)).grow()));
                stack.update(() -> stack.setPosition(node.x + panX + width / 2f, node.y + panY + height / 2f, Align.center));

                Button button = new Button(Styles.squaret);
                buildButton(node.zone, button);
                stack.add(button);
                addChild(stack);
            }

            dragged((x, y) -> {
                panX += x;
                panY += y;
                clamp();
            });
        }

        void clamp(){
            float pad = nodeSize;

            float ox = width/2f, oy = height/2f;
            float rx = bounds.x + panX + ox, ry = panY + oy + bounds.y;
            float rw = bounds.width, rh = bounds.height;
            rx = Mathf.clamp(rx, -rw + pad, Core.graphics.getWidth() - pad);
            ry = Mathf.clamp(ry, pad, Core.graphics.getHeight() - rh - pad);
            panX = rx - bounds.x - ox;
            panY = ry - bounds.y - oy;
        }

        @Override
        public void draw(){
            clamp();
            float offsetX = panX + width / 2f + x, offsetY = panY + height / 2f + y;

            for(ZoneNode node : nodes){
                for(ZoneNode child : node.allChildren){
                    Lines.stroke(Scl.scl(4f), node.zone.locked() || child.zone.locked() ? Pal.gray : Pal.gray);
                    Draw.alpha(parentAlpha);
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
            //this.height /= 2f;
            nodes.add(this);

            arr.selectFrom(content.zones(), other -> other.requirements.size > 0 && other.requirements.first().zone() == zone);

            children = new ZoneNode[arr.size];
            for(int i = 0; i < children.length; i++){
                children[i] = new ZoneNode(arr.get(i), this);
            }
        }
    }
}
