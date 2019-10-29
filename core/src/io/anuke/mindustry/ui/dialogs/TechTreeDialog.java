package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.*;
import io.anuke.arc.collection.*;
import io.anuke.arc.graphics.*;
import io.anuke.arc.graphics.g2d.*;
import io.anuke.arc.input.*;
import io.anuke.arc.math.*;
import io.anuke.arc.math.geom.*;
import io.anuke.arc.scene.*;
import io.anuke.arc.scene.actions.*;
import io.anuke.arc.scene.event.*;
import io.anuke.arc.scene.style.*;
import io.anuke.arc.scene.ui.*;
import io.anuke.arc.scene.ui.layout.*;
import io.anuke.arc.util.*;
import io.anuke.mindustry.content.*;
import io.anuke.mindustry.content.TechTree.*;
import io.anuke.mindustry.game.EventType.*;
import io.anuke.mindustry.gen.*;
import io.anuke.mindustry.graphics.*;
import io.anuke.mindustry.type.*;
import io.anuke.mindustry.ui.*;
import io.anuke.mindustry.ui.Cicon;
import io.anuke.mindustry.ui.layout.*;
import io.anuke.mindustry.ui.layout.TreeLayout.*;

import static io.anuke.mindustry.Vars.*;

public class TechTreeDialog extends FloatingDialog{
    private final float nodeSize = Scl.scl(60f);
    private ObjectSet<TechTreeNode> nodes = new ObjectSet<>();
    private TechTreeNode root = new TechTreeNode(TechTree.root, null);
    private Rectangle bounds = new Rectangle();
    private ItemsDisplay items;
    private View view;

    public TechTreeDialog(){
        super("");

        titleTable.remove();
        margin(0f).marginBottom(8);
        Stack stack = cont.stack(view = new View(), items = new ItemsDisplay()).grow().get();

        Events.on(ContentReloadEvent.class, e -> {
            nodes.clear();
            root = new TechTreeNode(TechTree.root, null);
            checkNodes(root);
            treeLayout();
            stack.getChildren().get(0).remove();
            stack.addChildAt(0, view = new View());
        });

        shown(() -> {
            checkNodes(root);
            treeLayout();
        });

        hidden(ui.deploy::setup);

        addCloseButton();

        buttons.addImageTextButton("$database", Icon.database, () -> {
            hide();
            ui.database.show();
        }).size(210f, 64f);

        //scaling/drag input

        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                view.setScale(Mathf.clamp(view.getScaleX() - amountY / 40f, 0.25f, 1f));
                view.setOrigin(Align.center);
                view.setTransform(true);
                return true;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                view.requestScroll();
                return super.mouseMoved(event, x, y);
            }
        });

        addListener(new ElementGestureListener(){
            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(view.lastZoom < 0){
                    view.lastZoom = view.getScaleX();
                }

                view.setScale(Mathf.clamp(distance / initialDistance * view.lastZoom, 0.25f, 1f));
                view.setOrigin(Align.center);
                view.setTransform(true);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                view.lastZoom = view.getScaleX();
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                view.panX += deltaX / view.getScaleX();
                view.panY += deltaY / view.getScaleY();
                view.moved = true;
                view.clamp();
            }
        });
    }

    void treeLayout(){
        RadialTreeLayout layout = new RadialTreeLayout();
        LayoutNode node = new LayoutNode(root, null);
        layout.layout(node);
        float minx = 0f, miny = 0f, maxx = 0f, maxy = 0f;
        copyInfo(node);

        for(TechTreeNode n : nodes){
            if(!n.visible) continue;
            minx = Math.min(n.x - n.width/2f, minx);
            maxx = Math.max(n.x + n.width/2f, maxx);
            miny = Math.min(n.y - n.height/2f, miny);
            maxy = Math.max(n.y + n.height/2f, maxy);
        }
        bounds = new Rectangle(minx, miny, maxx - minx, maxy - miny);
        bounds.y += nodeSize*1.5f;
    }

    void copyInfo(LayoutNode node){
        node.node.x = node.x;
        node.node.y = node.y;
        if(node.children != null){
            for(LayoutNode child : node.children){
                copyInfo(child);
            }
        }
    }

    void checkNodes(TechTreeNode node){
        boolean locked = locked(node.node);
        if(!locked) node.visible = true;
        for(TechTreeNode l : node.children){
            l.visible = !locked;
            checkNodes(l);
        }

        items.rebuild();
    }

    void showToast(String info){
        Table table = new Table();
        table.actions(Actions.fadeOut(0.5f, Interpolation.fade), Actions.remove());
        table.top().add(info);
        table.setName("toast");
        table.update(() -> {
            table.toFront();
            table.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() - 21, Align.top);
        });
        Core.scene.add(table);
    }

    boolean locked(TechNode node){
        return node.block.locked();
    }

    class LayoutNode extends TreeNode<LayoutNode>{
        final TechTreeNode node;

        LayoutNode(TechTreeNode node, LayoutNode parent){
            this.node = node;
            this.parent = parent;
            this.width = this.height = nodeSize;
            if(node.children != null){
                children = Array.with(node.children).map(t -> new LayoutNode(t, this)).toArray(LayoutNode.class);
            }
        }
    }

    class TechTreeNode extends TreeNode<TechTreeNode>{
        final TechNode node;
        boolean visible = true;

        TechTreeNode(TechNode node, TechTreeNode parent){
            this.node = node;
            this.parent = parent;
            this.width = this.height = nodeSize;
            nodes.add(this);
            if(node.children != null){
                children = new TechTreeNode[node.children.size];
                for(int i = 0; i < children.length; i++){
                    children[i] = new TechTreeNode(node.children.get(i), this);
                }
            }
        }
    }

    class View extends Group{
        float panX = 0, panY = -200, lastZoom = -1;
        boolean moved = false;
        ImageButton hoverNode;
        Table infoTable = new Table();

        {
            infoTable.touchable(Touchable.enabled);

            for(TechTreeNode node : nodes){
                ImageButton button = new ImageButton(node.node.block.icon(Cicon.medium), Styles.nodei);
                button.visible(() -> node.visible);
                button.clicked(() -> {
                    if(moved) return;

                    if(mobile){
                        hoverNode = button;
                        rebuild();
                        float right = infoTable.getRight();
                        if(right > Core.graphics.getWidth()){
                            float moveBy = right - Core.graphics.getWidth();
                            addAction(new RelativeTemporalAction(){
                                {
                                    setDuration(0.1f);
                                    setInterpolation(Interpolation.fade);
                                }

                                @Override
                                protected void updateRelative(float percentDelta){
                                    panX -= moveBy * percentDelta;
                                }
                            });
                        }
                    }else if(data.hasItems(node.node.requirements) && locked(node.node)){
                        unlock(node.node);
                    }
                });
                button.hovered(() -> {
                    if(!mobile && hoverNode != button && node.visible){
                        hoverNode = button;
                        rebuild();
                    }
                });
                button.exited(() -> {
                    if(!mobile && hoverNode == button && !infoTable.hasMouse() && !hoverNode.hasMouse()){
                        hoverNode = null;
                        rebuild();
                    }
                });
                button.touchable(() -> !node.visible ? Touchable.disabled : Touchable.enabled);
                button.setUserObject(node.node);
                button.setSize(nodeSize);
                button.update(() -> {
                    float offset = (Core.graphics.getHeight() % 2) / 2f;
                    button.setPosition(node.x + panX + width / 2f, node.y + panY + height / 2f + offset, Align.center);
                    button.getStyle().up = !locked(node.node) ? Tex.buttonOver : !data.hasItems(node.node.requirements) ? Tex.buttonRed : Tex.button;
                    ((TextureRegionDrawable)button.getStyle().imageUp)
                    .setRegion(node.visible ? node.node.block.icon(Cicon.medium) : Core.atlas.find("icon-locked"));
                    button.getImage().setColor(!locked(node.node) ? Color.white : Color.gray);
                });
                addChild(button);
            }

            if(mobile){
                tapped(() -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e == this){
                        hoverNode = null;
                        rebuild();
                    }
                });
            }

            setOrigin(Align.center);
            setTransform(true);
            released(() -> moved = false);
        }

        void clamp(){
            float pad = nodeSize;

            float ox = width/2f, oy = height/2f;
            float rx = bounds.x + panX + ox, ry = panY + oy + bounds.y;
            float rw = bounds.width, rh = bounds.height;
            rx = Mathf.clamp(rx, -rw + pad, Core.graphics.getWidth() - pad);
            ry = Mathf.clamp(ry, -rh + pad, Core.graphics.getHeight() - pad);
            panX = rx - bounds.x - ox;
            panY = ry - bounds.y - oy;
        }

        void unlock(TechNode node){
            data.unlockContent(node.block);
            data.removeItems(node.requirements);
            showToast(Core.bundle.format("researched", node.block.localizedName));
            checkNodes(root);
            hoverNode = null;
            treeLayout();
            rebuild();
            Core.scene.act();
            Sounds.unlock.play();
            Events.fire(new ResearchEvent(node.block));
        }

        void rebuild(){
            ImageButton button = hoverNode;

            infoTable.remove();
            infoTable.clear();
            infoTable.update(null);

            if(button == null) return;

            TechNode node = (TechNode)button.getUserObject();

            infoTable.exited(() -> {
                if(hoverNode == button && !infoTable.hasMouse() && !hoverNode.hasMouse()){
                    hoverNode = null;
                    rebuild();
                }
            });

            infoTable.update(() -> infoTable.setPosition(button.getX() + button.getWidth(), button.getY() + button.getHeight(), Align.topLeft));

            infoTable.left();
            infoTable.background(Tex.button).margin(8f);

            infoTable.table(b -> {
                b.margin(0).left().defaults().left();

                b.addImageButton(Icon.infoSmall, Styles.cleari, () -> ui.content.show(node.block)).growY().width(50f);
                b.add().grow();
                b.table(desc -> {
                    desc.left().defaults().left();
                    desc.add(node.block.localizedName);
                    desc.row();
                    if(locked(node)){
                        desc.table(t -> {
                            t.left();
                            for(ItemStack req : node.requirements){
                                t.table(list -> {
                                    list.left();
                                    list.addImage(req.item.icon(Cicon.small)).size(8 * 3).padRight(3);
                                    list.add(req.item.localizedName()).color(Color.lightGray);
                                    list.label(() -> " " + Math.min(data.getItem(req.item), req.amount) + " / " + req.amount)
                                    .update(l -> l.setColor(data.has(req.item, req.amount) ? Color.lightGray : Color.scarlet));
                                }).fillX().left();
                                t.row();
                            }
                        });
                    }else{
                        desc.add("$completed");
                    }
                }).pad(9);

                if(mobile && locked(node)){
                    b.row();
                    b.addImageTextButton("$research", Icon.checkSmall, Styles.nodet, () -> unlock(node))
                    .disabled(i -> !data.hasItems(node.requirements)).growX().height(44f).colspan(3);
                }
            });

            infoTable.row();
            if(node.block.description != null){
                infoTable.table(t -> t.margin(3f).left().labelWrap(node.block.description).color(Color.lightGray).growX()).fillX();
            }

            addChild(infoTable);
            infoTable.pack();
            infoTable.act(Core.graphics.getDeltaTime());
        }

        @Override
        public void drawChildren(){
            clamp();
            float offsetX = panX + width / 2f, offsetY = panY + height / 2f;

            for(TechTreeNode node : nodes){
                if(!node.visible) continue;
                for(TechTreeNode child : node.children){
                    if(!child.visible) continue;

                    Lines.stroke(Scl.scl(4f), locked(node.node) || locked(child.node) ? Pal.gray : Pal.accent);
                    Draw.alpha(parentAlpha);
                    Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                }
            }

            Draw.reset();
            super.drawChildren();
        }
    }
}
