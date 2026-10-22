package arc.struct;

import arc.func.*;
import arc.math.*;
import arc.util.*;

import java.util.*;

/**
 * An unordered set where the keys are objects. This implementation uses cuckoo hashing using 3 hashes, random walking, and a
 * small stash for problematic keys. Null keys are not allowed. No allocation is done except when growing the table size. <br>
 * <br>
 * This set performs very fast contains and remove (typically O(1), worst case O(log(n))). Add may be a bit slower, depending on
 * hash collisions. Load factors greater than 0.91 greatly increase the chances the set will have to rehash to the next higher POT
 * size.<br>
 * <br>
 * Iteration can be very slow for a set with a large capacity. {@link #clear(int)} and {@link #shrink(int)} can be used to reduce
 * the capacity. {@link OrderedSet} provides much faster iteration.
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class ObjectSet<T> implements Iterable<T>, Eachable<T>{
    public int size;

    T[] keyTable;

    float loadFactor;
    int threshold;

    /**
     * Used by {@link #place(Object)} to bit shift the upper bits of a {@code long} into a usable range (&gt;= 0 and &lt;=
     * {@link #mask}). The shift can be negative, which is convenient to match the number of bits in mask: if mask is a 7-bit
     * number, a shift of -7 shifts the upper 7 bits into the lowest 7 positions. This class sets the shift &gt; 32 and &lt; 64,
     * which if used with an int will still move the upper bits of an int to the lower bits due to Java's implicit modulus on
     * shifts.
     * <p>
     * {@link #mask} can also be used to mask the low bits of a number, which may be faster for some hashcodes, if
     * {@link #place(Object)} is overridden.
     */
    protected int shift;

    /**
     * A bitmask used to confine hashcodes to the size of the table. Must be all 1 bits in its low positions, ie a power of two
     * minus 1. If {@link #place(Object)} is overridden, this can be used instead of {@link #shift} to isolate usable bits of a
     * hash.
     */
    protected int mask;

    private transient ObjectSetIterator iterator1, iterator2;

    /** Creates a new set with an initial capacity of 51 and a load factor of 0.8. */
    public ObjectSet(){
        this(51, 0.8f);
    }

    /**
     * Creates a new set with a load factor of 0.8.
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two.
     */
    public ObjectSet(int initialCapacity){
        this(initialCapacity, 0.8f);
    }

    /**
     * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
     * growing the backing table.
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two.
     */
    public ObjectSet(int initialCapacity, float loadFactor){
        if(loadFactor <= 0f || loadFactor >= 1f)
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        this.loadFactor = loadFactor;

        int tableSize = tableSize(initialCapacity, loadFactor);
        threshold = (int)(tableSize * loadFactor);
        mask = tableSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        keyTable = (T[])new Object[tableSize];
    }

    /** Creates a new set identical to the specified set. */
    public ObjectSet(ObjectSet<? extends T> set){
        this((int)(set.keyTable.length * set.loadFactor), set.loadFactor);
        System.arraycopy(set.keyTable, 0, keyTable, 0, set.keyTable.length);
        size = set.size;
    }


    public static <T> ObjectSet<T> with(T... array){
        ObjectSet<T> set = new ObjectSet<>();
        set.addAll(array);
        return set;
    }

    public static <T> ObjectSet<T> with(Seq<T> array){
        ObjectSet<T> set = new ObjectSet<>();
        set.addAll(array);
        return set;
    }

    public ObjectSet<T> copy(){
        ObjectSet<T> result = new ObjectSet<>();
        result.addAll(this);
        return result;
    }

    /** Allocates a new set with all elements that match the predicate.*/
    public ObjectSet<T> select(Boolf<T> predicate){
        ObjectSet<T> arr = new ObjectSet<>();
        for(T t : this){
            if(predicate.get(t)) arr.add(t);
        }
        return arr;
    }

    public Seq<T> toSeq(){
        return iterator().toSeq();
    }

    @Override
    public void each(Cons<? super T> cons){
        for(T t : this){
            cons.get(t);
        }
    }

    public T find(Boolf<T> predicate){
        for(T t : this){
            if(predicate.get(t)){
                return t;
            }
        }
        return null;
    }

    /**
     * Returns an index >= 0 and <= {@link #mask} for the specified {@code item}.
     * <p>
     * The default implementation uses Fibonacci hashing on the item's {@link Object#hashCode()}: the hashcode is multiplied by a
     * long constant (2 to the 64th, divided by the golden ratio) then the uppermost bits are shifted into the lowest positions to
     * obtain an index in the desired range. Multiplication by a long may be slower than int (eg on GWT) but greatly improves
     * rehashing, allowing even very poor hashcodes, such as those that only differ in their upper bits, to be used without high
     * collision rates. Fibonacci hashing has increased collision rates when all or most hashcodes are multiples of larger
     * Fibonacci numbers (see <a href=
     * "https://probablydance.com/2018/06/16/fibonacci-hashing-the-optimization-that-the-world-forgot-or-a-better-alternative-to-integer-modulo/">Malte
     * Skarupke's blog post</a>).
     * <p>
     * This method can be overridden to customizing hashing. This may be useful eg in the unlikely event that most hashcodes are
     * Fibonacci numbers, if keys provide poor or incorrect hashcodes, or to simplify hashing if keys provide high quality
     * hashcodes and don't need Fibonacci hashing: {@code return item.hashCode() & mask;}
     */
    protected int place(T item){
        return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
    }

    /**
     * Returns the index of the key if already present, else -(index + 1) for the next empty index. This can be overridden in this
     * pacakge to compare for equality differently than {@link Object#equals(Object)}.
     */
    int locateKey(T key){
        if(key == null) throw new IllegalArgumentException("key cannot be null.");
        T[] keyTable = this.keyTable;
        for(int i = place(key); ; i = i + 1 & mask){
            T other = keyTable[i];
            if(other == null) return -(i + 1); // Empty space is available.
            if(other.equals(key)) return i; // Same key was found.
        }
    }

    public boolean any(){
        return size > 0;
    }

    /**
     * Returns true if the key was added to the set or false if it was already in the set. If this set already contains the key,
     * the call leaves the set unchanged and returns false.
     */
    public boolean add(T key){
        if(key == null) return false;
        int i = locateKey(key);
        if(i >= 0) return false; // Existing key was found.
        i = -(i + 1); // Empty space was found.
        keyTable[i] = key;
        if(++size >= threshold) resize(keyTable.length << 1);
        return true;
    }

    public void addAll(Seq<? extends T> array){
        addAll(array.items, 0, array.size);
    }

    public void addAll(Seq<? extends T> array, int offset, int length){
        if(offset + length > array.size)
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        addAll(array.items, offset, length);
    }

    public boolean addAll(T... array){
        return addAll(array, 0, array.length);
    }

    public boolean addAll(T[] array, int offset, int length){
        ensureCapacity(length);
        int oldSize = size;
        for(int i = offset, n = i + length; i < n; i++){
            T value = array[i];
            if(value != null) add(value);
        }
        return oldSize != size;
    }

    public void addAll(ObjectSet<? extends T> set){
        ensureCapacity(set.size);
        int oldSize = size;
        T[] keyTable = set.keyTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            T key = keyTable[i];
            if(key != null) add(key);
        }
    }

    /** Skips checks for existing keys, doesn't increment size. */
    private void addResize(T key){
        T[] keyTable = this.keyTable;
        for(int i = place(key); ; i = (i + 1) & mask){
            if(keyTable[i] == null){
                keyTable[i] = key;
                return;
            }
        }
    }

    public void removeAll(T[] array, int offset, int length){
        for(int i = offset, n = i + length; i < n; i++)
            remove(array[i]);
    }

    public void removeAll(T[] array){
        for(T t : array){
            remove(t);
        }
    }

    public void removeAll(Seq<? extends T> array){
        removeAll(array.items, 0, array.size);
    }


    /** Returns true if the key was removed. */
    public boolean remove(T key){
        int i = locateKey(key);
        if(i < 0) return false;
        T[] keyTable = this.keyTable;
        int mask = this.mask, next = i + 1 & mask;
        while((key = keyTable[next]) != null){
            int placement = place(key);
            if((next - placement & mask) > (i - placement & mask)){
                keyTable[i] = key;
                i = next;
            }
            next = next + 1 & mask;
        }
        keyTable[i] = null;
        size--;
        return true;
    }

    /** Returns true if the set has one or more items. */
    public boolean notEmpty(){
        return size > 0;
    }

    /** Returns true if the set is empty. */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
     * nothing is done. If the set contains more items than the specified capacity, the next highest power of two capacity is used
     * instead.
     */
    public void shrink(int maximumCapacity){
        if(maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
        if(size > maximumCapacity) maximumCapacity = size;
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if(keyTable.length > tableSize) resize(tableSize);
    }

    /**
     * Clears the set and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger.
     * The reduction is done by allocating new arrays, though for large arrays this can be faster than clearing the existing
     * array.
     */
    public void clear(int maximumCapacity){
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if(keyTable.length <= tableSize){
            clear();
            return;
        }
        size = 0;
        resize(tableSize);
    }

    /**
     * Clears the set, leaving the backing arrays at the current capacity. When the capacity is high and the population is low,
     * iteration can be unnecessarily slow. {@link #clear(int)} can be used to reduce the capacity.
     */
    public void clear(){
        if(size == 0) return;
        size = 0;
        Arrays.fill(keyTable, null);
    }

    public boolean contains(T key){
        return locateKey(key) >= 0;
    }

    public @Nullable T get(T key){
        int i = locateKey(key);
        return i < 0 ? null : keyTable[i];
    }

    public T first(){
        T[] keyTable = this.keyTable;
        for(int i = 0, n = keyTable.length; i < n; i++)
            if(keyTable[i] != null) return keyTable[i];
        throw new IllegalStateException("ObjectSet is empty.");
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
     * adding many items to avoid multiple backing array resizes.
     */
    public void ensureCapacity(int additionalCapacity){
        int tableSize = tableSize(size + additionalCapacity, loadFactor);
        if(keyTable.length < tableSize) resize(tableSize);
    }

    private void resize(int newSize){
        int oldCapacity = keyTable.length;
        threshold = (int)(newSize * loadFactor);
        mask = newSize - 1;
        shift = Long.numberOfLeadingZeros(mask);
        T[] oldKeyTable = keyTable;

        keyTable = (T[])(new Object[newSize]);

        if(size > 0){
            for(int i = 0; i < oldCapacity; i++){
                T key = oldKeyTable[i];
                if(key != null) addResize(key);
            }
        }
    }

    public int hashCode(){
        int h = size;
        T[] keyTable = this.keyTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            T key = keyTable[i];
            if(key != null) h += key.hashCode();
        }
        return h;
    }

    public boolean equals(Object obj){
        if(!(obj instanceof ObjectSet)) return false;
        ObjectSet other = (ObjectSet)obj;
        if(other.size != size) return false;
        T[] keyTable = this.keyTable;
        for(int i = 0, n = keyTable.length; i < n; i++)
            if(keyTable[i] != null && !other.contains(keyTable[i])) return false;
        return true;
    }

    public String toString(){
        return '{' + toString(", ") + '}';
    }

    public String toString(String separator){
        if(size == 0) return "";
        java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
        T[] keyTable = this.keyTable;
        int i = keyTable.length;
        while(i-- > 0){
            T key = keyTable[i];
            if(key == null) continue;
            buffer.append(key == this ? "(this)" : key);
            break;
        }
        while(i-- > 0){
            T key = keyTable[i];
            if(key == null) continue;
            buffer.append(separator);
            buffer.append(key == this ? "(this)" : key);
        }
        return buffer.toString();
    }

    /**
     * Returns an iterator for the keys in the set. Remove is supported.
     * <p>
     * Use the {@link ObjectSetIterator} constructor for nested or multithreaded iteration.
     */
    public ObjectSetIterator<T> iterator(){
        if(iterator1 == null){
            iterator1 = new ObjectSetIterator(this);
            iterator2 = new ObjectSetIterator(this);
        }
        if(!iterator1.valid){
            iterator1.reset();
            iterator1.valid = true;
            iterator2.valid = false;
            return iterator1;
        }
        iterator2.reset();
        iterator2.valid = true;
        iterator1.valid = false;
        return iterator2;
    }

    static int tableSize(int capacity, float loadFactor){
        if(capacity < 0) throw new IllegalArgumentException("capacity must be >= 0: " + capacity);
        int tableSize = Mathf.nextPowerOfTwo(Math.max(2, (int)Math.ceil(capacity / loadFactor)));
        if(tableSize > 1 << 30) throw new IllegalArgumentException("The required capacity is too large: " + capacity);
        return tableSize;
    }

    public static class ObjectSetIterator<K> implements Iterable<K>, Iterator<K>{
        public boolean hasNext;

        final ObjectSet<K> set;
        int nextIndex, currentIndex;
        boolean valid = true;

        public ObjectSetIterator(ObjectSet<K> set){
            this.set = set;
            reset();
        }

        public void reset(){
            currentIndex = -1;
            nextIndex = -1;
            findNextIndex();
        }

        private void findNextIndex(){
            K[] keyTable = set.keyTable;
            for(int n = set.keyTable.length; ++nextIndex < n; ){
                if(keyTable[nextIndex] != null){
                    hasNext = true;
                    return;
                }
            }
            hasNext = false;
        }

        public void remove(){
            int i = currentIndex;
            if(i < 0) throw new IllegalStateException("next must be called before remove.");
            K[] keyTable = set.keyTable;
            int mask = set.mask, next = i + 1 & mask;
            K key;
            while((key = keyTable[next]) != null){
                int placement = set.place(key);
                if((next - placement & mask) > (i - placement & mask)){
                    keyTable[i] = key;
                    i = next;
                }
                next = next + 1 & mask;
            }
            keyTable[i] = null;
            set.size--;
            if(i != currentIndex) --nextIndex;
            currentIndex = -1;
        }

        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        public K next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K key = set.keyTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return key;
        }

        public ObjectSetIterator<K> iterator(){
            return this;
        }

        /** Adds the remaining values to the array. */
        public Seq<K> toSeq(Seq<K> array){
            while(hasNext)
                array.add(next());
            return array;
        }

        /** Returns a new array containing the remaining values. */
        public Seq<K> toSeq(){
            return toSeq(new Seq<K>(true, set.size));
        }
    }
}
