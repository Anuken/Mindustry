package arc.struct;

import java.util.Comparator;

/**
 * Guarantees that array entries provided by {@link #begin()} between indexes 0 and {@link #size} at the time begin was called
 * will not be modified until {@link #end()} is called. If modification of the SnapshotArray occurs between begin/end, the backing
 * array is copied prior to the modification, ensuring that the backing array that was returned by {@link #begin()} is unaffected.
 * To avoid allocation, an attempt is made to reuse any extra array created as a result of this copy on subsequent copies.
 * <p>
 * It is suggested iteration be done in this specific way:
 *
 * <pre>
 * SnapshotArray array = new SnapshotArray();
 * // ...
 * Object[] items = array.begin();
 * for (int i = 0, n = array.size; i &lt; n; i++) {
 * 	Object item = items[i];
 * 	// ...
 * }
 * array.end();
 * </pre>
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class SnapshotSeq<T> extends Seq<T>{
    private T[] snapshot, recycled;
    private int snapshots;

    public SnapshotSeq(){
        super();
    }

    public SnapshotSeq(Seq<T> array){
        super(array);
    }

    public SnapshotSeq(boolean ordered, int capacity, Class<?> arrayType){
        super(ordered, capacity, arrayType);
    }

    public SnapshotSeq(boolean ordered, int capacity){
        super(ordered, capacity);
    }

    public SnapshotSeq(boolean ordered, T[] array, int startIndex, int count){
        super(ordered, array, startIndex, count);
    }

    public SnapshotSeq(Class<?> arrayType){
        super(arrayType);
    }

    public SnapshotSeq(int capacity){
        super(capacity);
    }

    public SnapshotSeq(T[] array){
        super(array);
    }

    /** @see #SnapshotSeq(Object[]) */
    public static <T> SnapshotSeq<T> with(T... array){
        return new SnapshotSeq<>(array);
    }

    /** Returns the backing array, which is guaranteed to not be modified before {@link #end()}. */
    public T[] begin(){
        modified();
        snapshot = items;
        snapshots++;
        return items;
    }

    /** Releases the guarantee that the array returned by {@link #begin()} won't be modified. */
    public void end(){
        snapshots = Math.max(0, snapshots - 1);
        if(snapshot == null) return;
        if(snapshot != items && snapshots == 0){
            // The backing array was copied, keep around the old array.
            recycled = snapshot;
            for(int i = 0, n = recycled.length; i < n; i++)
                recycled[i] = null;
        }
        snapshot = null;
    }

    private void modified(){
        if(snapshot == null || snapshot != items) return;
        // Snapshot is in use, copy backing array to recycled array or create new backing array.
        if(recycled != null && recycled.length >= size){
            System.arraycopy(items, 0, recycled, 0, size);
            items = recycled;
            recycled = null;
        }else
            resize(items.length);
    }

    public void set(int index, T value){
        modified();
        super.set(index, value);
    }

    public void insert(int index, T value){
        modified();
        super.insert(index, value);
    }

    public void swap(int first, int second){
        modified();
        super.swap(first, second);
    }

    public boolean remove(T value, boolean identity){
        modified();
        return super.remove(value, identity);
    }

    public T remove(int index){
        modified();
        return super.remove(index);
    }

    public void removeRange(int start, int end){
        modified();
        super.removeRange(start, end);
    }

    public boolean removeAll(Seq<? extends T> array, boolean identity){
        modified();
        return super.removeAll(array, identity);
    }

    public T pop(){
        modified();
        return super.pop();
    }

    public Seq<T> clear(){
        modified();
        super.clear();
        return this;
    }

    public Seq<T> sort(){
        modified();
        return super.sort();
    }

    public Seq<T> sort(Comparator<? super T> comparator){
        modified();
        return super.sort(comparator);
    }

    public Seq<T> reverse(){
        modified();
        return super.reverse();
    }

    public Seq<T> shuffle(){
        modified();
        return super.shuffle();
    }

    public void truncate(int newSize){
        modified();
        super.truncate(newSize);
    }

    public T[] setSize(int newSize){
        modified();
        return super.setSize(newSize);
    }
}
