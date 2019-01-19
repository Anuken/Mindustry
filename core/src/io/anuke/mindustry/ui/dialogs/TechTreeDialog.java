package io.anuke.mindustry.ui.dialogs;

import io.anuke.arc.Core;
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
import io.anuke.mindustry.content.TechTree;
import io.anuke.mindustry.content.TechTree.TechNode;
import io.anuke.mindustry.game.Content;
import io.anuke.mindustry.game.UnlockableContent;
import io.anuke.mindustry.graphics.Palette;
import io.anuke.mindustry.ui.TreeLayout;
import io.anuke.mindustry.ui.TreeLayout.TreeNode;
import io.anuke.mindustry.world.Block;

public class TechTreeDialog extends FloatingDialog{
    private TreeLayout layout;
    private ObjectSet<TechTreeNode> nodes = new ObjectSet<>();

    public TechTreeDialog(){
        super("$techtree");

        layout = new TreeLayout();
        layout.gapBetweenLevels = 60f;
        layout.gapBetweenNodes = 40f;
        layout.layout(new TechTreeNode(TechTree.root, null));

        cont.add(new View()).grow();

        addCloseButton();
    }

    class TechTreeNode extends TreeNode{
        final TechNode node;

        public TechTreeNode(TechNode node, TreeNode parent){
            this.node = node;
            this.parent = parent;
            this.width = this.height = 60f;
            nodes.add(this);
            if(node.children != null){
                children = new TechTreeNode[node.children.length];
                for(int i = 0; i < children.length; i++){
                    children[i] = new TechTreeNode(node.children[i], this);
                }
            }
        }
    }

    class View extends Element{
        float panX = 0, panY = 0;
        Rectangle clip = new Rectangle();

        {
            addListener(new InputListener(){
                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer){
                    super.touchDragged(event, x, y, pointer);
                    panX += Core.input.deltaX(pointer);
                    panY += Core.input.deltaY(pointer);
                }

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button){
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
                Draw.drawable("content-background", node.x + offsetX - node.width/2f, node.y + offsetY - node.height/2f, node.width, node.height);

                Content content = node.node.content;
                TextureRegion region = content instanceof Block ? ((Block)content).getEditorIcon() :
                                                                  ((UnlockableContent)content).getContentIcon();
                Draw.rect(region, node.x + offsetX, node.y + offsetY, 8*3, 8*3);
            }

            ScissorStack.popScissors();
        }
    }
}
