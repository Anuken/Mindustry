package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.style.TextureRegionDrawable;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.TechTree;
import io.anuke.mindustry.content.TechTree.TechNode;
import io.anuke.mindustry.graphics.Pal;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.ItemsDisplay;
import io.anuke.mindustry.ui.TreeLayout;
import io.anuke.mindustry.ui.TreeLayout.TreeNode;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;

import static io.anuke.mindustry.Vars.*;

public class TechTreeDialog extends FloatingDialog{
    private static final float nodeSize = 60f;
    private ObjectSet<TechTreeNode> nodes = new ObjectSet<>();
    private TechTreeNode root = new TechTreeNode(TechTree.root, null);
    private ItemsDisplay items;

    public TechTreeDialog(){
        super("");

        TreeLayout layout = new TreeLayout();
        layout.gapBetweenLevels = 60f;
        layout.gapBetweenNodes = 40f;
        layout.layout(root);

        titleTable.remove();
        margin(0f);
        cont.stack(new View(), items = new ItemsDisplay()).grow();

        { //debug code; TODO remove
            ObjectSet<Block> used = new ObjectSet<Block>().select(t -> true);
            for(TechTreeNode node : nodes){
                used.add(node.node.block);
            }
            Array<Block> recipes = content.blocks().select(r -> r.isVisible() && !used.contains(r));
            recipes.sort(Structs.comparing(r -> r.buildCost));

            if(recipes.size > 0){
                Log.info("Missing recipe tree items! ");
                recipes.forEach(r -> Log.info(">    {0}", r));
            }
        }

        shown(() -> checkNodes(root));
        hidden(ui.deploy::setup);
        addCloseButton();
    }

    @Override
    protected void drawBackground(float x, float y){
        drawDefaultBackground(x, y);
    }

    void checkNodes(TechTreeNode node){
        boolean locked = locked(node.node);
        if(!locked) node.visible = true;
        for(TechTreeNode l : node.children){
            l.visible = !locked && l.node.block.isVisible();
            checkNodes(l);
        }

        items.rebuild();
    }

    void showToast(String info){
        int maxIndex = 0;

        for(Element e : Core.scene.root.getChildren()){
            if("toast".equals(e.getName())){
                maxIndex = Math.max(maxIndex, (Integer)e.getUserObject() + 1);
            }
        }

        int m = maxIndex;

        Table table = new Table();
        table.actions(Actions.fadeOut(7f, Interpolation.fade), Actions.removeActor());
        table.top().add(info);
        table.setName("toast");
        table.setUserObject(maxIndex);
        table.update(() -> {
            table.toFront();
            table.setPosition(Core.graphics.getWidth()/2f, Core.graphics.getHeight() - 21 - m*20f, Align.top);
        });
        Core.scene.add(table);
    }

    boolean locked(TechNode node){
        return !data.isUnlocked(node.block);
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
                button.clicked(() -> {
                    if(mobile){
                        hoverNode = button;
                        rebuild();
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
                button.setSize(nodeSize, nodeSize);
                button.update(() -> {
                    float offset = (Core.graphics.getHeight() % 2) / 2f;
                    button.setPosition(node.x + panX + width/2f, node.y + panY + height/2f + offset, Align.center);
                    button.getStyle().up = Core.scene.skin.getDrawable(!locked(node.node) ? "content-background" : "content-background-locked");
                    ((TextureRegionDrawable)button.getStyle().imageUp)
                        .setRegion(node.visible ? node.node.block.icon(Icon.medium) : Core.atlas.find("icon-tree-locked"));
                    button.getImage().setColor(!locked(node.node) ? Color.WHITE : Color.GRAY);
                });
                addChild(button);
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
            showToast(Core.bundle.format("researched", node.block.formalName));
            checkNodes(root);
            hoverNode = null;
            rebuild();
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

            infoTable.background("content-background");
            infoTable.update(() -> infoTable.setPosition(button.getX() + button.getWidth(), button.getY() + button.getHeight(), Align.topLeft));

            infoTable.margin(0).left().defaults().left();

            infoTable.addImageButton("icon-info", "node", 14*2, () -> ui.content.show(node.block)).growY().width(50f);

            infoTable.add().grow();

            infoTable.table(desc -> {
                desc.left().defaults().left();
                desc.add(node.block.formalName);
                desc.row();
                if(locked(node)){
                    desc.table(t -> {
                        t.left();
                        for(ItemStack req : node.requirements){
                            t.table(list -> {
                                list.left();
                                list.addImage(req.item.getContentIcon()).size(8 * 3).padRight(3);
                                list.add(req.item.localizedName()).color(Color.LIGHT_GRAY);
                                list.add(" " + Math.min(data.items().get(req.item, 0), req.amount) + " / " + req.amount)
                                .color(data.has(req.item, req.amount) ? Color.LIGHT_GRAY : Color.SCARLET);
                            }).fillX().left();
                            t.row();
                        }
                    });
                }else{
                    desc.add("$completed");
                }
            }).pad(9);

            if(mobile && locked(node)){
                infoTable.row();
                infoTable.addImageTextButton("$research", "icon-check", "node", 16*2, () -> unlock(node))
                .disabled(b -> !data.hasItems(node.requirements)).growX().height(44f).colspan(3);
            }

            addChild(infoTable);
            infoTable.pack();
        }

        @Override
        public void draw(){
            float offsetX = panX + width/2f + x, offsetY = panY + height/2f + y;

            for(TechTreeNode node : nodes){
                for(TechTreeNode child : node.children){
                    Lines.stroke(3f, locked(node.node) || locked(child.node) ? Pal.locked : Pal.accent);

                    Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                }
            }

            super.draw();
        }
    }
}
