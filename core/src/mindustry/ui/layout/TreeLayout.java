package mindustry.ui.layout;

public interface TreeLayout{
    void layout(TreeNode root);

    class TreeNode<T extends TreeNode>{
        public float width, height, x, y;

        //should be initialized by user
        public T[] children;
        public T parent;

        //internal stuff
        public float mode, prelim, change, shift, cachedWidth = -1f;
        public int number = -1, leaves;
        public TreeNode thread, ancestor;

        public boolean isLeaf(){
            return children == null || children.length == 0;
        }

        public float calcWidth(){
            if(children == null) return width;
            if(cachedWidth > 0) return cachedWidth;

            float cWidth = 0;
            for(T node : children){
                cWidth += node.calcWidth();
            }
            return cachedWidth = Math.max(width, cWidth);
        }
    }
}
