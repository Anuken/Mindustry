package mindustry.ai;

import arc.util.*;

/** A priority queue. */
@SuppressWarnings("unchecked")
public class PathfindQueue{
    private static final double CAPACITY_RATIO_LOW = 1.5f;
    private static final double CAPACITY_RATIO_HI = 2f;

    /**
     * Priority queue represented as a balanced binary heap: the two children of queue[n] are queue[2*n+1] and queue[2*(n+1)]. The
     * priority queue is ordered by the elements' natural ordering: For each node n in the heap and each descendant d of n, n <= d.
     * The element with the lowest value is in queue[0], assuming the queue is nonempty.
     */
    public int[] queue;
    /** Weights of each object in the queue. */
    public float[] weights;
    /** The number of elements in the priority queue. */
    public int size = 0;

    public PathfindQueue(){
        this(12);
    }

    public PathfindQueue(int initialCapacity){
        this.queue = new int[initialCapacity];
        this.weights = new float[initialCapacity];
    }

    public boolean empty(){
        return size == 0;
    }

    /**
     * Inserts the specified element into this priority queue. If {@code uniqueness} is enabled and this priority queue already
     * contains the element, the call leaves the queue unchanged and returns false.
     * @return true if the element was added to this queue, else false
     * @throws ClassCastException if the specified element cannot be compared with elements currently in this priority queue
     * according to the priority queue's ordering
     * @throws IllegalArgumentException if the specified element is null
     */
    public boolean add(int e, float weight){
        int i = size;
        if(i >= queue.length) growToSize(i + 1);
        size = i + 1;
        if(i == 0){
            queue[0] = e;
            weights[0] = weight;
        }else{
            siftUp(i, e, weight);
        }
        return true;
    }

    /**
     * Retrieves, but does not remove, the head of this queue. If this queue is empty, {@code 0} is returned.
     * @return the head of this queue
     */
    public int peek(){
        return size == 0 ? 0 : queue[0];
    }

    /** Removes all of the elements from this priority queue. The queue will be empty after this call returns. */
    public void clear(){
        size = 0;
    }

    /**
     * Retrieves and removes the head of this queue, or returns {@code null} if this queue is empty.
     * @return the head of this queue, or {@code null} if this queue is empty.
     */
    public int poll(){
        if(size == 0) return 0;
        int s = --size;
        int result = queue[0];
        int x = queue[s];
        if(s != 0) siftDown(0, x, weights[s]);
        return result;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by promoting x up the tree until it is greater than or equal to its
     * parent, or is the root.
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftUp(int k, int x, float weight){
        while(k > 0){
            int parent = (k - 1) >>> 1;
            int e = queue[parent];
            if(weight >= weights[parent]) break;
            queue[k] = e;
            weights[k] = weights[parent];
            k = parent;
        }
        queue[k] = x;
        weights[k] = weight;
    }

    /**
     * Inserts item x at position k, maintaining heap invariant by demoting x down the tree repeatedly until it is less than or
     * equal to its children or is a leaf.
     * @param k the position to fill
     * @param x the item to insert
     */
    private void siftDown(int k, int x, float weight){
        int half = size >>> 1; // loop while a non-leaf
        while(k < half){
            int child = (k << 1) + 1; // assume left child is least
            int c = queue[child];
            int right = child + 1;
            if(right < size && weights[child] > weights[right]){
                c = queue[child = right];
            }
            if(weight <= weights[child]) break;
            queue[k] = c;
            weights[k] = weights[child];
            k = child;
        }
        queue[k] = x;
        weights[k] = weight;
    }

    /**
     * Increases the capacity of the array.
     * @param minCapacity the desired minimum capacity
     */
    private void growToSize(int minCapacity){
        if(minCapacity < 0) // overflow
            throw new ArcRuntimeException("Capacity upper limit exceeded.");
        int oldCapacity = queue.length;
        // Double size if small; else grow by 50%
        int newCapacity = (int)((oldCapacity < 64) ? ((oldCapacity + 1) * CAPACITY_RATIO_HI) : (oldCapacity * CAPACITY_RATIO_LOW));
        if(newCapacity < 0) // overflow
            newCapacity = Integer.MAX_VALUE;
        if(newCapacity < minCapacity) newCapacity = minCapacity;

        int[] newQueue = new int[newCapacity];
        float[] newWeights = new float[newCapacity];
        System.arraycopy(queue, 0, newQueue, 0, size);
        System.arraycopy(weights, 0, newWeights, 0, size);
        queue = newQueue;
        weights = newWeights;
    }
}