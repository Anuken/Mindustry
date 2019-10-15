package io.anuke.mindustry.ui.layout;

import io.anuke.arc.collection.*;
import io.anuke.arc.math.*;

public class RadialTreeLayout implements TreeLayout{
    private static ObjectSet<TreeNode> visited = new ObjectSet<>();
    private static Queue<TreeNode> queue = new Queue<>();

    public float startRadius, delta;

    @Override
    public void layout(TreeNode root){
        startRadius = root.height * 2.4f;
        delta = root.height * 2.4f;

        bfs(root, true);
        radialize(root, 0, 360);
    }

    void radialize(TreeNode root, float from, float to){
        int depthOfVertex = root.number;
        float theta = from;
        float radius = startRadius + (delta * depthOfVertex);

        int leavesNumber = bfs(root, false);
        for(TreeNode child : root.children){
            int lambda = bfs(child, false);
            float mi = theta + ((float)lambda / leavesNumber * (to - from));

            float x = radius * Mathf.cos((theta + mi) / 2f * Mathf.degRad);
            float y = radius * Mathf.sin((theta + mi) / 2f * Mathf.degRad);

            child.x = x;
            child.y = y;

            if(child.children.length > 0) radialize(child, theta, mi);
            theta = mi;
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
                if(!visited.contains(child)){
                    visited.add(child);
                    queue.addLast(child);
                }
            }
        }

        return leaves;
    }
}
