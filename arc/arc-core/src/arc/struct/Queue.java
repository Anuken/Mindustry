package arc.struct;

import arc.func.*;
import arc.util.*;

import java.util.*;

/**
 * A resizable, ordered array of objects with efficient add and remove at the beginning and end. Values in the backing array may
 * wrap back to the beginning, making add and remove at the beginning and end O(1) (unless the backing array needs to resize when
 * adding). Deque functionality is provided via {@link #removeLast()} and {@link #addFirst(Object)}.
 */
@SuppressWarnings("unchecked")
public class Queue<T> implements Iterable<T>, Eachable<T>{
    /** Number of elements in the queue. */
    public int size = 0;
    /** Contains the values in the queue. Head and tail indices go in a circle around this array, wrapping at the end. */
    public T[] values;
    /** Index of first element. Logically smaller than tail. Unless empty, it points to a valid element inside queue. */
    protected int head = 0;
    /**
     * Index of last element. Logically bigger than head. Usually points to an empty position, but points to the head when full
     * (size == values.length).
     */
    protected int tail = 0;
    private @Nullable QueueIterable<T> iterable;

    /** Creates a new Queue which can hold 16 values without needing to resize backing array. */
    public Queue(){
        this(16);
    }

    /** Creates a new Queue which can hold the specified number of values without needing to resize backing array. */
    public Queue(int initialSize){
        // noinspection unchecked
        this.values = (T[])new Object[initialSize];
    }

    /**
     * Creates a new Queue which can hold the specified number of values without needing to resize backing array. This creates
     * backing array of the specified type via reflection, which is necessary only when accessing the backing array directly.
     */
    public Queue(int initialSize, Class<T> type){
        this.values = (T[])java.lang.reflect.Array.newInstance(type, initialSize);
    }

    public T[] toArray(Class<T> type){
        T[] out = (T[])java.lang.reflect.Array.newInstance(type, size);
        for(int i = 0; i < size; i++){
            out[i] = get(i);
        }
        return out;
    }

    /**
     * Append given object to the tail. (enqueue to tail) Unless backing array needs resizing, operates in O(1) time.
     * @param object can be null
     */
    public void addLast(T object){
        T[] values = this.values;

        if(size == values.length){
            resize(values.length << 1);// * 2
            values = this.values;
        }

        values[tail++] = object;
        if(tail == values.length){
            tail = 0;
        }
        size++;
    }

    /** Adds an object to the tail. */
    public void add(T object){
        addLast(object);
    }

    /**
     * Prepend given object to the head. (enqueue to head) Unless backing array needs resizing, operates in O(1) time.
     * @param object can be null
     * @see #addLast(Object)
     */
    public void addFirst(T object){
        T[] values = this.values;

        if(size == values.length){
            resize(values.length << 1);// * 2
            values = this.values;
        }

        int head = this.head;
        head--;
        if(head == -1){
            head = values.length - 1;
        }
        values[head] = object;

        this.head = head;
        this.size++;
    }

    /**
     * Reduces the size of the backing array to the size of the actual items. This is useful to release memory when many items
     * have been removed, or if it is known that more items will not be added.
     * @return {@link #values}
     */
    public T[] shrink(){
        if(values.length != size) resize(size);
        return values;
    }

    /**
     * Increases the size of the backing array to accommodate the specified number of additional items. Useful before adding many
     * items to avoid multiple backing array resizes.
     */
    public void ensureCapacity(int additional){
        final int needed = size + additional;
        if(values.length < needed){
            resize(needed);
        }
    }

    /** Resize backing array. newSize must be bigger than current size. */
    protected void resize(int newSize){
        final T[] values = this.values;
        final int head = this.head;
        final int tail = this.tail;

        final T[] newArray = (T[])java.lang.reflect.Array.newInstance(values.getClass().getComponentType(), newSize);
        if(head < tail){
            // Continuous
            System.arraycopy(values, head, newArray, 0, tail - head);
        }else if(size > 0){
            // Wrapped
            final int rest = values.length - head;
            System.arraycopy(values, head, newArray, 0, rest);
            System.arraycopy(values, 0, newArray, rest, tail);
        }
        this.values = newArray;
        this.head = 0;
        this.tail = size;
    }

    /**
     * Remove the first item from the queue. (dequeue from head) Always O(1).
     * @return removed object
     * @throws NoSuchElementException when queue is empty
     */
    public T removeFirst(){
        if(size == 0){
            // Underflow
            throw new NoSuchElementException("Queue is empty.");
        }

        final T[] values = this.values;

        final T result = values[head];
        values[head] = null;
        head++;
        if(head == values.length){
            head = 0;
        }
        size--;

        return result;
    }

    /**
     * Remove the last item from the queue. (dequeue from tail) Always O(1).
     * @return removed object
     * @throws NoSuchElementException when queue is empty
     * @see #removeFirst()
     */
    public T removeLast(){
        if(size == 0){
            throw new NoSuchElementException("Queue is empty.");
        }

        final T[] values = this.values;
        int tail = this.tail;
        tail--;
        if(tail == -1){
            tail = values.length - 1;
        }
        final T result = values[tail];
        values[tail] = null;
        this.tail = tail;
        size--;

        return result;
    }

    public boolean contains(T value){
        return contains(value, true);
    }

    public boolean contains(T value, boolean identity){
        return indexOf(value, identity) != -1;
    }

    /**
     * Returns the index of first occurrence of value in the queue, or -1 if no such value exists.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return An index of first occurrence of value in queue or -1 if no such value exists
     */
    public int indexOf(T value, boolean identity){
        if(size == 0) return -1;
        T[] values = this.values;
        final int head = this.head, tail = this.tail;
        if(identity || value == null){
            if(head < tail){
                for(int i = head; i < tail; i++)
                    if(values[i] == value) return i - head;
            }else{
                for(int i = head, n = values.length; i < n; i++)
                    if(values[i] == value) return i - head;
                for(int i = 0; i < tail; i++)
                    if(values[i] == value) return i + values.length - head;
            }
        }else{
            if(head < tail){
                for(int i = head; i < tail; i++)
                    if(value.equals(values[i])) return i - head;
            }else{
                for(int i = head, n = values.length; i < n; i++)
                    if(value.equals(values[i])) return i - head;
                for(int i = 0; i < tail; i++)
                    if(value.equals(values[i])) return i + values.length - head;
            }
        }
        return -1;
    }

    public int indexOf(Boolf<T> value){
        if(size == 0) return -1;
        T[] values = this.values;
        final int head = this.head, tail = this.tail;
        if(head < tail){
            for(int i = head; i < tail; i++)
                if(value.get(values[i])) return i - head;
        }else{
            for(int i = head, n = values.length; i < n; i++)
                if(value.get(values[i])) return i - head;
            for(int i = 0; i < tail; i++)
                if(value.get(values[i])) return i + values.length - head;
        }
        return -1;
    }

    public boolean remove(Boolf<T> value){
        int i = indexOf(value);
        if(i != -1){
            removeIndex(i);
            return true;
        }
        return false;
    }

    public boolean remove(T value){
        return remove(value, false);
    }

    /**
     * Removes the first instance of the specified value in the queue.
     * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
     * @return true if value was found and removed, false otherwise
     */
    public boolean remove(T value, boolean identity){
        int index = indexOf(value, identity);
        if(index == -1) return false;
        removeIndex(index);
        return true;
    }

    /** Removes and returns the item at the specified index. */
    public T removeIndex(int index){
        if(index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);

        T[] values = this.values;
        int head = this.head, tail = this.tail;
        index += head;
        T value;
        if(head < tail){ // index is between head and tail.
            value = values[index];
            System.arraycopy(values, index + 1, values, index, tail - index);
            values[tail] = null;
            this.tail--;
        }else if(index >= values.length){ // index is between 0 and tail.
            index -= values.length;
            value = values[index];
            System.arraycopy(values, index + 1, values, index, tail - index);
            this.tail--;
        }else{ // index is between head and values.length.
            value = values[index];
            System.arraycopy(values, head, values, head + 1, index - head);
            values[head] = null;
            this.head++;
            if(this.head == values.length){
                this.head = 0;
            }
        }
        size--;
        return value;
    }

    /** Returns true if the queue is empty. */
    public boolean isEmpty(){
        return size == 0;
    }

    /**
     * Returns the first (head) item in the queue (without removing it).
     * @throws NoSuchElementException when queue is empty
     * @see #addFirst(Object)
     * @see #removeFirst()
     */
    public T first(){
        if(size == 0){
            // Underflow
            throw new NoSuchElementException("Queue is empty.");
        }
        return values[head];
    }

    /**
     * Returns the last (tail) item in the queue (without removing it).
     * @throws NoSuchElementException when queue is empty
     * @see #addLast(Object)
     * @see #removeLast()
     */
    public T last(){
        if(size == 0){
            // Underflow
            throw new NoSuchElementException("Queue is empty.");
        }
        final T[] values = this.values;
        int tail = this.tail;
        tail--;
        if(tail == -1){
            tail = values.length - 1;
        }
        return values[tail];
    }

    /**
     * Retrieves the value in queue without removing it. Indexing is from the front to back, zero based. Therefore get(0) is the
     * same as {@link #first()}.
     * @throws IndexOutOfBoundsException when the index is negative or >= size
     */
    public T get(int index){
        if(index < 0) throw new IndexOutOfBoundsException("index can't be < 0: " + index);
        if(index >= size) throw new IndexOutOfBoundsException("index can't be >= size: " + index + " >= " + size);
        final T[] values = this.values;

        int i = head + index;
        if(i >= values.length){
            i -= values.length;
        }
        return values[i];
    }

    /**
     * Removes all values from this queue. Values in backing array are set to null to prevent memory leak, so this operates in
     * O(n).
     */
    public void clear(){
        if(size == 0) return;
        final T[] values = this.values;
        final int head = this.head;
        final int tail = this.tail;

        if(head < tail){
            // Continuous
            for(int i = head; i < tail; i++){
                values[i] = null;
            }
        }else{
            // Wrapped
            for(int i = head; i < values.length; i++){
                values[i] = null;
            }
            for(int i = 0; i < tail; i++){
                values[i] = null;
            }
        }
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    /**
     * Returns an iterator for the items in the queue. Remove is supported. Note that the same iterator instance is returned each
     * time this method is called. Use the constructor for nested or multithreaded iteration.
     */
    @Override
    public Iterator<T> iterator(){
        if(iterable == null) iterable = new QueueIterable<>(this);
        return iterable.iterator();
    }

    @Override
    public void each(Cons<? super T> c){
        final T[] values = this.values;

        for(int index = 0; index < size; index++){
            int i = head + index;
            if(i >= values.length){
                i -= values.length;
            }
            c.get(values[i]);
        }
    }

    public T find(Boolf<T> func){
        final T[] values = this.values;

        for(int index = 0; index < size; index++){
            int i = head + index;
            if(i >= values.length){
                i -= values.length;
            }
            T val = values[i];
            if(func.get(val)){
                return val;
            }
        }
        return null;
    }

    public String toString(){
        if(size == 0){
            return "[]";
        }
        final T[] values = this.values;
        final int head = this.head;
        final int tail = this.tail;

        StringBuilder sb = new StringBuilder(64);
        sb.append('[');
        sb.append(values[head]);
        for(int i = (head + 1) % values.length; i != tail; i = (i + 1) % values.length){
            sb.append(", ").append(values[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    public int hashCode(){
        final int size = this.size;
        final T[] values = this.values;
        final int backingLength = values.length;
        int index = this.head;

        int hash = size + 1;
        for(int s = 0; s < size; s++){
            final T value = values[index];

            hash *= 31;
            if(value != null) hash += value.hashCode();

            index++;
            if(index == backingLength) index = 0;
        }

        return hash;
    }

    public boolean equals(Object o){
        if(this == o) return true;
        if(!(o instanceof Queue)) return false;

        Queue<?> q = (Queue<?>)o;
        final int size = this.size;

        if(q.size != size) return false;

        final T[] myValues = this.values;
        final int myBackingLength = myValues.length;
        final Object[] itsValues = q.values;
        final int itsBackingLength = itsValues.length;

        int myIndex = head;
        int itsIndex = q.head;
        for(int s = 0; s < size; s++){
            T myValue = myValues[myIndex];
            Object itsValue = itsValues[itsIndex];

            if(!(myValue == null ? itsValue == null : myValue.equals(itsValue))) return false;
            myIndex++;
            itsIndex++;
            if(myIndex == myBackingLength) myIndex = 0;
            if(itsIndex == itsBackingLength) itsIndex = 0;
        }
        return true;
    }

    public static class QueueIterable<T> implements Iterable<T>{
        final Queue<T> queue;
        final boolean allowRemove;
        private QueueIterator iterator1, iterator2;

        public QueueIterable(Queue<T> queue){
            this(queue, true);
        }

        public QueueIterable(Queue<T> queue, boolean allowRemove){
            this.queue = queue;
            this.allowRemove = allowRemove;
        }

        @Override
        public Iterator<T> iterator(){
            if(iterator1 == null){
                iterator1 = new QueueIterator();
                iterator2 = new QueueIterator();
            }

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
            return new QueueIterator();
        }

        private class QueueIterator implements Iterator<T>, Iterable<T>{
            int index;
            boolean done = true;

            QueueIterator(){
            }

            @Override
            public boolean hasNext(){
                if(index >= queue.size) done = true;
                return index < queue.size;
            }

            @Override
            public T next(){
                if(index >= queue.size) throw new NoSuchElementException(String.valueOf(index));
                return queue.get(index++);
            }

            @Override
            public void remove(){
                if(!allowRemove) throw new ArcRuntimeException("Remove not allowed.");
                index--;
                queue.removeIndex(index);
            }

            public void reset(){
                index = 0;
            }

            @Override
            public Iterator<T> iterator(){
                return this;
            }
        }
    }
}
