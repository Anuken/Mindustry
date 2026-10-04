package arc.struct;

/** @author Nathan Sweet */
@SuppressWarnings("unchecked")
public class BinaryHeap<T extends BinaryHeap.Node>{
    private final boolean isMaxHeap;
    public int size;
    private Node[] nodes;

    public BinaryHeap(){
        this(16, false);
    }

    public BinaryHeap(int capacity, boolean isMaxHeap){
        this.isMaxHeap = isMaxHeap;
        nodes = new Node[capacity];
    }

    public T add(T node){
        // Expand if necessary.
        if(size == nodes.length){
            Node[] newNodes = new Node[size << 1];
            System.arraycopy(nodes, 0, newNodes, 0, size);
            nodes = newNodes;
        }
        // Insert at end and bubble up.
        node.index = size;
        nodes[size] = node;
        up(size++);
        return node;
    }

    public T add(T node, float value){
        node.value = value;
        return add(node);
    }

    /**
     * Returns if binary heap contains the provided node.
     * @param node May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     */
    public boolean contains(T node, boolean identity){
        if(identity || node == null){
            for(Node n : nodes)
                if(n == node) return true;
        }else{
            for(Node n : nodes)
                if(n.equals(node)) return true;
        }
        return false;
    }

    public T peek(){
        if(size == 0) throw new IllegalStateException("The heap is empty.");
        return (T)nodes[0];
    }

    public T pop(){
        return remove(0);
    }

    public T remove(T node){
        return remove(node.index);
    }

    private T remove(int index){
        Node[] nodes = this.nodes;
        Node removed = nodes[index];
        nodes[index] = nodes[--size];
        nodes[size] = null;
        if(size > 0 && index < size) down(index);
        return (T)removed;
    }

    /** Returns true if the heap is empty. */
    public boolean isEmpty(){
        return size == 0;
    }

    public void clear(){
        Node[] nodes = this.nodes;
        for(int i = 0, n = size; i < n; i++)
            nodes[i] = null;
        size = 0;
    }

    public void setValue(T node, float value){
        float oldValue = node.value;
        node.value = value;
        if(value < oldValue ^ isMaxHeap)
            up(node.index);
        else
            down(node.index);
    }

    private void up(int index){
        Node[] nodes = this.nodes;
        Node node = nodes[index];
        float value = node.value;
        while(index > 0){
            int parentIndex = (index - 1) >> 1;
            Node parent = nodes[parentIndex];
            if(value < parent.value ^ isMaxHeap){
                nodes[index] = parent;
                parent.index = index;
                index = parentIndex;
            }else
                break;
        }
        nodes[index] = node;
        node.index = index;
    }

    private void down(int index){
        Node[] nodes = this.nodes;
        int size = this.size;

        Node node = nodes[index];
        float value = node.value;

        while(true){
            int leftIndex = 1 + (index << 1);
            if(leftIndex >= size) break;
            int rightIndex = leftIndex + 1;

            // Always have a left child.
            Node leftNode = nodes[leftIndex];
            float leftValue = leftNode.value;

            // May have a right child.
            Node rightNode;
            float rightValue;
            if(rightIndex >= size){
                rightNode = null;
                rightValue = isMaxHeap ? Float.MIN_VALUE : Float.MAX_VALUE;
            }else{
                rightNode = nodes[rightIndex];
                rightValue = rightNode.value;
            }

            // The smallest of the three values is the parent.
            if(leftValue < rightValue ^ isMaxHeap){
                if(leftValue == value || (leftValue > value ^ isMaxHeap)) break;
                nodes[index] = leftNode;
                leftNode.index = index;
                index = leftIndex;
            }else{
                if(rightValue == value || (rightValue > value ^ isMaxHeap)) break;
                nodes[index] = rightNode;
                rightNode.index = index;
                index = rightIndex;
            }
        }

        nodes[index] = node;
        node.index = index;
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof BinaryHeap)) return false;
        BinaryHeap other = (BinaryHeap)obj;
        if(other.size != size) return false;
        for(int i = 0, n = size; i < n; i++)
            if(other.nodes[i].value != nodes[i].value) return false;
        return true;
    }

    public int hashCode(){
        int h = 1;
        for(int i = 0, n = size; i < n; i++)
            h = h * 31 + Float.floatToIntBits(nodes[i].value);
        return h;
    }

    public String toString(){
        if(size == 0) return "[]";
        Node[] nodes = this.nodes;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(nodes[0].value);
        for(int i = 1; i < size; i++){
            buffer.append(", ");
            buffer.append(nodes[i].value);
        }
        buffer.append(']');
        return buffer.toString();
    }

    /** @author Nathan Sweet */
    public static class Node{
        float value;
        int index;

        public Node(float value){
            this.value = value;
        }

        public float getValue(){
            return value;
        }

        public String toString(){
            return Float.toString(value);
        }
    }
}
