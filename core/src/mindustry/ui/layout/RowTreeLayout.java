package mindustry.ui.layout;

import arc.struct.*;

public class RowTreeLayout implements TreeLayout{

    @Override
    public void layout(TreeNode root){
       layout(root, 0, new IntSeq());

        /*
        def minimum_ws(tree, depth=0):
    tree.x = nexts[depth]
    tree.y = depth
    nexts[depth] += 1
    for c in tree.children:
        minimum_ws(tree, c)
         */
    }

    void layout(TreeNode node, int depth, IntSeq nexts){
        float size = node.height * 5f;

        if(nexts.size < depth + 1){
            nexts.ensureCapacity(depth + 1);
            nexts.size = depth + 1;
        }

        node.x = size * nexts.get(depth);
        node.y = size * depth;
        nexts.incr(depth, 1);
        for(TreeNode child : node.children){
            layout(child, depth + 1, nexts);
        }
    }
}
