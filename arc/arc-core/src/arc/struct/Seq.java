package arc.struct;

import arc.func.*;
import arc.math.*;
import arc.util.*;

import java.util.*;

/**
 * A resizable, ordered or unordered array of objects. If unordered, this class avoids a memory copy when removing elements (the
 * last element is moved to the removed element's position).
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class Seq<T> implements Iterable<T>, Eachable<T>{
    /** Debugging variable to count total number of iterators allocated. */
    public static int iteratorsAllocated = 0;
    /**
     * Provides direct access to the underlying array. If the Array's generic type is not Object, this field may only be accessed
     * if the {@link Seq#Seq(boolean, int, Class)} constructor was used.
     */
    public T[] items;

    public int size;
    public boolean ordered;

    private @Nullable SeqIterable<T> iterable;

    /** Creates an ordered array with a capacity of 16. */
    public Seq(){
        this(true, 16);
    }

    /** Creates an ordered array with the specified capacity. */
    public Seq(int capacity){
        this(true, capacity);
    }

    /** Creates an ordered/unordered array with the specified capacity. */
    public Seq(boolean ordered){
        this(ordered, 16);
    }

    /**
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     */
    public Seq(boolean ordered, int capacity){
        this.ordered = ordered;
        items = (T[])new Object[capacity];
    }

    /**
     * Creates a new array with {@link #items} of the specified type.
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     */
    public Seq(boolean ordered, int capacity, Class<?> arrayType){
        this.ordered = ordered;
        items = (T[])java.lang.reflect.Array.newInstance(arrayType, capacity);
    }

    /** Creates an ordered array with {@link #items} of the specified type and a capacity of 16. */
    public Seq(Class<?> arrayType){
        this(true, 16, arrayType);
    }

    /**
     * Creates a new array containing the elements in the specified array. The new array will have the same type of backing array
     * and will be ordered if the specified array is ordered. The capacity is set to the number of elements, so any subsequent
     * elements added will cause the backing array to be grown.
     */
    public Seq(Seq<? extends T> array){
        this(array.ordered, array.size, array.items.getClass().getComponentType());
        size = array.size;
        System.arraycopy(array.items, 0, items, 0, size);
    }

    /**
     * Creates a new ordered array containing the elements in the specified array. The new array will have the same type of
     * backing array. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array
     * to be grown.
     */
    public Seq(T[] array){
        this(true, array, 0, array.length);
    }

    /**
     * Creates a new array containing the elements in the specified array. The new array will have the same type of backing array.
     * The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be grown.
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     */
    public Seq(boolean ordered, T[] array, int start, int count){
        this(ordered, count, array.getClass().getComponentType());
        size = count;
        System.arraycopy(array, start, items, 0, size);
    }

    public static <T> Seq<T> withArrays(Object... arrays){
        Seq<T> result = new Seq<>();
        for(Object a : arrays){
            if(a instanceof Seq){
                result.addAll((Seq<? extends T>)a);
            }else{
                result.add((T)a);
            }
        }
        return result;
    }

    /** @see #Seq(Object[]) */
    public static <T> Seq<T> with(T... array){
        return new Seq<>(array);
    }

    public static <T> Seq<T> with(Iterable<T> array){
        Seq<T> out = new Seq<>();
        for(T thing : array){
            out.add(thing);
        }
        return out;
    }

    public static <T, V> Seq<V> map(T[] array, Func<T, V> mapper){
        Seq<V> result = new Seq<>(array.length);
        for(int i = 0; i < array.length; i++){
            result.add(mapper.get(array[i]));
        }
        return result;
    }

    /** @see #Seq(Object[]) */
    public static <T> Seq<T> select(T[] array, Boolf<T> test){
        Seq<T> out = new Seq<>(array.length);
        for(T t : array){
            if(test.get(t)){
                out.add(t);
            }
        }
        return out;
    }

    public <K, V> ObjectMap<K, V> asMap(Func<T, K> keygen, Func<T, V> valgen){
        ObjectMap<K, V> map = new ObjectMap<>();
        for(int i = 0; i < size; i++){
            map.put(keygen.get(items[i]), valgen.get(items[i]));
        }
        return map;
    }

    public <K> ObjectMap<K, Seq<T>> groupBy(Func<T, K> keygen){
        ObjectMap<K, Seq<T>> map = new ObjectMap<>();
        for(int i = 0; i < size; i++){
            T item = items[i];
            map.get(keygen.get(item), Seq::new).add(item);
        }
        return map;
    }

    public <K> ObjectIntMap<K> groupByCount(Func<T, K> keygen){
        ObjectIntMap<K> map = new ObjectIntMap<>();
        for(int i = 0; i < size; i++){
            map.increment(keygen.get(items[i]));
        }
        return map;
    }

    public <K> ObjectMap<K, T> asMap(Func<T, K> keygen){
        return asMap(keygen, t -> t);
    }

    public ObjectSet<T> asSet(){
        return ObjectSet.with(this);
    }

    public Seq<T> copy(){
        return new Seq<>(this);
    }

    public ArrayList<T> list(){
        ArrayList<T> list = new ArrayList<>(size);
        each(list::add);
        return list;
    }

    public float sumf(Floatf<T> summer){
        float sum = 0;
        for(int i = 0; i < size; i++){
            sum += summer.get(items[i]);
        }
        return sum;
    }

    public int sum(Intf<T> summer){
        int sum = 0;
        for(int i = 0; i < size; i++){
            sum += summer.get(items[i]);
        }
        return sum;
    }

    public <E extends T> void each(Boolf<? super T> pred, Cons<E> consumer){
        for(int i = 0; i < size; i++){
            if(pred.get(items[i])) consumer.get((E)items[i]);
        }
    }

    @Override
    public void each(Cons<? super T> consumer){
        for(int i = 0; i < size; i++){
            consumer.get(items[i]);
        }
    }

    /** Replaces values without creating a new array. */
    public void replace(Func<T, T> mapper){
        for(int i = 0; i < size; i++){
            items[i] = mapper.get(items[i]);
        }
    }

    /** Flattens this array of arrays into one array. Allocates a new instance. */
    public <R> Seq<R> flatten(){
        Seq<R> arr = new Seq<>();
        for(int i = 0; i < size; i++){
            arr.addAll((Seq<R>)items[i]);
        }
        return arr;
    }

    /** Returns a new array with the mapped values. */
    public <R> Seq<R> flatMap(Func<T, Iterable<R>> mapper){
        Seq<R> arr = new Seq<>(size);
        for(int i = 0; i < size; i++){
            arr.addAll(mapper.get(items[i]));
        }
        return arr;
    }

    /** Returns a new array with the mapped values. */
    public <R> Seq<R> map(Func<T, R> mapper){
        Seq<R> arr = new Seq<>(size);
        for(int i = 0; i < size; i++){
            arr.add(mapper.get(items[i]));
        }
        return arr;
    }

    /** @return a new int array with the mapped values. */
    public IntSeq mapInt(Intf<T> mapper){
        IntSeq arr = new IntSeq(size);
        for(int i = 0; i < size; i++){
            arr.add(mapper.get(items[i]));
        }
        return arr;
    }

    /** @return a new int array with the mapped values. */
    public IntSeq mapInt(Intf<T> mapper, Boolf<T> retain){
        IntSeq arr = new IntSeq(size);
        for(int i = 0; i < size; i++){
            T item = items[i];
            if(retain.get(item)){
                arr.add(mapper.get(item));
            }
        }
        return arr;
    }

    /** @return a new float array with the mapped values. */
    public FloatSeq mapFloat(Floatf<T> mapper){
        FloatSeq arr = new FloatSeq(size);
        for(int i = 0; i < size; i++){
            arr.add(mapper.get(items[i]));
        }
        return arr;
    }

    public <R> R reduce(R initial, Func2<T, R, R> reducer){
        R result = initial;
        for(int i = 0; i < size; i++){
            result = reducer.get(items[i], result);
        }
        return result;
    }

    public boolean allMatch(Boolf<T> predicate){
        for(int i = 0; i < size; i++){
            if(!predicate.get(items[i])){
                return false;
            }
        }
        return true;
    }

    public boolean contains(Boolf<T> predicate){
        for(int i = 0; i < size; i++){
            if(predicate.get(items[i])){
                return true;
            }
        }
        return false;
    }

    public T min(Comparator<T> func){
        T result = null;
        for(int i = 0; i < size; i++){
            T t = items[i];
            if(result == null || func.compare(result, t) > 0){
                result = t;
            }
        }
        return result;
    }

    public T max(Comparator<T> func){
        T result = null;
        for(int i = 0; i < size; i++){
            T t = items[i];
            if(result == null || func.compare(result, t) < 0){
                result = t;
            }
        }
        return result;
    }

    public T min(Boolf<T> filter, Floatf<T> func){
        T result = null;
        float min = Float.MAX_VALUE;
        for(int i = 0; i < size; i++){
            T t = items[i];
            if(!filter.get(t)) continue;
            float val = func.get(t);
            if(val <= min){
                result = t;
                min = val;
            }
        }
        return result;
    }

    public T min(Boolf<T> filter, Comparator<T> func){
        T result = null;
        for(int i = 0; i < size; i++){
            T t = items[i];
            if(filter.get(t) && (result == null || func.compare(result, t) > 0)){
                result = t;
            }
        }
        return result;
    }

    public T min(Floatf<T> func){
        T result = null;
        float min = Float.MAX_VALUE;
        for(int i = 0; i < size; i++){
            T t = items[i];
            float val = func.get(t);
            if(val <= min){
                result = t;
                min = val;
            }
        }
        return result;
    }

    public T max(Floatf<T> func){
        T result = null;
        float max = Float.NEGATIVE_INFINITY;
        for(int i = 0; i < size; i++){
            T t = items[i];
            float val = func.get(t);
            if(val >= max){
                result = t;
                max = val;
            }
        }
        return result;
    }

    public @Nullable T find(Boolf<T> predicate){
        for(int i = 0; i < size; i++){
            if(predicate.get(items[i])){
                return items[i];
            }
        }
        return null;
    }

    public Seq<T> with(Cons<Seq<T>> cons){
        cons.get(this);
        return this;
    }

    /**
     * Adds a value if it was not already in this sequence.
     * @return whether this value was added successfully.
     */
    public boolean addUnique(T value){
        if(!contains(value)){
            add(value);
            return true;
        }
        return false;
    }

    public Seq<T> add(T value){
        T[] items = this.items;
        if(size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size++] = value;
        return this;
    }

    public Seq<T> add(T value1, T value2){
        T[] items = this.items;
        if(size + 1 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size] = value1;
        items[size + 1] = value2;
        size += 2;
        return this;
    }

    public Seq<T> add(T value1, T value2, T value3){
        T[] items = this.items;
        if(size + 2 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size] = value1;
        items[size + 1] = value2;
        items[size + 2] = value3;
        size += 3;
        return this;
    }

    public Seq<T> add(T value1, T value2, T value3, T value4){
        T[] items = this.items;
        if(size + 3 >= items.length) items = resize(Math.max(8, (int)(size * 1.8f))); // 1.75 isn't enough when size=5.
        items[size] = value1;
        items[size + 1] = value2;
        items[size + 2] = value3;
        items[size + 3] = value4;
        size += 4;
        return this;
    }

    public Seq<T> add(Seq<? extends T> array){
        addAll(array.items, 0, array.size);
        return this;
    }

    public Seq<T> add(T[] array){
        addAll(array, 0, array.length);
        return this;
    }

    public Seq<T> addAll(Seq<? extends T> array){
        addAll(array.items, 0, array.size);
        return this;
    }

    public Seq<T> addAll(Seq<? extends T> array, int start, int count){
        if(start + count > array.size)
            throw new IllegalArgumentException("start + count must be <= size: " + start + " + " + count + " <= " + array.size);
        addAll(array.items, start, count);
        return this;
    }

    public Seq<T> addAll(T... array){
        addAll(array, 0, array.length);
        return this;
    }

    public Seq<T> addAll(T[] array, int start, int count){
        T[] items = this.items;
        int sizeNeeded = size + count;
        if(sizeNeeded > items.length) items = resize(Math.max(8, (int)(sizeNeeded * 1.75f)));
        System.arraycopy(array, start, items, size, count);
        size += count;
        return this;
    }

    public Seq<T> addAll(Iterable<? extends T> items){
        if(items instanceof Seq){
            addAll((Seq)items);
        }else{
            for(T t : items){
                add(t);
            }
        }
        return this;
    }

    /** Sets this array's contents to the specified array. */
    public void set(Seq<? extends T> array){
        if(array == this) return;
        clear();
        addAll(array);
    }

    /** Sets this array's contents to the specified array. */
    public void set(T[] array){
        clear();
        addAll(array);
    }

    @Nullable
    public T getFrac(float index){
        if(isEmpty()) return null;
        return get(Mathf.clamp((int)(index * size), 0, size - 1));
    }

    public T get(int index){
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        return items[index];
    }

    public void set(int index, T value){
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] = value;
    }

    public void insert(int index, T value){
        if(index > size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
        T[] items = this.items;
        if(size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        if(ordered)
            System.arraycopy(items, index, items, index + 1, size - index);
        else
            items[size] = items[index];
        size++;
        items[index] = value;
    }

    public void swap(int first, int second){
        if(first >= size) throw new IndexOutOfBoundsException("first can't be >= size: " + first + " >= " + size);
        if(second >= size) throw new IndexOutOfBoundsException("second can't be >= size: " + second + " >= " + size);
        T[] items = this.items;
        T firstValue = items[first];
        items[first] = items[second];
        items[second] = firstValue;
    }

    /**
     * Replaces the first occurrence of 'from' with 'to'.
     * @return whether anything was replaced.
     * */
    public boolean replace(T from, T to){
        int idx = indexOf(from);
        if(idx != -1){
            items[idx] = to;
            return true;
        }
        return false;
    }

    /** @return whether this sequence contains every other element in the other sequence. */
    public boolean containsAll(Seq<T> seq){
        return containsAll(seq, false);
    }

    /** @return whether this sequence contains every other element in the other sequence. */
    public boolean containsAll(Seq<T> seq, boolean identity){
        T[] others = seq.items;

        for(int i = 0; i < seq.size; i++){
            if(!contains(others[i], identity)){
                return false;
            }
        }
        return true;
    }

    public boolean contains(T value){
        return contains(value, false);
    }

    /**
     * Returns if this array contains value.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return true if array contains value, false if it doesn't
     */
    public boolean contains(T value, boolean identity){
        T[] items = this.items;
        int i = size - 1;
        if(identity || value == null){
            while(i >= 0)
                if(items[i--] == value) return true;
        }else{
            while(i >= 0)
                if(value.equals(items[i--])) return true;
        }
        return false;
    }

    public int indexOf(T value){
        return indexOf(value, false);
    }

    /**
     * Returns the index of first occurrence of value in the array, or -1 if no such value exists.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of first occurrence of value in array or -1 if no such value exists
     */
    public int indexOf(T value, boolean identity){
        T[] items = this.items;
        if(identity || value == null){
            for(int i = 0, n = size; i < n; i++)
                if(items[i] == value) return i;
        }else{
            for(int i = 0, n = size; i < n; i++)
                if(value.equals(items[i])) return i;
        }
        return -1;
    }

    public int indexOf(Boolf<T> value){
        T[] items = this.items;
        for(int i = 0, n = size; i < n; i++)
            if(value.get(items[i])) return i;
        return -1;
    }

    /**
     * Returns an index of last occurrence of value in array or -1 if no such value exists. Search is started from the end of an
     * array.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of last occurrence of value in array or -1 if no such value exists
     */
    public int lastIndexOf(T value, boolean identity){
        T[] items = this.items;
        if(identity || value == null){
            for(int i = size - 1; i >= 0; i--)
                if(items[i] == value) return i;
        }else{
            for(int i = size - 1; i >= 0; i--)
                if(value.equals(items[i])) return i;
        }
        return -1;
    }

    /** Removes a value, without using identity. */
    public boolean remove(T value){
        return remove(value, false);
    }

    /** Removes a single value by predicate.
     * @return whether the item was found and removed. */
    public boolean remove(Boolf<T> value){
        for(int i = 0; i < size; i++){
            if(value.get(items[i])){
                remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the first instance of the specified value in the array.
     * @param value May be null.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return true if value was found and removed, false otherwise
     */
    public boolean remove(T value, boolean identity){
        T[] items = this.items;
        if(identity || value == null){
            for(int i = 0, n = size; i < n; i++){
                if(items[i] == value){
                    remove(i);
                    return true;
                }
            }
        }else{
            for(int i = 0, n = size; i < n; i++){
                if(value.equals(items[i])){
                    remove(i);
                    return true;
                }
            }
        }
        return false;
    }

    /** Removes and returns the item at the specified index. */
    public T remove(int index){
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        T[] items = this.items;
        T value = items[index];
        size--;
        if(ordered)
            System.arraycopy(items, index + 1, items, index, size - index);
        else
            items[index] = items[size];
        items[size] = null;
        return value;
    }

    /** Removes the items between the specified indices, inclusive. */
    public void removeRange(int start, int end){
        if(end >= size) throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + size);
        if(start > end) throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
        T[] items = this.items;
        int count = end - start + 1;
        if(ordered)
            System.arraycopy(items, start + count, items, start, size - (start + count));
        else{
            int lastIndex = this.size - 1;
            for(int i = 0; i < count; i++)
                items[start + i] = items[lastIndex - i];
        }
        size -= count;
    }

    /** @return this object */
    public Seq<T> removeAll(Boolf<T> pred){
        Iterator<T> iter = iterator();
        while(iter.hasNext()){
            if(pred.get(iter.next())){
                iter.remove();
            }
        }
        return this;
    }

    public boolean removeAll(Seq<? extends T> array){
        return removeAll(array, false);
    }

    /**
     * Removes from this array all of elements contained in the specified array.
     * @param identity True to use ==, false to use .equals().
     * @return true if this array was modified.
     */
    public boolean removeAll(Seq<? extends T> array, boolean identity){
        int size = this.size;
        int startSize = size;
        T[] items = this.items;
        if(identity){
            for(int i = 0, n = array.size; i < n; i++){
                T item = array.get(i);
                for(int ii = 0; ii < size; ii++){
                    if(item == items[ii]){
                        remove(ii);
                        size--;
                        break;
                    }
                }
            }
        }else{
            for(int i = 0, n = array.size; i < n; i++){
                T item = array.get(i);
                for(int ii = 0; ii < size; ii++){
                    if(item.equals(items[ii])){
                        remove(ii);
                        size--;
                        break;
                    }
                }
            }
        }
        return size != startSize;
    }

    /** If this array is empty, returns an object specified by the constructor.
     * Otherwise, acts like pop(). */
    public T pop(Prov<T> constructor){
        if(size == 0) return constructor.get();
        return pop();
    }

    /** Removes and returns the last item. */
    public T pop(){
        if(size == 0) throw new IllegalStateException("Array is empty.");
        --size;
        T item = items[size];
        items[size] = null;
        return item;
    }

    /** Returns the last item. */
    public T peek(){
        if(size == 0) throw new IllegalStateException("Array is empty.");
        return items[size - 1];
    }

    /** Returns the first item. */
    public T first(){
        if(size == 0) throw new IllegalStateException("Array is empty.");
        return items[0];
    }

    /** Returns the first item, or null if this Seq is empty. */
    @Nullable
    public T firstOpt(){
        if(size == 0) return null;
        return items[0];
    }

    /** Returns true if the array is empty. */
    public boolean isEmpty(){
        return size == 0;
    }

    public boolean any(){
        return size > 0;
    }

    public Seq<T> clear(){
        T[] items = this.items;
        for(int i = 0, n = size; i < n; i++)
            items[i] = null;
        size = 0;
        return this;
    }

    /**
     * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     * @return {@link #items}
     */
    public T[] shrink(){
        if(items.length != size) resize(size);
        return items;
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     * @return {@link #items}
     */
    public T[] ensureCapacity(int additionalCapacity){
        if(additionalCapacity < 0)
            throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
        int sizeNeeded = size + additionalCapacity;
        if(sizeNeeded > items.length) resize(Math.max(8, sizeNeeded));
        return items;
    }

    /**
     * Sets the array size, leaving any values beyond the current size null.
     * @return {@link #items}
     */
    public T[] setSize(int newSize){
        truncate(newSize);
        if(newSize > items.length) resize(Math.max(8, newSize));
        size = newSize;
        return items;
    }

    /** Creates a new backing array with the specified size containing the current items. */
    protected T[] resize(int newSize){
        T[] items = this.items;
        //avoid reflection when possible
        T[] newItems = (T[])(items.getClass() == Object[].class ? new Object[newSize] : java.lang.reflect.Array.newInstance(items.getClass().getComponentType(), newSize));
        System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    /**
     * Sorts this array. The array elements must implement {@link Comparable}. This method is not thread safe (uses
     * {@link Sort#instance()}).
     */
    public Seq<T> sort(){
        Sort.instance().sort(items, 0, size);
        return this;
    }

    /** Sorts the array. This method is not thread safe (uses {@link Sort#instance()}). */
    public Seq<T> sort(Comparator<? super T> comparator){
        Sort.instance().sort(items, comparator, 0, size);
        return this;
    }

    public Seq<T> sort(Floatf<? super T> comparator){
        Sort.instance().sort(items, Structs.comparingFloat(comparator), 0, size);
        return this;
    }

    public <U extends Comparable<? super U>> Seq<T> sortComparing(Func<? super T, ? extends U> keyExtractor){
        sort(Structs.comparing(keyExtractor));
        return this;
    }

    public Seq<T> selectFrom(Seq<T> base, Boolf<T> predicate){
        clear();
        base.each(t -> {
            if(predicate.get(t)){
                add(t);
            }
        });
        return this;
    }

    /** Note that this allocates a new set. Mutates. */
    public Seq<T> distinct(){
        ObjectSet<T> set = asSet();
        clear();
        addAll(set);
        return this;
    }

    public <R> Seq<R> as(){
        return (Seq<R>)this;
    }

    /** Allocates a new array with all elements that match the predicate. */
    public Seq<T> select(Boolf<T> predicate){
        Seq<T> arr = new Seq<>();
        for(int i = 0; i < size; i++){
            if(predicate.get(items[i])){
                arr.add(items[i]);
            }
        }
        return arr;
    }

    /** Removes everything that does not match this predicate. */
    public Seq<T> retainAll(Boolf<T> predicate){
        return removeAll(e -> !predicate.get(e));
    }

    public int count(Boolf<T> predicate){
        int count = 0;
        for(int i = 0; i < size; i++){
            if(predicate.get(items[i])){
                count ++;
            }
        }
        return count;
    }

    /**
     * Selects the nth-lowest element from the Seq according to Comparator ranking. This might partially sort the Array. The
     * array must have a size greater than 0, or a {@link ArcRuntimeException} will be thrown.
     * @param comparator used for comparison
     * @param kthLowest rank of desired object according to comparison, n is based on ordinal numbers, not array indices. for min
     * value use 1, for max value use size of array, using 0 results in runtime exception.
     * @return the value of the Nth lowest ranked object.
     * @see Select
     */
    public T selectRanked(Comparator<T> comparator, int kthLowest){
        if(kthLowest < 1){
            throw new ArcRuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
        }
        return Select.instance().select(items, comparator, kthLowest, size);
    }

    /**
     * @param comparator used for comparison
     * @param kthLowest rank of desired object according to comparison, n is based on ordinal numbers, not array indices. for min
     * value use 1, for max value use size of array, using 0 results in runtime exception.
     * @return the index of the Nth lowest ranked object.
     * @see Seq#selectRanked(java.util.Comparator, int)
     */
    public int selectRankedIndex(Comparator<T> comparator, int kthLowest){
        if(kthLowest < 1){
            throw new ArcRuntimeException("nth_lowest must be greater than 0, 1 = first, 2 = second...");
        }
        return Select.instance().selectIndex(items, comparator, kthLowest, size);
    }

    public Seq<T> reverse(){
        T[] items = this.items;
        for(int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++){
            int ii = lastIndex - i;
            T temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }

        return this;
    }

    public Seq<T> shuffle(){
        T[] items = this.items;
        for(int i = size - 1; i >= 0; i--){
            int ii = Mathf.random(i);
            T temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }

        return this;
    }

    /**
     * Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
     * taken.
     */
    public void truncate(int newSize){
        if(newSize < 0) throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
        if(size <= newSize) return;
        for(int i = newSize; i < size; i++)
            items[i] = null;
        size = newSize;
    }

    public T random(Rand rand){
        if(size == 0) return null;
        return items[rand.random(0, size - 1)];
    }

    /** Returns a random item from the array, or null if the array is empty. */
    public T random(){
        return random(Mathf.rand);
    }

    /** Returns a random item from the array, excluding the specified element. If the array is empty, returns null.
     * If this array only has one element, returns that element. */
    public T random(T exclude){
        if(exclude == null) return random();
        if(size == 0) return null;
        if(size == 1) return first();

        int eidx = indexOf(exclude);
        //this item isn't even in the array!
        if(eidx == -1) return random();

        //shift up the index
        int index = Mathf.random(0, size - 2);
        if(index >= eidx){
            index ++;
        }
        return items[index];
    }

    /**
     * Returns the items as an array. Note the array is typed, so the {@link #Seq(Class)} constructor must have been used.
     * Otherwise use {@link #toArray(Class)} to specify the array type.
     */
    public T[] toArray(){
        return toArray(items.getClass().getComponentType());
    }

    public <V> V[] toArray(Class type){
        V[] result = (V[])java.lang.reflect.Array.newInstance(type, size);
        System.arraycopy(items, 0, result, 0, size);
        return result;
    }

    @Override
    public int hashCode(){
        if(!ordered) return super.hashCode();
        Object[] items = this.items;
        int h = 1;
        for(int i = 0, n = size; i < n; i++){
            h *= 31;
            Object item = items[i];
            if(item != null) h += item.hashCode();
        }
        return h;
    }

    @Override
    public boolean equals(Object object){
        if(object == this) return true;
        if(!ordered) return false;
        if(!(object instanceof Seq)) return false;
        Seq array = (Seq)object;
        if(!array.ordered) return false;
        int n = size;
        if(n != array.size) return false;
        Object[] items1 = this.items;
        Object[] items2 = array.items;
        for(int i = 0; i < n; i++){
            Object o1 = items1[i];
            Object o2 = items2[i];
            if(!(o1 == null ? o2 == null : o1.equals(o2))) return false;
        }
        return true;
    }

    @Override
    public String toString(){
        if(size == 0) return "[]";
        T[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append('[');
        buffer.append(items[0]);
        for(int i = 1; i < size; i++){
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append(']');
        return buffer.toString();
    }

    public String toString(String separator, Func<T, String> stringifier){
        if(size == 0) return "";
        T[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append(stringifier.get(items[0]));
        for(int i = 1; i < size; i++){
            buffer.append(separator);
            buffer.append(stringifier.get(items[i]));
        }
        return buffer.toString();
    }

    public String toString(String separator){
        return toString(separator, String::valueOf);
    }

    /**
     * Returns an iterator for the items in the array. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called, unless you are using nested loops.
     * <b>Never, ever</b> access this iterator's method manually, e.g. hasNext()/next().
     * Note that calling 'break' while iterating will permanently clog this iterator, falling back to an implementation that allocates new ones.
     */
    @Override
    public Iterator<T> iterator(){
        if(iterable == null) iterable = new SeqIterable<>(this);
        return iterable.iterator();
    }

    public static class SeqIterable<T> implements Iterable<T>{
        final Seq<T> array;
        final boolean allowRemove;
        private SeqIterator iterator1 = new SeqIterator(), iterator2 = new SeqIterator();

        public SeqIterable(Seq<T> array){
            this(array, true);
        }

        public SeqIterable(Seq<T> array, boolean allowRemove){
            this.array = array;
            this.allowRemove = allowRemove;
        }

        @Override
        public Iterator<T> iterator(){
            if(iterator1.done){
                iterator1.index = 0;
                iterator1.done = false;
                return iterator1;
            }

            if(iterator2.done){
                iterator2.index = 0;
                iterator2.done = false;
                return iterator2;
            }
            //allocate new iterator in the case of 3+ nested loops.
            return new SeqIterator();
        }

        private class SeqIterator implements Iterator<T>{
            int index;
            boolean done = true;

            {
                iteratorsAllocated ++;
            }

            SeqIterator(){
            }

            @Override
            public boolean hasNext(){
                if(index >= array.size) done = true;
                return index < array.size;
            }

            @Override
            public T next(){
                if(index >= array.size) throw new NoSuchElementException(String.valueOf(index));
                return array.items[index++];
            }

            @Override
            public void remove(){
                if(!allowRemove) throw new ArcRuntimeException("Remove not allowed.");
                index--;
                array.remove(index);
            }
        }
    }
}
