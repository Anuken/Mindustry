package arc.struct;

import arc.util.*;

import java.util.*;

/**
 * An {@link ObjectMap} that also stores keys in an {@link Seq} using the insertion order. Iteration over the
 * {@link #entries()}, {@link #keys()}, and {@link #values()} is ordered and faster than an unordered map. Keys can also be
 * accessed and the order changed using {@link #orderedKeys()}. There is some additional overhead for put and remove. When used
 * for faster iteration versus ObjectMap and the order does not actually matter, copying during remove can be greatly reduced by
 * setting {@link Seq#ordered} to false for {@link OrderedMap#orderedKeys()}.
 * @author Nathan Sweet
 */
public class OrderedMap<K, V> extends ObjectMap<K, V>{
    final Seq<K> keys;

    public static <K, V> OrderedMap<K, V> of(Object... values){
        OrderedMap<K, V> map = new OrderedMap<>();

        for(int i = 0; i < values.length / 2; i++){
            map.put((K)values[i * 2], (V)values[i * 2 + 1]);
        }

        return map;
    }

    /** Creates a new map with an initial capacity of 51 and a load factor of 0.8. */
    public OrderedMap(){
        keys = new Seq<>();
    }

    /**
     * Creates a new map with a load factor of 0.8.
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two.
     */
    public OrderedMap(int initialCapacity){
        super(initialCapacity);
        keys = new Seq<>(initialCapacity);
    }

    /**
     * Creates a new map with the specified initial capacity and load factor. This map will hold initialCapacity items before
     * growing the backing table.
     * @param initialCapacity The backing array size is initialCapacity / loadFactor, increased to the next power of two.
     */
    public OrderedMap(int initialCapacity, float loadFactor){
        super(initialCapacity, loadFactor);
        keys = new Seq<>(initialCapacity);
    }

    /** Creates a new map containing the items in the specified map. */
    public OrderedMap(OrderedMap<? extends K, ? extends V> map){
        super(map);
        keys = new Seq<>(map.keys);
    }

    @Override
    public V put(K key, V value){
        int i = locateKey(key);
        if(i >= 0){ // Existing key was found.
            V oldValue = valueTable[i];
            valueTable[i] = value;
            return oldValue;
        }
        i = -(i + 1); // Empty space was found.
        keyTable[i] = key;
        valueTable[i] = value;
        keys.add(key);
        if(++size >= threshold) resize(keyTable.length << 1);
        return null;
    }

    @Override
    public @Nullable V putMissing(K key, @Nullable V value){
        int i = locateKey(key);
        if(i >= 0) return valueTable[i]; // Existing key was found.
        i = -(i + 1); // Empty space was found.
        keyTable[i] = key;
        valueTable[i] = value;
        keys.add(key);
        if(++size >= threshold) resize(keyTable.length << 1);
        return null;
    }

    public <T extends K> void putAll(OrderedMap<T, ? extends V> map){
        ensureCapacity(map.size);
        K[] keys = map.keys.items;
        for(int i = 0, n = map.keys.size; i < n; i++){
            K key = keys[i];
            put(key, map.get((T)key));
        }
    }

    @Override
    public V remove(K key){
        keys.remove(key, false);
        return super.remove(key);
    }

    public V removeIndex(int index){
        return super.remove(keys.remove(index));
    }

    /**
     * Changes the key {@code before} to {@code after} without changing its position in the order or its value. Returns true if
     * {@code after} has been added to the OrderedMap and {@code before} has been removed; returns false if {@code after} is
     * already present or {@code before} is not present. If you are iterating over an OrderedMap and have an index, you should
     * prefer {@link #alterIndex(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
     * @param before a key that must be present for this to succeed
     * @param after a key that must not be in this map for this to succeed
     * @return true if {@code before} was removed and {@code after} was added, false otherwise
     */
    public boolean alter(K before, K after){
        if(containsKey(after)) return false;
        int index = keys.indexOf(before, false);
        if(index == -1) return false;
        super.put(after, super.remove(before));
        keys.set(index, after);
        return true;
    }

    /**
     * Changes the key at the given {@code index} in the order to {@code after}, without changing the ordering of other entries or
     * any values. If {@code after} is already present, this returns false; it will also return false if {@code index} is invalid
     * for the size of this map. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
     * @param index the index in the order of the key to change; must be non-negative and less than {@link #size}
     * @param after the key that will replace the contents at {@code index}; this key must not be present for this to succeed
     * @return true if {@code after} successfully replaced the key at {@code index}, false otherwise
     */
    public boolean alterIndex(int index, K after){
        if(index < 0 || index >= size || containsKey(after)) return false;
        super.put(after, super.remove(keys.get(index)));
        keys.set(index, after);
        return true;
    }

    @Override
    public void clear(int maximumCapacity){
        keys.clear();
        super.clear(maximumCapacity);
    }

    @Override
    public void clear(){
        keys.clear();
        super.clear();
    }

    public Seq<K> orderedKeys(){
        return keys;
    }

    @Override
    public Entries<K, V> iterator(){
        return entries();
    }

    /**
     * Returns an iterator for the entries in the map. Remove is supported.
     * <p>
     * Use the {@link OrderedMapEntries} constructor for nested or multithreaded iteration.
     */
    @Override
    public Entries<K, V> entries(){
        if(entries1 == null){
            entries1 = new OrderedMapEntries<>(this);
            entries2 = new OrderedMapEntries<>(this);
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
     * Use the {@link OrderedMapValues} constructor for nested or multithreaded iteration.
     */
    @Override
    public Values<V> values(){
        if(values1 == null){
            values1 = new OrderedMapValues<>(this);
            values2 = new OrderedMapValues<>(this);
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
     * Use the {@link OrderedMapKeys} constructor for nested or multithreaded iteration.
     */
    @Override
    public Keys<K> keys(){
        if(keys1 == null){
            keys1 = new OrderedMapKeys<>(this);
            keys2 = new OrderedMapKeys<>(this);
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

    @Override
    public String toString(String separator, boolean braces){
        if(size == 0) return braces ? "{}" : "";
        StringBuilder buffer = new StringBuilder(32);
        if(braces) buffer.append('{');
        Seq<K> keys = this.keys;
        for(int i = 0, n = keys.size; i < n; i++){
            K key = keys.get(i);
            if(i > 0) buffer.append(separator);
            buffer.append(key == this ? "(this)" : key);
            buffer.append('=');
            V value = get(key);
            buffer.append(value == this ? "(this)" : value);
        }
        if(braces) buffer.append('}');
        return buffer.toString();
    }

    public static class OrderedMapEntries<K, V> extends Entries<K, V>{
        private Seq<K> keys;

        public OrderedMapEntries(OrderedMap<K, V> map){
            super(map);
            keys = map.keys;
        }

        @Override
        public void reset(){
            currentIndex = -1;
            nextIndex = 0;
            hasNext = map.size > 0;
        }

        @Override
        public Entry<K, V> next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            currentIndex = nextIndex;
            entry.key = keys.get(nextIndex);
            entry.value = map.get(entry.key);
            nextIndex++;
            hasNext = nextIndex < map.size;
            return entry;
        }

        @Override
        public void remove(){
            if(currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
            map.remove(entry.key);
            nextIndex--;
            currentIndex = -1;
        }
    }

    public static class OrderedMapKeys<K> extends Keys<K>{
        private Seq<K> keys;

        public OrderedMapKeys(OrderedMap<K, ?> map){
            super(map);
            keys = map.keys;
        }

        @Override
        public void reset(){
            currentIndex = -1;
            nextIndex = 0;
            hasNext = map.size > 0;
        }

        @Override
        public K next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K key = keys.get(nextIndex);
            currentIndex = nextIndex;
            nextIndex++;
            hasNext = nextIndex < map.size;
            return key;
        }

        @Override
        public void remove(){
            if(currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
            ((OrderedMap)map).removeIndex(currentIndex);
            nextIndex = currentIndex;
            currentIndex = -1;
        }

        @Override
        public Seq<K> toSeq(Seq<K> array){
            array.addAll(keys, nextIndex, keys.size - nextIndex);
            nextIndex = keys.size;
            hasNext = false;
            return array;
        }

        @Override
        public Seq<K> toSeq(){
            return toSeq(new Seq<>(true, keys.size - nextIndex));
        }
    }

    public static class OrderedMapValues<V> extends Values<V>{
        private Seq<V> keys;

        public OrderedMapValues(OrderedMap<?, V> map){
            super(map);
            keys = (Seq<V>)map.keys;
        }

        @Override
        public void reset(){
            currentIndex = -1;
            nextIndex = 0;
            hasNext = map.size > 0;
        }

        @Override
        public V next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            V value = map.get(keys.get(nextIndex));
            currentIndex = nextIndex;
            nextIndex++;
            hasNext = nextIndex < map.size;
            return value;
        }

        @Override
        public void remove(){
            if(currentIndex < 0) throw new IllegalStateException("next must be called before remove.");
            ((OrderedMap)map).removeIndex(currentIndex);
            nextIndex = currentIndex;
            currentIndex = -1;
        }

        @Override
        public Seq<V> toSeq(Seq<V> array){
            int n = keys.size;
            array.ensureCapacity(n - nextIndex);
            Object[] keys = this.keys.items;
            for(int i = nextIndex; i < n; i++)
                array.add(map.get(keys[i]));
            currentIndex = n - 1;
            nextIndex = n;
            hasNext = false;
            return array;
        }

        @Override
        public Seq<V> toSeq(){
            return toSeq(new Seq<>(true, keys.size - nextIndex));
        }
    }
}
