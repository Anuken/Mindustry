package arc.struct;

import arc.util.*;

import java.util.*;

/**
 * An {@link ObjectSet} that also stores keys in an {@link Seq} using the insertion order. {@link #iterator() Iteration} is
 * ordered and faster than an unordered set. Keys can also be accessed and the order changed using {@link #orderedItems()}. There
 * is some additional overhead for put and remove. When used for faster iteration versus ObjectSet and the order does not actually
 * matter, copying during remove can be greatly reduced by setting {@link Seq#ordered} to false for
 * {@link OrderedSet#orderedItems()}.
 * @author Nathan Sweet
 */

public class OrderedSet<T> extends ObjectSet<T>{
    final Seq<T> items;
    transient OrderedSetIterator iterator1, iterator2;

    public OrderedSet(){
        items = new Seq();
    }

    public OrderedSet(int initialCapacity, float loadFactor){
        super(initialCapacity, loadFactor);
        items = new Seq(initialCapacity);
    }

    public OrderedSet(int initialCapacity){
        super(initialCapacity);
        items = new Seq(initialCapacity);
    }

    public OrderedSet(OrderedSet<? extends T> set){
        super(set);
        items = new Seq(set.items);
    }

    public boolean add(T key){
        if(!super.add(key)) return false;
        items.add(key);
        return true;
    }

    /**
     * Sets the key at the specfied index. Returns true if the key was added to the set or false if it was already in the set. If
     * this set already contains the key, the existing key's index is changed if needed and false is returned.
     */
    public boolean add(T key, int index){
        if(!super.add(key)){
            int oldIndex = items.indexOf(key, true);
            if(oldIndex != index) items.insert(index, items.remove(oldIndex));
            return false;
        }
        items.insert(index, key);
        return true;
    }

    public void addAll(OrderedSet<T> set){
        ensureCapacity(set.size);
        T[] keys = set.items.items;
        for(int i = 0, n = set.items.size; i < n; i++)
            add(keys[i]);
    }

    public void ensureCapacity(int additionalCapacity){
        super.ensureCapacity(additionalCapacity);
        items.ensureCapacity(additionalCapacity);
    }

    public boolean remove(T key){
        if(!super.remove(key)) return false;
        items.remove(key, false);
        return true;
    }

    public T removeIndex(int index){
        T key = items.remove(index);
        super.remove(key);
        return key;
    }

    /**
     * Changes the item {@code before} to {@code after} without changing its position in the order. Returns true if {@code after}
     * has been added to the OrderedSet and {@code before} has been removed; returns false if {@code after} is already present or
     * {@code before} is not present. If you are iterating over an OrderedSet and have an index, you should prefer
     * {@link #alterIndex(int, Object)}, which doesn't need to search for an index like this does and so can be faster.
     * @param before an item that must be present for this to succeed
     * @param after an item that must not be in this set for this to succeed
     * @return true if {@code before} was removed and {@code after} was added, false otherwise
     */
    public boolean alter(T before, T after){
        if(contains(after)) return false;
        if(!super.remove(before)) return false;
        super.add(after);
        items.set(items.indexOf(before, false), after);
        return true;
    }

    /**
     * Changes the item at the given {@code index} in the order to {@code after}, without changing the ordering of other items. If
     * {@code after} is already present, this returns false; it will also return false if {@code index} is invalid for the size of
     * this set. Otherwise, it returns true. Unlike {@link #alter(Object, Object)}, this operates in constant time.
     * @param index the index in the order of the item to change; must be non-negative and less than {@link #size}
     * @param after the item that will replace the contents at {@code index}; this item must not be present for this to succeed
     * @return true if {@code after} successfully replaced the contents at {@code index}, false otherwise
     */
    public boolean alterIndex(int index, T after){
        if(index < 0 || index >= size || contains(after)) return false;
        super.remove(items.get(index));
        super.add(after);
        items.set(index, after);
        return true;
    }

    public void clear(int maximumCapacity){
        items.clear();
        super.clear(maximumCapacity);
    }

    public void clear(){
        items.clear();
        super.clear();
    }

    public Seq<T> orderedItems(){
        return items;
    }

    public T first(){
        return items.first();
    }

    public int hashCode(){
        int h = size;
        T[] items = this.items.items;
        for(int i = 0, n = this.items.size; i < n; i++){
            T key = items[i];
            if(key != null) h += key.hashCode();
        }
        return h;
    }

    public boolean equals(Object obj){
        if(!(obj instanceof ObjectSet)) return false;
        ObjectSet other = (ObjectSet)obj;
        if(other.size != size) return false;
        T[] items = this.items.items;
        for(int i = 0, n = this.items.size; i < n; i++)
            if(items[i] != null && !other.contains(items[i])) return false;
        return true;
    }

    public OrderedSetIterator<T> iterator(){
        if(iterator1 == null){
            iterator1 = new OrderedSetIterator(this);
            iterator2 = new OrderedSetIterator(this);
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

    public String toString(){
        if(size == 0) return "{}";
        T[] items = this.items.items;
        java.lang.StringBuilder buffer = new java.lang.StringBuilder(32);
        buffer.append('{');
        buffer.append(items[0]);
        for(int i = 1; i < size; i++){
            buffer.append(", ");
            buffer.append(items[i]);
        }
        buffer.append('}');
        return buffer.toString();
    }

    public String toString(String separator){
        return items.toString(separator);
    }

    static public class OrderedSetIterator<K> extends ObjectSetIterator<K>{
        private Seq<K> items;

        public OrderedSetIterator(OrderedSet<K> set){
            super(set);
            items = set.items;
        }

        public void reset(){
            nextIndex = 0;
            hasNext = set.size > 0;
        }

        public K next(){
            if(!hasNext) throw new NoSuchElementException();
            if(!valid) throw new ArcRuntimeException("#iterator() cannot be used nested.");
            K key = items.get(nextIndex);
            nextIndex++;
            hasNext = nextIndex < set.size;
            return key;
        }

        public void remove(){
            if(nextIndex < 0) throw new IllegalStateException("next must be called before remove.");
            nextIndex--;
            ((OrderedSet)set).removeIndex(nextIndex);
        }

        public Seq<K> toSeq(Seq<K> array){
            array.addAll(items, nextIndex, items.size - nextIndex);
            nextIndex = items.size;
            hasNext = false;
            return array;
        }

        public Seq<K> toSeq(){
            return toSeq(new Seq(true, set.size - nextIndex));
        }
    }

    static public <T> OrderedSet<T> with(T... array){
        OrderedSet<T> set = new OrderedSet<T>();
        set.addAll(array);
        return set;
    }
}
