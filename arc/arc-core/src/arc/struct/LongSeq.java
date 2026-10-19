package arc.struct;

import arc.math.Mathf;

import java.util.Arrays;

/**
 * A resizable, ordered or unordered long array. Avoids the boxing that occurs with ArrayList<Long>. If unordered, this class
 * avoids a memory copy when removing elements (the last element is moved to the removed element's position).
 * @author Nathan Sweet
 */
public class LongSeq{
    public long[] items;
    public int size;
    public boolean ordered;

    /** Creates an ordered array with a capacity of 16. */
    public LongSeq(){
        this(true, 16);
    }

    /** Creates an ordered array with the specified capacity. */
    public LongSeq(int capacity){
        this(true, capacity);
    }

    /**
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     * @param capacity Any elements added beyond this will cause the backing array to be grown.
     */
    public LongSeq(boolean ordered, int capacity){
        this.ordered = ordered;
        items = new long[capacity];
    }

    /**
     * Creates a new array containing the elements in the specific array. The new array will be ordered if the specific array is
     * ordered. The capacity is set to the number of elements, so any subsequent elements added will cause the backing array to be
     * grown.
     */
    public LongSeq(LongSeq array){
        this.ordered = array.ordered;
        size = array.size;
        items = new long[size];
        System.arraycopy(array.items, 0, items, 0, size);
    }

    /**
     * Creates a new ordered array containing the elements in the specified array. The capacity is set to the number of elements,
     * so any subsequent elements added will cause the backing array to be grown.
     */
    public LongSeq(long[] array){
        this(true, array, 0, array.length);
    }

    /**
     * Creates a new array containing the elements in the specified array. The capacity is set to the number of elements, so any
     * subsequent elements added will cause the backing array to be grown.
     * @param ordered If false, methods that remove elements may change the order of other elements in the array, which avoids a
     * memory copy.
     */
    public LongSeq(boolean ordered, long[] array, int startIndex, int count){
        this(ordered, count);
        size = count;
        System.arraycopy(array, startIndex, items, 0, count);
    }

    /** @see #LongSeq(long[]) */
    public static LongSeq with(long... array){
        return new LongSeq(array);
    }

    public void add(long value){
        long[] items = this.items;
        if(size == items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size++] = value;
    }

    public void add(long value1, long value2){
        long[] items = this.items;
        if(size + 1 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size] = value1;
        items[size + 1] = value2;
        size += 2;
    }

    public void add(long value1, long value2, long value3){
        long[] items = this.items;
        if(size + 2 >= items.length) items = resize(Math.max(8, (int)(size * 1.75f)));
        items[size] = value1;
        items[size + 1] = value2;
        items[size + 2] = value3;
        size += 3;
    }

    public void add(long value1, long value2, long value3, long value4){
        long[] items = this.items;
        if(size + 3 >= items.length) items = resize(Math.max(8, (int)(size * 1.8f))); // 1.75 isn't enough when size=5.
        items[size] = value1;
        items[size + 1] = value2;
        items[size + 2] = value3;
        items[size + 3] = value4;
        size += 4;
    }

    public void addAll(LongSeq array){
        addAll(array.items, 0, array.size);
    }

    public void addAll(LongSeq array, int offset, int length){
        if(offset + length > array.size)
            throw new IllegalArgumentException("offset + length must be <= size: " + offset + " + " + length + " <= " + array.size);
        addAll(array.items, offset, length);
    }

    public void addAll(long... array){
        addAll(array, 0, array.length);
    }

    public void addAll(long[] array, int offset, int length){
        long[] items = this.items;
        int sizeNeeded = size + length;
        if(sizeNeeded > items.length) items = resize(Math.max(8, (int)(sizeNeeded * 1.75f)));
        System.arraycopy(array, offset, items, size, length);
        size += length;
    }

    public long get(int index){
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        return items[index];
    }

    public void set(int index, long value){
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] = value;
    }

    public void incr(int index, long value){
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] += value;
    }

    public void mul(int index, long value){
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        items[index] *= value;
    }

    public void insert(int index, long value){
        if(index > size) throw new IndexOutOfBoundsException("index can't be > size: " + index + " > " + size);
        long[] items = this.items;
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
        long[] items = this.items;
        long firstValue = items[first];
        items[first] = items[second];
        items[second] = firstValue;
    }

    public boolean contains(long value){
        int i = size - 1;
        long[] items = this.items;
        while(i >= 0)
            if(items[i--] == value) return true;
        return false;
    }

    public int indexOf(long value){
        long[] items = this.items;
        for(int i = 0, n = size; i < n; i++)
            if(items[i] == value) return i;
        return -1;
    }

    public int lastIndexOf(char value){
        long[] items = this.items;
        for(int i = size - 1; i >= 0; i--)
            if(items[i] == value) return i;
        return -1;
    }

    public boolean removeValue(long value){
        long[] items = this.items;
        for(int i = 0, n = size; i < n; i++){
            if(items[i] == value){
                removeIndex(i);
                return true;
            }
        }
        return false;
    }

    /** Removes and returns the item at the specified index. */
    public long removeIndex(int index){
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        long[] items = this.items;
        long value = items[index];
        size--;
        if(ordered)
            System.arraycopy(items, index + 1, items, index, size - index);
        else
            items[index] = items[size];
        return value;
    }

    /** Removes the items between the specified indices, inclusive. */
    public void removeRange(int start, int end){
        if(end >= size) throw new IndexOutOfBoundsException("end can't be >= size: " + end + " >= " + size);
        if(start > end) throw new IndexOutOfBoundsException("start can't be > end: " + start + " > " + end);
        long[] items = this.items;
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

    /**
     * Removes from this array all of elements contained in the specified array.
     * @return true if this array was modified.
     */
    public boolean removeAll(LongSeq array){
        int size = this.size;
        int startSize = size;
        long[] items = this.items;
        for(int i = 0, n = array.size; i < n; i++){
            long item = array.get(i);
            for(int ii = 0; ii < size; ii++){
                if(item == items[ii]){
                    removeIndex(ii);
                    size--;
                    break;
                }
            }
        }
        return size != startSize;
    }

    /** Removes and returns the last item. */
    public long pop(){
        return items[--size];
    }

    /** Returns the last item. */
    public long peek(){
        return items[size - 1];
    }

    /** Returns the first item. */
    public long first(){
        if(size == 0) throw new IllegalStateException("Array is empty.");
        return items[0];
    }

    /** Returns true if the array is empty. */
    public boolean isEmpty(){
        return size == 0;
    }

    public void clear(){
        size = 0;
    }

    /**
     * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     * @return {@link #items}
     */
    public long[] shrink(){
        if(items.length != size) resize(size);
        return items;
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     * @return {@link #items}
     */
    public long[] ensureCapacity(int additionalCapacity){
        if(additionalCapacity < 0)
            throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
        int sizeNeeded = size + additionalCapacity;
        if(sizeNeeded > items.length) resize(Math.max(8, sizeNeeded));
        return items;
    }

    /**
     * Sets the array size, leaving any values beyond the current size undefined.
     * @return {@link #items}
     */
    public long[] setSize(int newSize){
        if(newSize < 0) throw new IllegalArgumentException("newSize must be >= 0: " + newSize);
        if(newSize > items.length) resize(Math.max(8, newSize));
        size = newSize;
        return items;
    }

    protected long[] resize(int newSize){
        long[] newItems = new long[newSize];
        long[] items = this.items;
        System.arraycopy(items, 0, newItems, 0, Math.min(size, newItems.length));
        this.items = newItems;
        return newItems;
    }

    public void sort(){
        Arrays.sort(items, 0, size);
    }

    public void reverse(){
        long[] items = this.items;
        for(int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++){
            int ii = lastIndex - i;
            long temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    public void shuffle(){
        long[] items = this.items;
        for(int i = size - 1; i >= 0; i--){
            int ii = Mathf.random(i);
            long temp = items[i];
            items[i] = items[ii];
            items[ii] = temp;
        }
    }

    /**
     * Reduces the size of the array to the specified size. If the array is already smaller than the specified size, no action is
     * taken.
     */
    public void truncate(int newSize){
        if(size > newSize) size = newSize;
    }

    /** Returns a random item from the array, or zero if the array is empty. */
    public long random(){
        if(size == 0) return 0;
        return items[Mathf.random(0, size - 1)];
    }

    public long[] toArray(){
        long[] array = new long[size];
        System.arraycopy(items, 0, array, 0, size);
        return array;
    }

    public int hashCode(){
        if(!ordered) return super.hashCode();
        long[] items = this.items;
        int h = 1;
        for(int i = 0, n = size; i < n; i++)
            h = h * 31 + (int)(items[i] ^ (items[i] >>> 32));
        return h;
    }

    public boolean equals(Object object){
        if(object == this) return true;
        if(!ordered) return false;
        if(!(object instanceof LongSeq)) return false;
        LongSeq array = (LongSeq)object;
        if(!array.ordered) return false;
        int n = size;
        if(n != array.size) return false;
        long[] items1 = this.items;
        long[] items2 = array.items;
        for(int i = 0; i < n; i++)
            if(items[i] != array.items[i]) return false;
        return true;
    }

    public String toString(){
        if(size == 0) return "[]";
        long[] items = this.items;
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

    public String toString(String separator){
        if(size == 0) return "";
        long[] items = this.items;
        StringBuilder buffer = new StringBuilder(32);
        buffer.append(items[0]);
        for(int i = 1; i < size; i++){
            buffer.append(separator);
            buffer.append(items[i]);
        }
        return buffer.toString();
    }
}
