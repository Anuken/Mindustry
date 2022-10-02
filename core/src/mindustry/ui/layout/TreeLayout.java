package mindustry.ui.layout;

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

        public float calcWidth(int depth){
            if(children == null || depth == 0) return width;

            float cWidth = 0;
            for(T node : children){
                cWidth += node.calcWidth(depth - 1);
            }
            return Math.max(width, cWidth);
        }

        public int countDepth(){
            if(children == null) return 0;

            int depth = 0;
            for(T node : children){
                depth = Math.max(depth, node.countDepth() + 1);
            }
            return depth;
        }
    }
}
