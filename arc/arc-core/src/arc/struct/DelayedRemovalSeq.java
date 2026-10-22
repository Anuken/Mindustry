package arc.struct;

import java.util.Comparator;

/**
 * Queues any removals done after {@link #begin()} is called to occur once {@link #end()} is called. This can allow code out of
 * your control to remove items without affecting iteration. Between begin and end, most mutator methods will throw
 * IllegalStateException. Only {@link #remove(int)}, {@link #remove(Object, boolean)}, {@link #removeRange(int, int)},
 * {@link #clear()}, and add methods are allowed.
 * <p>
 * Code using this class must not rely on items being removed immediately. Consider using {@link SnapshotSeq} if this is a
 * problem.
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class DelayedRemovalSeq<T> extends Seq<T>{
    private int iterating;
    private final IntSeq remove = new IntSeq(0);
    private int clear;

    public DelayedRemovalSeq(){
        super();
    }

    public DelayedRemovalSeq(Seq<? extends T> array){
        super(array);
    }

    public DelayedRemovalSeq(boolean ordered, int capacity, Class<?> arrayType){
        super(ordered, capacity, arrayType);
    }

    public DelayedRemovalSeq(boolean ordered, int capacity){
        super(ordered, capacity);
    }

    public DelayedRemovalSeq(boolean ordered, T[] array, int startIndex, int count){
        super(ordered, array, startIndex, count);
    }

    public DelayedRemovalSeq(Class<?> arrayType){
        super(arrayType);
    }

    public DelayedRemovalSeq(int capacity){
        super(capacity);
    }

    public DelayedRemovalSeq(T[] array){
        super(array);
    }

    /** @see #DelayedRemovalSeq(Object[]) */
    public static <T> DelayedRemovalSeq<T> with(T... array){
        return new DelayedRemovalSeq<>(array);
    }

    public void begin(){
        iterating++;
    }

    public void end(){
        if(iterating == 0) throw new IllegalStateException("begin must be called before end.");
        iterating--;
        if(iterating == 0){
            if(clear > 0 && clear == size){
                remove.clear();
                clear();
            }else{
                for(int i = 0, n = remove.size; i < n; i++){
                    int index = remove.pop();
                    if(index >= clear) remove(index);
                }
                for(int i = clear - 1; i >= 0; i--)
                    remove(i);
            }
            clear = 0;
        }
    }

    private void removeIntern(int index){
        if(index < clear) return;
        for(int i = 0, n = remove.size; i < n; i++){
            int removeIndex = remove.get(i);
            if(index == removeIndex) return;
            if(index < removeIndex){
                remove.insert(i, index);
                return;
            }
        }
        remove.add(index);
    }

    public boolean remove(T value, boolean identity){
        if(iterating > 0){
            int index = indexOf(value, identity);
            if(index == -1) return false;
            removeIntern(index);
            return true;
        }
        return super.remove(value, identity);
    }

    public T remove(int index){
        if(iterating > 0){
            removeIntern(index);
            return get(index);
        }
        return super.remove(index);
    }

    public void removeRange(int start, int end){
        if(iterating > 0){
            for(int i = end; i >= start; i--)
                removeIntern(i);
        }else
            super.removeRange(start, end);
    }

    public Seq<T> clear(){
        if(iterating > 0){
            clear = size;
            return this;
        }
        return super.clear();
    }

    public void set(int index, T value){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        super.set(index, value);
    }

    public void insert(int index, T value){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        super.insert(index, value);
    }

    public void swap(int first, int second){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        super.swap(first, second);
    }

    public T pop(){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        return super.pop();
    }

    public Seq<T> sort(){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        return super.sort();
    }

    public Seq<T> sort(Comparator<? super T> comparator){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        return super.sort(comparator);
    }

    public Seq<T> reverse(){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        return super.reverse();
    }

    public Seq<T> shuffle(){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        return super.shuffle();
    }

    public void truncate(int newSize){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        super.truncate(newSize);
    }

    public T[] setSize(int newSize){
        if(iterating > 0) throw new IllegalStateException("Invalid between begin/end.");
        return super.setSize(newSize);
    }
}
