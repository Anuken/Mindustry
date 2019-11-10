package io.anuke.mindustry.ui.layout;

public interface TreeLayout{
    void layout(TreeNode root);

    class TreeNode<T extends TreeNode>{
        public float width, height, x, y;

        //should be initialized by user
        public T[] children;
        public T parent;

        //internal stuff
        public float mode, prelim, change, shift;
        public int number = -1, leaves;
        public TreeNode thread, ancestor;

        public boolean isLeaf(){
            return children == null || children.length == 0;
        }
    }
}
