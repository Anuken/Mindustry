package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.actions.RelativeTemporalAction;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.style.TextureRegionDrawable;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.scene.ui.layout.Unit;
import io.anuke.arc.util.Align;
import io.anuke.mindustry.content.TechTree;
import io.anuke.mindustry.content.TechTree.TechNode;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemsDisplay;
import io.anuke.mindustry.ui.TreeLayout;
import io.anuke.mindustry.ui.TreeLayout.TreeNode;
import io.anuke.mindustry.world.Block.Icon;

import static io.anuke.mindustry.Vars.*;

public class TechTreeDialog extends FloatingDialog{
    private final float nodeSize = Unit.dp.scl(60f);
    private ObjectSet<TechTreeNode> nodes = new ObjectSet<>();
    private TechTreeNode root = new TechTreeNode(TechTree.root, null);
    private ItemsDisplay items;

    public TechTreeDialog(){
        super("");

        titleTable.remove();
        margin(0f).marginBottom(8);
        cont.stack(new View(), items = new ItemsDisplay()).grow();

        shown(() -> {
            checkNodes(root);
            treeLayout();
        });

        hidden(ui.deploy::setup);

        addCloseButton();

        buttons.addImageTextButton("$database", "icon-database", iconsize, () -> {
            hide();
            ui.database.show();
        }).size(210f, 64f);
    }

    @Override
    protected void drawBackground(float x, float y){
        drawDefaultBackground(x, y);
    }

    void treeLayout(){
        TreeLayout layout = new TreeLayout();
        layout.gapBetweenLevels = Unit.dp.scl(60f);
        layout.gapBetweenNodes = Unit.dp.scl(40f);
        LayoutNode node = new LayoutNode(root, null);
        layout.layout(node);
        copyInfo(node);
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
                children = Array.with(node.children).select(n -> n.visible).map(t -> new LayoutNode(t, this)).toArray(LayoutNode.class);
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
        float panX = 0, panY = -200;
        boolean moved = false;
        ImageButton hoverNode;
        Table infoTable = new Table();

        {
            infoTable.touchable(Touchable.enabled);

            for(TechTreeNode node : nodes){
                ImageButton button = new ImageButton(node.node.block.icon(Icon.medium), "node");
                button.visible(() -> node.visible);
                button.clicked(() -> {
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
                button.tapped(() -> moved = false);
                button.setSize(nodeSize);
                button.update(() -> {
                    float offset = (Core.graphics.getHeight() % 2) / 2f;
                    button.setPosition(node.x + panX + width / 2f, node.y + panY + height / 2f + offset, Align.center);
                    button.getStyle().up = Core.scene.skin.getDrawable(!locked(node.node) ? "content-background" : !data.hasItems(node.node.requirements) ? "content-background-noitems" : "content-background-locked");
                    ((TextureRegionDrawable)button.getStyle().imageUp)
                    .setRegion(node.visible ? node.node.block.icon(Icon.medium) : Core.atlas.find("icon-locked"));
                    button.getImage().setColor(!locked(node.node) ? Color.WHITE : Color.GRAY);
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

            dragged((x, y) -> {
                moved = true;
                panX += x;
                panY += y;
            });
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

            infoTable.table("content-background", b -> {
                b.margin(0).left().defaults().left();

                b.addImageButton("icon-info", "node", iconsize, () -> ui.content.show(node.block)).growY().width(50f);
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
                                    list.addImage(req.item.getContentIcon()).size(8 * 3).padRight(3);
                                    list.add(req.item.localizedName()).color(Color.LIGHT_GRAY);
                                    list.label(() -> " " + Math.min(data.getItem(req.item), req.amount) + " / " + req.amount)
                                    .update(l -> l.setColor(data.has(req.item, req.amount) ? Color.LIGHT_GRAY : Color.SCARLET));
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
                    b.addImageTextButton("$research", "icon-check", "node", iconsize, () -> unlock(node))
                    .disabled(i -> !data.hasItems(node.requirements)).growX().height(44f).colspan(3);
                }
            });

            infoTable.row();
            if(node.block.description != null){
                infoTable.table("dialogDim", t -> t.margin(3f).left().labelWrap(node.block.description).color(Color.LIGHT_GRAY).growX()).fillX();
            }


            addChild(infoTable);
            infoTable.pack();
            infoTable.act(Core.graphics.getDeltaTime());
        }

        @Override
        public void draw(){
            float offsetX = panX + width / 2f + x, offsetY = panY + height / 2f + y;

            for(TechTreeNode node : nodes){
                if(!node.visible) continue;
                for(TechTreeNode child : node.children){
                    if(!child.visible) continue;

                    Lines.stroke(Unit.dp.scl(3f), locked(node.node) || locked(child.node) ? Pal.locked : Pal.accent);
                    Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                }
            }

            Draw.reset();
            super.draw();
        }
    }
}
