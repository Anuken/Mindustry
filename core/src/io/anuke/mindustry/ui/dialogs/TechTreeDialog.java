package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.collection.Array;
import io.anuke.arc.collection.ObjectSet;
import io.anuke.arc.graphics.g2d.Draw;
import io.anuke.arc.graphics.g2d.Lines;
import io.anuke.arc.graphics.g2d.ScissorStack;
import io.anuke.arc.graphics.g2d.TextureRegion;
import io.anuke.arc.input.KeyCode;
import io.anuke.arc.math.geom.Rectangle;
import io.anuke.arc.scene.Element;
import io.anuke.arc.scene.event.InputEvent;
import io.anuke.arc.scene.event.InputListener;
import io.anuke.arc.util.Log;
import io.anuke.arc.util.Structs;
import io.anuke.mindustry.content.Blocks;
import io.anuke.mindustry.content.TechTree;
import io.anuke.mindustry.content.TechTree.TechNode;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.type.Recipe;
import io.anuke.mindustry.type.Recipe.RecipeVisibility;
import io.anuke.mindustry.ui.TreeLayout;
import io.anuke.mindustry.ui.TreeLayout.TreeNode;
import io.anuke.mindustry.world.Block.Icon;

import static io.anuke.mindustry.Vars.content;

public class TechTreeDialog extends FloatingDialog{
    private ObjectSet<TechTreeNode> nodes = new ObjectSet<>();
    private static final float nodeSize = 60f;

    public TechTreeDialog(){
        super("$techtree");

        TreeLayout layout = new TreeLayout();
        layout.gapBetweenLevels = 60f;
        layout.gapBetweenNodes = 40f;
        layout.layout(new TechTreeNode(TechTree.root, null));
        cont.add(new View()).grow();

        { //debug code
            ObjectSet<Recipe> used = new ObjectSet<>();
            for(TechTreeNode node : nodes){
                if(node.node.block != null) used.add(Recipe.getByResult(node.node.block));
            }
            Array<Recipe> recipes = content.recipes().select(r -> r.visibility == RecipeVisibility.all && !used.contains(r));
            recipes.sort(Structs.comparing(r -> r.cost));

            if(recipes.size > 0){
                Log.info("Recipe tree coverage: {0}%", (int)((float)nodes.size / content.recipes().select(r -> r.visibility == RecipeVisibility.all).size * 100));
                Log.info("Missing items: ");
                recipes.forEach(r -> Log.info("    {0}", r));
            }
        }

        addCloseButton();
    }

    class TechTreeNode extends TreeNode{
        final TechNode node;

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

    class View extends Element{
        float panX = 0, panY = 0;
        Rectangle clip = new Rectangle();

        {
            addListener(new InputListener(){
                float lastX, lastY;
                @Override
                public void touchDragged(InputEvent event, float mx, float my, int pointer){
                    panX -= lastX - mx;
                    panY -= lastY - my;
                    lastX = mx;
                    lastY = my;
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
                    lastX = x;
                    lastY = y;
                    return true;
                }
            });
        }

        @Override
        public void draw(){
            if(!ScissorStack.pushScissors(clip.set(x, y, width, height))){
                return;
            }

            float offsetX = panX + width/2f + x, offsetY = panY + height/2f + y;

            Lines.stroke(3f, Palette.accent);

            for(TreeNode node : nodes){
                for(TreeNode child : node.children){
                    Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                }
            }

            Draw.color();

            for(TechTreeNode node : nodes){
                Draw.drawable("content-background", node.x + offsetX - nodeSize/2f, node.y + offsetY - nodeSize/2f, nodeSize, nodeSize);

                TextureRegion region = node.node.block == null ? Blocks.core.icon(Icon.medium) : node.node.block.icon(Icon.medium);
                Draw.rect(region, node.x + offsetX, node.y + offsetY - 0.5f, region.getWidth(), region.getHeight());
            }

            ScissorStack.popScissors();
        }
    }
}
