package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.Color;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.Interpolation;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.Group;
import io.anuke.arc.scene.actions.Actions;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.scene.event.Touchable;
import io.anuke.arc.scene.style.TextureRegionDrawable;
import io.anuke.arc.scene.ui.ImageButton;
import io.anuke.arc.scene.ui.layout.Table;
import io.anuke.arc.util.Align;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.TechTree;
import io.anuke.mindustry.content.TechTree.TechNode;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.ItemStack;
import io.anuke.mindustry.ui.TreeLayout;
import io.anuke.mindustry.ui.TreeLayout.TreeNode;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Block.Icon;

import static io.anuke.mindustry.Vars.*;

public class TechTreeDialog extends FloatingDialog{
    private ObjectSet<TechTreeNode> nodes = new ObjectSet<>();
    private TechTreeNode root = new TechTreeNode(TechTree.root, null);
    private static final float nodeSize = 60f;

    public TechTreeDialog(){
        super("");

        cont.setFillParent(true);

        TreeLayout layout = new TreeLayout();
        layout.gapBetweenLevels = 60f;
        layout.gapBetweenNodes = 40f;
        layout.layout(root);

        cont.add(new View()).grow();

        { //debug code; TODO remove
            ObjectSet<Block> used = new ObjectSet<Block>().select(t -> true);
            for(TechTreeNode node : nodes){
                used.add(node.node.block);
            }
            Array<Block> recipes = content.blocks().select(r -> r.isVisible() && !used.contains(r));
            recipes.sort(Structs.comparing(r -> r.buildCost));

            if(recipes.size > 0){
                Log.info("Recipe tree coverage: {0}%", (int)((float)nodes.size / content.blocks().select(Block::isVisible).size * 100));
                Log.info("Missing items: ");
                recipes.forEach(r -> Log.info("    {0}", r));
            }
        }

        shown(() -> checkNodes(root));
        addCloseButton();
    }

    @Override
    protected void drawBackground(float x, float y){
        drawDefaultBackground(x, y);
    }

    void checkNodes(TechTreeNode node){
        boolean locked = locked(node);
        if(!locked) node.visible = true;
        for(TreeNode child : node.children){
            TechTreeNode l = (TechTreeNode)child;
            l.visible = !locked && l.node.block.isVisible();
            checkNodes(l);
        }
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

    boolean locked(TreeNode node){
        return locked(((TechTreeNode)node).node);
    }

    boolean locked(TechNode node){
        return !data.isUnlocked(node.block);
    }

    class TechTreeNode extends TreeNode{
        final TechNode node;
        boolean visible = true;

        public TechTreeNode(TechNode node, TreeNode parent){
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
        Rectangle clip = new Rectangle();
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
                    }else if(data.hasItems(node.node.requirements) && locked(node)){
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
                    if(hoverNode == button && !infoTable.hasMouse() && !hoverNode.hasMouse()){
                        hoverNode = null;
                        rebuild();
                    }
                });
                button.touchable(() -> !node.visible ? Touchable.disabled : Touchable.enabled);
                button.setUserObject(node.node);
                button.tapped(() -> moved = false);
                button.setSize(nodeSize, nodeSize);
                button.update(() -> {
                    button.setPosition(node.x + panX + width/2f, node.y + panY + height/2f, Align.center);
                    button.getStyle().up = Core.scene.skin.getDrawable(!locked(node) ? "content-background" : "content-background-locked");
                    ((TextureRegionDrawable)button.getStyle().imageUp)
                        .setRegion(node.visible ? node.node.block.icon(Icon.medium) : Core.atlas.find("icon-tree-locked"));
                    button.getImage().setColor(!locked(node) ? Color.WHITE : Color.GRAY);
                });
                addChild(button);
            }

            addListener(new InputListener(){
                float lastX, lastY;
                @Override
                public void touchDragged(InputEvent event, float mx, float my, int pointer){
                    panX -= lastX - mx;
                    panY -= lastY - my;
                    lastX = mx;
                    lastY = my;
                    moved = true;
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    lastX = x;
                    lastY = y;
                    return true;
                }
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

            for(TreeNode node : nodes){
                for(TreeNode child : node.children){
                    Lines.stroke(3f, locked(node) || locked(child) ? Palette.locked : Palette.accent);

                    Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                }
            }

            super.draw();
        }
    }
}
