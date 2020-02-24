package mindustry.ui.layout;

import arc.struct.*;
import arc.math.*;

public class RadialTreeLayout implements TreeLayout{
    private static ObjectSet<TreeNode> visited = new ObjectSet<>();
    private static Queue<TreeNode> queue = new Queue<>();

    public float startRadius, delta;

    @Override
    public void layout(TreeNode root){
        startRadius = root.height * 2.4f;
        delta = root.height * 20.4f;

        bfs(root, true);

        ObjectSet<TreeNode> all = new ObjectSet<>(visited);
        for(TreeNode node : all){
            node.leaves = bfs(node, false);
        }

        radialize(root, startRadius, 0, 360);
    }

    void radialize(TreeNode root, float radius, float from, float to){
        float angle = from;

        for(TreeNode child : root.children){
            float nextAngle = angle + ((float)child.leaves / root.leaves * (to - from));

            float x = radius * Mathf.cos((angle + nextAngle) / 2f * Mathf.degRad);
            float y = radius * Mathf.sin((angle + nextAngle) / 2f * Mathf.degRad);

            child.x = x;
            child.y = y;

            if(child.children.length > 0) radialize(child, radius + delta, angle, nextAngle);
            angle = nextAngle;
        }
    }

    int bfs(TreeNode node, boolean assign){
        visited.clear();
        queue.clear();
        if(assign) node.number = 0;
        int leaves = 0;

        visited.add(node);
        queue.addFirst(node);

        while(!queue.isEmpty()){
            TreeNode current = queue.removeFirst();
            if(current.children.length == 0) leaves++;

            for(TreeNode child : current.children){
                if(assign) child.number = current.number + 1;
                if(visited.add(child)){
                    queue.addLast(child);
                }
            }
        }

        return leaves;
    }
}
