package arc.struct;

import arc.func.*;
import arc.util.*;

import java.util.*;

import static arc.struct.ObjectSet.*;

/**
 * An unordered map. This implementation is a cuckoo hash map using 3 hashes, random walking, and a small stash for problematic
 * keys. Null keys are not allowed. Null values are allowed. No allocation is done except when growing the table size. <br>
 * <br>
 * This map performs very fast get, containsKey, and remove (typically O(1), worst case O(log(n))). Put may be a bit slower,
 * depending on hash collisions. Load factors greater than 0.91 greatly increase the chances the map will have to rehash to the
 * next higher POT size.<br>
 * <br>
 * Iteration can be very slow for a map with a large capacity. {@link #clear(int)} and {@link #shrink(int)} can be used to reduce
 * the capacity. {@link OrderedMap} provides much faster iteration.
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class ObjectMap<K, V> implements Iterable<ObjectMap.Entry<K, V>>{
    static final Object dummy = new Object();

    public int size;

    K[] keyTable;
    V[] valueTable;

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

    transient Entries<K, V> entries1, entries2;
    transient Values<V> values1, values2;
    transient Keys<K> keys1, keys2;

    public static <K, V> ObjectMap<K, V> of(Object... values){
        ObjectMap<K, V> map = new ObjectMap<>();

        for(int i = 0; i < values.length / 2; i++){
            map.put((K)values[i * 2], (V)values[i * 2 + 1]);
        }

        return map;
    }

    /** Creates a new map with an initial capacity of 51 and a load factor of 0.8. */
    public ObjectMap(){
        this(51, 0.8f);
    }

    /**
     * Creates a new map with a load factor of 0.8.
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two.
     */
    public ObjectMap(int initialCapacity){
        this(initialCapacity, 0.8f);
    }

    /**
     * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
     * growing the backing table.
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two.
     */
    public ObjectMap(int initialCapacity, float loadFactor){
        if(loadFactor <= 0f || loadFactor >= 1f)
            throw new IllegalArgumentException("loadFactor must be > 0 and < 1: " + loadFactor);
        this.loadFactor = loadFactor;

        int tableSize = tableSize(initialCapacity, loadFactor);
        threshold = (int)(tableSize * loadFactor);
        mask = tableSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        keyTable = (K[])new Object[tableSize];
        valueTable = (V[])new Object[tableSize];
    }

    /** Creates a new map identical to the specified map. */
    public ObjectMap(ObjectMap<? extends K, ? extends V> map){
        this((int)(map.keyTable.length * map.loadFactor), map.loadFactor);
        System.arraycopy(map.keyTable, 0, keyTable, 0, map.keyTable.length);
        System.arraycopy(map.valueTable, 0, valueTable, 0, map.valueTable.length);
        size = map.size;
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
    protected int place(K item){
        return (int)(item.hashCode() * 0x9E3779B97F4A7C15L >>> shift);
    }

    /**
     * Returns the index of the key if already present, else -(index + 1) for the next empty index. This can be overridden in this
     * pacakge to compare for equality differently than {@link Object#equals(Object)}.
     */
    int locateKey(K key){
        if(key == null) throw new IllegalArgumentException("key cannot be null.");
        K[] keyTable = this.keyTable;
        for(int i = place(key); ; i = i + 1 & mask){
            K other = keyTable[i];
            if(other == null) return -(i + 1); // Empty space is available.
            if(other.equals(key)) return i; // Same key was found.
        }
    }

    /** Returns the old value associated with the specified key, or null. */
    public @Nullable V put(K key, @Nullable V value){
        int i = locateKey(key);
        if(i >= 0){ // Existing key was found.
            V oldValue = valueTable[i];
            valueTable[i] = value;
            return oldValue;
        }
        i = -(i + 1); // Empty space was found.
        keyTable[i] = key;
        valueTable[i] = value;
        if(++size >= threshold) resize(keyTable.length << 1);
        return null;
    }

    public @Nullable V putMissing(K key, @Nullable V value){
        int i = locateKey(key);
        if(i >= 0) return valueTable[i]; // Existing key was found.
        i = -(i + 1); // Empty space was found.
        keyTable[i] = key;
        valueTable[i] = value;
        if(++size >= threshold) resize(keyTable.length << 1);
        return null;
    }

    public void putAll(Object... values){
        for(int i = 0; i < values.length / 2; i++){
            put((K)values[i * 2], (V)values[i * 2 + 1]);
        }
    }

    public void putAll(ObjectMap<? extends K, ? extends V> map){
        ensureCapacity(map.size);
        K[] keyTable = map.keyTable;
        V[] valueTable = map.valueTable;
        K key;
        for(int i = 0, n = keyTable.length; i < n; i++){
            key = keyTable[i];
            if(key != null) put(key, valueTable[i]);
        }
    }

    /** Put all the keys of this other map into this map, and return this map for chaining. */
    public ObjectMap<K, V> merge(ObjectMap<? extends K, ? extends V> map){
        putAll(map);
        return this;
    }

    /**Iterates through key/value pairs.*/
    public void each(Cons2<K, V> cons){
        for(Entry<K, V> entry : entries()){
            cons.get(entry.key, entry.value);
        }
    }

    public ObjectMap<K, V> copy(){
        return new ObjectMap<>(this);
    }

    public void set(ObjectMap<K, V> value){
        clear();
        putAll(value);
    }

    /** Skips checks for existing keys, doesn't increment size. */
    private void putResize(K key, @Nullable V value){
        K[] keyTable = this.keyTable;
        for(int i = place(key); ; i = (i + 1) & mask){
            if(keyTable[i] == null){
                keyTable[i] = key;
                valueTable[i] = value;
                return;
            }
        }
    }

    public V getThrow(K key, Prov<? extends RuntimeException> error){
        if(!containsKey(key)){
            throw error.get();
        }
        return get(key);
    }

    /** Tries to get the value. If it does not exist, it creates a new instance using the supplier and places it, returning the value.*/
    public V get(K key, Prov<V> supplier){
        V val = get(key);
        if(val == null){
            put(key, val = supplier.get());
        }
        return val;
    }

    /** Returns the value for the specified key, or null if the key is not in the map. */
    public @Nullable <T extends K> V get(T key){
        int i = locateKey(key);
        return i < 0 ? null : valueTable[i];
    }

    /** Returns the value for the specified key, or the default value if the key is not in the map. */
    public V get(K key, @Nullable V defaultValue){
        if(key == null) return defaultValue;
        int i = locateKey(key);
        return i < 0 ? defaultValue : valueTable[i];
    }

    /** Returns the value for the removed key, or null if the key is not in the map. */
    public @Nullable V remove(K key){
        int i = locateKey(key);
        if(i < 0) return null;
        K[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        V oldValue = valueTable[i];
        int mask = this.mask, next = i + 1 & mask;
        while((key = keyTable[next]) != null){
            int placement = place(key);
            if((next - placement & mask) > (i - placement & mask)){
                keyTable[i] = key;
                valueTable[i] = valueTable[next];
                i = next;
            }
            next = next + 1 & mask;
        }
        keyTable[i] = null;
        valueTable[i] = null;
        size--;
        return oldValue;
    }

    /** Returns true if the map has one or more items. */
    public boolean notEmpty(){
        return size > 0;
    }

    /** Returns true if the map is empty. */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * Reduces the size of the backing arrays to be the specified capacity / loadFactor, or less. If the capacity is already less,
     * nothing is done. If the map contains more items than the specified capacity, the next highest power of two capacity is used
     * instead.
     */
    public void shrink(int maximumCapacity){
        if(maximumCapacity < 0) throw new IllegalArgumentException("maximumCapacity must be >= 0: " + maximumCapacity);
        if(size > maximumCapacity) maximumCapacity = size;
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if(keyTable.length > tableSize) resize(tableSize);
    }

    /** Clears the map and reduces the size of the backing arrays to be the specified capacity / loadFactor, if they are larger. */
    public void clear(int maximumCapacity){
        int tableSize = tableSize(maximumCapacity, loadFactor);
        if(keyTable.length <= tableSize){
            clear();
            return;
        }
        size = 0;
        resize(tableSize);
    }

    public void clear(){
        if(size == 0) return;
        size = 0;
        Arrays.fill(keyTable, null);
        Arrays.fill(valueTable, null);
    }

    /**
     * Returns true if the specified value is in the map. Note this traverses the entire map and compares every value, which may
     * be an expensive operation.
     * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
     * {@link #equals(Object)}.
     */
    public boolean containsValue(@Nullable Object value, boolean identity){
        V[] valueTable = this.valueTable;
        if(value == null){
            K[] keyTable = this.keyTable;
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(keyTable[i] != null && valueTable[i] == null) return true;
        }else if(identity){
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(valueTable[i] == value) return true;
        }else{
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(value.equals(valueTable[i])) return true;
        }
        return false;
    }

    public boolean containsKey(K key){
        return locateKey(key) >= 0;
    }

    /**
     * Returns the key for the specified value, or null if it is not in the map. Note this traverses the entire map and compares
     * every value, which may be an expensive operation.
     * @param identity If true, uses == to compare the specified value with values in the map. If false, uses
     * {@link #equals(Object)}.
     */
    public @Nullable K findKey(@Nullable Object value, boolean identity){
        V[] valueTable = this.valueTable;
        if(value == null){
            K[] keyTable = this.keyTable;
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(keyTable[i] != null && valueTable[i] == null) return keyTable[i];
        }else if(identity){
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(valueTable[i] == value) return keyTable[i];
        }else{
            for(int i = valueTable.length - 1; i >= 0; i--)
                if(value.equals(valueTable[i])) return keyTable[i];
        }
        return null;
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items / loadFactor. Useful before
     * adding many items to avoid multiple backing array resizes.
     */
    public void ensureCapacity(int additionalCapacity){
        int tableSize = tableSize(size + additionalCapacity, loadFactor);
        if(keyTable.length < tableSize) resize(tableSize);
    }

    final void resize(int newSize){
        int oldCapacity = keyTable.length;
        threshold = (int)(newSize * loadFactor);
        mask = newSize - 1;
        shift = Long.numberOfLeadingZeros(mask);

        K[] oldKeyTable = keyTable;
        V[] oldValueTable = valueTable;

        keyTable = (K[])new Object[newSize];
        valueTable = (V[])new Object[newSize];

        if(size > 0){
            for(int i = 0; i < oldCapacity; i++){
                K key = oldKeyTable[i];
                if(key != null) putResize(key, oldValueTable[i]);
            }
        }
    }

    public int hashCode(){
        int h = size;
        K[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            K key = keyTable[i];
            if(key != null){
                h += key.hashCode();
                V value = valueTable[i];
                if(value != null) h += value.hashCode();
            }
        }
        return h;
    }

    public boolean equals(Object obj){
        if(obj == this) return true;
        if(!(obj instanceof ObjectMap)) return false;
        ObjectMap other = (ObjectMap)obj;
        if(other.size != size) return false;
        K[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        for(int i = 0, n = keyTable.length; i < n; i++){
            K key = keyTable[i];
            if(key != null){
                V value = valueTable[i];
                if(value == null){
                    if(other.get(key, dummy) != null) return false;
                }else{
                    if(!value.equals(other.get(key))) return false;
                }
            }
        }
        return true;
    }

    public String toString(String separator){
        return toString(separator, false);
    }

    @Override
    public String toString(){
        return toString(", ", true);
    }

    public String toString(String separator, boolean braces){
        if(size == 0) return braces ? "{}" : "";
        StringBuilder buffer = new StringBuilder(32);
        if(braces) buffer.append('{');
        K[] keyTable = this.keyTable;
        V[] valueTable = this.valueTable;
        int i = keyTable.length;
        while(i-- > 0){
            K key = keyTable[i];
            if(key == null) continue;
            buffer.append(key == this ? "(this)" : key);
            buffer.append('=');
            V value = valueTable[i];
            buffer.append(value == this ? "(this)" : value);
            break;
        }
        while(i-- > 0){
            K key = keyTable[i];
            if(key == null) continue;
            buffer.append(separator);
            buffer.append(key == this ? "(this)" : key);
            buffer.append('=');
            V value = valueTable[i];
            buffer.append(value == this ? "(this)" : value);
        }
        if(braces) buffer.append('}');
        return buffer.toString();
    }

    @Override
    public Entries<K, V> iterator(){
        return entries();
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     * <p>
     * Use the {@link Entries} constructor for nested or multithreaded iteration.
     */
    public Entries<K, V> entries(){
        if(entries1 == null){
            entries1 = new Entries<>(this);
            entries2 = new Entries<>(this);
        }
        if(!entries1.valid){
            entries1.reset();
            entries1.valid = true;
            entries2.valid = false;
            return entries1;
        }
        entries2.reset();
        entries2.valid = true;
        entries1.valid = false;
        return entries2;
    }

    /**
     * Returns an iterator for the values in the map. Remove is supported.
     * <p>
     * Use the {@link Values} constructor for nested or multithreaded iteration.
     */
    public Values<V> values(){
        if(values1 == null){
            values1 = new Values<>(this);
            values2 = new Values<>(this);
        }
        if(!values1.valid){
            values1.reset();
            values1.valid = true;
            values2.valid = false;
            return values1;
        }
        values2.reset();
        values2.valid = true;
        values1.valid = false;
        return values2;
    }

    /**
     * Returns an iterator for the keys in the map. Remove is supported.
     * <p>
     * Use the {@link Keys} constructor for nested or multithreaded iteration.
     */
    public Keys<K> keys(){
        if(keys1 == null){
            keys1 = new Keys(this);
            keys2 = new Keys(this);
        }
        if(!keys1.valid){
            keys1.reset();
            keys1.valid = true;
            keys2.valid = false;
            return keys1;
        }
        keys2.reset();
        keys2.valid = true;
        keys1.valid = false;
        return keys2;
    }

    static public class Entry<K, V>{
        public K key;
        public @Nullable V value;

        public String toString(){
            return key + "=" + value;
        }
    }

    static private abstract class MapIterator<K, V, I> implements Iterable<I>, Iterator<I>{
        public boolean hasNext;

        final ObjectMap<K, V> map;
        int nextIndex, currentIndex;
        boolean valid = true;

        public MapIterator(ObjectMap<K, V> map){
            this.map = map;
            reset();
        }

        public void reset(){
            currentIndex = -1;
            nextIndex = -1;
            findNextIndex();
        }

        void findNextIndex(){
            K[] keyTable = map.keyTable;
            for(int n = keyTable.length; ++nextIndex < n; ){
                if(keyTable[nextIndex] != null){
                    hasNext = true;
                    return;
                }
            }
            hasNext = false;
        }

        @Override
        public void remove(){
            int i = currentIndex;
            if(i < 0) throw new IllegalStateException("next must be called before remove.");
            K[] keyTable = map.keyTable;
            V[] valueTable = map.valueTable;
            int mask = map.mask, next = i + 1 & mask;
            K key;
            while((key = keyTable[next]) != null){
                int placement = map.place(key);
                if((next - placement & mask) > (i - placement & mask)){
                    keyTable[i] = key;
                    valueTable[i] = valueTable[next];
                    i = next;
                }
                next = next + 1 & mask;
            }
            keyTable[i] = null;
            valueTable[i] = null;
            map.size--;
            if(i != currentIndex) --nextIndex;
            currentIndex = -1;
        }
    }

    static public class Entries<K, V> extends MapIterator<K, V, Entry<K, V>>{
        Entry<K, V> entry = new Entry<>();

        public Entries(ObjectMap<K, V> map){
            super(map);
        }

        /** Note the same entry instance is returned each time this method is called. */
        @Override
        public Entry<K, V> next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K[] keyTable = map.keyTable;
            entry.key = keyTable[nextIndex];
            entry.value = map.valueTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return entry;
        }

        @Override
        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        @Override
        public Entries<K, V> iterator(){
            return this;
        }
    }

    static public class Values<V> extends MapIterator<Object, V, V>{
        public Values(ObjectMap<?, V> map){
            super((ObjectMap<Object, V>)map);
        }

        @Override
        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        @Override
        public @Nullable V next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            V value = map.valueTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return value;
        }

        @Override
        public Values<V> iterator(){
            return this;
        }

        /** Returns a new array containing the remaining values. */
        public Seq<V> toSeq(){
            return toSeq(new Seq<>(true, map.size));
        }

        /** Adds the remaining values to the specified array. */
        public Seq<V> toSeq(Seq<V> array){
            while(hasNext)
                array.add(next());
            return array;
        }
    }

    static public class Keys<K> extends MapIterator<K, Object, K>{
        public Keys(ObjectMap<K, ?> map){
            super((ObjectMap<K, Object>)map);
        }

        @Override
        public boolean hasNext(){
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            return hasNext;
        }

        @Override
        public K next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K key = map.keyTable[nextIndex];
            currentIndex = nextIndex;
            findNextIndex();
            return key;
        }

        @Override
        public Keys<K> iterator(){
            return this;
        }

        /** Returns a new array containing the remaining keys. */
        public Seq<K> toSeq(){
            return toSeq(new Seq<>(true, map.size));
        }

        /** Adds the remaining keys to the array. */
        public Seq<K> toSeq(Seq<K> array){
            while(hasNext)
                array.add(next());
            return array;
        }
    }
}
