package mindustry.ui.layout;

import arc.struct.*;
import arc.math.geom.*;

/**
 * Algorithm taken from <a href="https://github.com/abego/treelayout">TreeLayout</a>.
 */
public class BranchTreeLayout implements TreeLayout{
    public TreeLocation rootLocation = TreeLocation.top;
    public TreeAlignment alignment = TreeAlignment.awayFromRoot;
    public float gapBetweenLevels = 10;
    public float gapBetweenNodes = 10f;

    private final FloatSeq sizeOfLevel = new FloatSeq();
    private float boundsLeft = Float.MAX_VALUE;
    private float boundsRight = Float.MIN_VALUE;
    private float boundsTop = Float.MAX_VALUE;
    private float boundsBottom = Float.MIN_VALUE;

    @Override
    public void layout(TreeNode root){
        firstWalk(root, null);
        calcSizeOfLevels(root, 0);
        secondWalk(root, -root.prelim, 0, 0);
    }

    private float getWidthOrHeightOfNode(TreeNode treeNode, boolean returnWidth){
        return returnWidth ? treeNode.width : treeNode.height;
    }

    private float getNodeThickness(TreeNode treeNode){
        return getWidthOrHeightOfNode(treeNode, !isLevelChangeInYAxis());
    }

    private float getNodeSize(TreeNode treeNode){
        return getWidthOrHeightOfNode(treeNode, isLevelChangeInYAxis());
    }

    private boolean isLevelChangeInYAxis(){
        return rootLocation == TreeLocation.top || rootLocation == TreeLocation.bottom;
    }

    private int getLevelChangeSign(){
        return rootLocation == TreeLocation.bottom || rootLocation == TreeLocation.right ? -1 : 1;
    }

    private void updateBounds(TreeNode node, float centerX, float centerY){
        float width = node.width;
        float height = node.height;
        float left = centerX - width / 2;
        float right = centerX + width / 2;
        float top = centerY - height / 2;
        float bottom = centerY + height / 2;
        if(boundsLeft > left){
            boundsLeft = left;
        }
        if(boundsRight < right){
            boundsRight = right;
        }
        if(boundsTop > top){
            boundsTop = top;
        }
        if(boundsBottom < bottom){
            boundsBottom = bottom;
        }
    }

    public Rect getBounds(){
        return new Rect(boundsLeft, boundsBottom, boundsRight - boundsLeft, boundsTop - boundsBottom);
    }

    private void calcSizeOfLevels(TreeNode node, int level){
        float oldSize;
        if(sizeOfLevel.size <= level){
            sizeOfLevel.add(0);
            oldSize = 0;
        }else{
            oldSize = sizeOfLevel.get(level);
        }

        float size = getNodeThickness(node);
        if(oldSize < size){
            sizeOfLevel.set(level, size);
        }

        if(!node.isLeaf()){
            for(TreeNode child : node.children){
                calcSizeOfLevels(child, level + 1);
            }
        }
    }

    public int getLevelCount(){
        return sizeOfLevel.size;
    }

    public float getGapBetweenNodes(TreeNode a, TreeNode b){
        return gapBetweenNodes;
    }

    public float getSizeOfLevel(int level){
        if(!(level >= 0)) throw new IllegalArgumentException("level must be >= 0");
        if(!(level < getLevelCount())) throw new IllegalArgumentException("level must be < levelCount");

        return sizeOfLevel.get(level);
    }

    private TreeNode getAncestor(TreeNode node){
        return node.ancestor != null ? node.ancestor : node;
    }

    private float getDistance(TreeNode v, TreeNode w){
        float sizeOfNodes = getNodeSize(v) + getNodeSize(w);

        return sizeOfNodes / 2 + getGapBetweenNodes(v, w);
    }

    private TreeNode nextLeft(TreeNode v){
        return v.isLeaf() ? v.thread : v.children[0];
    }

    private TreeNode nextRight(TreeNode v){
        return v.isLeaf() ? v.thread : v.children[v.children.length - 1];
    }

    private int getNumber(TreeNode node, TreeNode parentNode){
        if(node.number == -1){
            int number = 1;
            for(TreeNode child : parentNode.children){
                child.number = number++;
            }
        }
        return node.number;
    }

    private TreeNode ancestor(TreeNode vIMinus, TreeNode parentOfV, TreeNode defaultAncestor){
        TreeNode ancestor = getAncestor(vIMinus);
        return ancestor.parent == parentOfV ? ancestor : defaultAncestor;
    }

    private void moveSubtree(TreeNode wMinus, TreeNode wPlus, TreeNode parent, float shift){
        int subtrees = getNumber(wPlus, parent) - getNumber(wMinus, parent);
        wPlus.change = wPlus.change - shift / subtrees;
        wPlus.shift = wPlus.shift + shift;
        wMinus.change = wMinus.change + shift / subtrees;
        wPlus.prelim = wPlus.prelim + shift;
        wPlus.mode = wPlus.mode + shift;
    }

    private TreeNode apportion(TreeNode v, TreeNode defaultAncestor, TreeNode leftSibling, TreeNode parentOfV){
        if(leftSibling == null){
            return defaultAncestor;
        }

        TreeNode vOPlus = v;
        TreeNode vIPlus = v;
        TreeNode vIMinus = leftSibling;

        TreeNode vOMinus = parentOfV.children[0];

        float sIPlus = (vIPlus).mode;
        float sOPlus = (vOPlus).mode;
        float sIMinus = (vIMinus).mode;
        float sOMinus = (vOMinus).mode;

        TreeNode nextRightVIMinus = nextRight(vIMinus);
        TreeNode nextLeftVIPlus = nextLeft(vIPlus);

        while(nextRightVIMinus != null && nextLeftVIPlus != null){
            vIMinus = nextRightVIMinus;
            vIPlus = nextLeftVIPlus;
            vOMinus = nextLeft(vOMinus);
            vOPlus = nextRight(vOPlus);
            vOPlus.ancestor = v;
            float shift = (vIMinus.prelim + sIMinus)
            - (vIPlus.prelim + sIPlus)
            + getDistance(vIMinus, vIPlus);

            if(shift > 0){
                moveSubtree(ancestor(vIMinus, parentOfV, defaultAncestor),
                v, parentOfV, shift);
                sIPlus = sIPlus + shift;
                sOPlus = sOPlus + shift;
            }
            sIMinus += vIMinus.mode;
            sIPlus += vIPlus.mode;
            sOMinus += vOMinus.mode;
            sOPlus += vOPlus.mode;

            nextRightVIMinus = nextRight(vIMinus);
            nextLeftVIPlus = nextLeft(vIPlus);
        }

        if(nextRightVIMinus != null && nextRight(vOPlus) == null){
            vOPlus.thread = nextRightVIMinus;
            vOPlus.mode += sIMinus - sOPlus;
        }

        if(nextLeftVIPlus != null && nextLeft(vOMinus) == null){
            vOMinus.thread = nextLeftVIPlus;
            vOMinus.mode += sIPlus - sOMinus;
            defaultAncestor = v;
        }
        return defaultAncestor;
    }

    private void executeShifts(TreeNode v){
        float shift = 0;
        float change = 0;

        for(int i = v.children.length - 1; i >= 0; i--){
            TreeNode w = v.children[i];
            change = change + w.change;
            w.prelim += shift;
            w.mode += shift;
            shift += w.shift + change;
        }
    }

    private void firstWalk(TreeNode v, TreeNode leftSibling){
        if(v.isLeaf()){
            if(leftSibling != null){
                v.prelim = leftSibling.prelim + getDistance(v, leftSibling);
            }

        }else{
            TreeNode defaultAncestor = v.children[0];
            TreeNode previousChild = null;
            for(TreeNode w : v.children){
                firstWalk(w, previousChild);
                defaultAncestor = apportion(w, defaultAncestor, previousChild, v);
                previousChild = w;
            }
            executeShifts(v);
            float midpoint = (v.children[0].prelim + v.children[v.children.length - 1].prelim) / 2f;
            if(leftSibling != null){
                v.prelim = leftSibling.prelim + getDistance(v, leftSibling);
                v.mode = v.prelim - midpoint;
            }else{
                v.prelim = midpoint;
            }
        }
    }

    private void secondWalk(TreeNode v, float m, int level, float levelStart){
        float levelChangeSign = getLevelChangeSign();
        boolean levelChangeOnYAxis = isLevelChangeInYAxis();
        float levelSize = getSizeOfLevel(level);

        float x = v.prelim + m;

        float y;
        if(alignment == TreeAlignment.center){
            y = levelStart + levelChangeSign * (levelSize / 2);
        }else if(alignment == TreeAlignment.towardsRoot){
            y = levelStart + levelChangeSign * (getNodeThickness(v) / 2);
        }else{
            y = levelStart + levelSize - levelChangeSign * (getNodeThickness(v) / 2);
        }

        if(!levelChangeOnYAxis){
            float t = x;
            x = y;
            y = t;
        }

        v.x = x;
        v.y = y;
        updateBounds(v, x, y);

        if(!v.isLeaf()){
            float nextLevelStart = levelStart
            + (levelSize + gapBetweenLevels)
            * levelChangeSign;
            for(TreeNode w : v.children){
                secondWalk(w, m + v.mode, level + 1, nextLevelStart);
            }
        }
    }

    public enum TreeLocation{
        top, left, bottom, right
    }

    public enum TreeAlignment{
        center, towardsRoot, awayFromRoot
    }
}
