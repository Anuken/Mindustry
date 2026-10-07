package arc.struct;

/**
 * A bitset, without size limitation, allows comparison via bitwise operators to other bitfields.
 * @author mzechner
 * @author jshapcott
 */
public class Bits{
    long[] bits = {0};

    public Bits(){
    }

    /**
     * Creates a bit set whose initial size is large enough to explicitly represent bits with indices in the range 0 through
     * nbits-1.
     * @param nbits the initial size of the bit set
     */
    public Bits(int nbits){
        checkCapacity(nbits >>> 6);
    }

    /** Sets this bits to have the same bits as another. Both sets should have the same length. */
    public void set(Bits other){
        int length = Math.min(bits.length, other.bits.length);
        System.arraycopy(other.bits, 0, bits, 0, length);
    }

    /**
     * @param index the index of the bit
     * @return whether the bit is set
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    public boolean get(int index){
        final int word = index >>> 6;
        //TODO why does the original source to index & 0x3F, and why doesn't java.util.BitSet?
        return word < bits.length && (bits[word] & (1L << (index))) != 0L;
    }

    /**
     * Returns the bit at the given index and clears it in one go.
     * @param index the index of the bit
     * @return whether the bit was set before invocation
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    public boolean getAndClear(int index){
        final int word = index >>> 6;
        if(word >= bits.length) return false;
        long oldBits = bits[word];
        bits[word] &= ~(1L << (index & 0x3F));
        return bits[word] != oldBits;
    }

    /**
     * Returns the bit at the given index and sets it in one go.
     * @param index the index of the bit
     * @return whether the bit was set before invocation
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    public boolean getAndSet(int index){
        final int word = index >>> 6;
        checkCapacity(word);
        long oldBits = bits[word];
        bits[word] |= 1L << (index & 0x3F);
        return bits[word] == oldBits;
    }

    public void set(int index, boolean value){
        if(value){
            set(index);
        }else{
            clear(index);
        }
    }

    /**
     * @param index the index of the bit to set
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    public void set(int index){
        final int word = index >>> 6;
        checkCapacity(word);
        bits[word] |= 1L << (index & 0x3F);
    }

    /**
     * @param from index to start from, inclusive.
     * @param to index to end at, exclusive.
     * */
    public void set(int from, int to){
        if(from == to) return;

        int startWordIndex = from >>> 6;
        int endWordIndex = (to - 1) >>> 6;
        checkCapacity(endWordIndex);

        long mask = 0xffffffffffffffffL;
        long firstWordMask = mask << from;
        long lastWordMask = mask >>> -to;

        if(startWordIndex == endWordIndex){
            // Case 1: One word
            bits[startWordIndex] |= (firstWordMask & lastWordMask);
        }else{
            // Case 2: Multiple words
            // Handle first word
            bits[startWordIndex] |= firstWordMask;

            // Handle intermediate words, if any
            for(int i = startWordIndex + 1; i < endWordIndex; i++)
                bits[i] = mask;

            // Handle last word (restores invariants)
            bits[endWordIndex] |= lastWordMask;
        }
    }

    /** @param index the index of the bit to flip */
    public void flip(int index){
        final int word = index >>> 6;
        checkCapacity(word);
        bits[word] ^= 1L << (index & 0x3F);
    }

    private void checkCapacity(int len){
        if(len >= bits.length){
            long[] newBits = new long[len + 1];
            System.arraycopy(bits, 0, newBits, 0, bits.length);
            bits = newBits;
        }
    }

    /**
     * @param index the index of the bit to clear
     * @throws ArrayIndexOutOfBoundsException if index < 0
     */
    public void clear(int index){
        final int word = index >>> 6;
        if(word >= bits.length) return;
        bits[word] &= ~(1L << (index & 0x3F));
    }

    /** Clears the entire bitset */
    public void clear(){
        long[] bits = this.bits;
        int length = bits.length;
        for(int i = 0; i < length; i++){
            bits[i] = 0L;
        }
    }

    /** @return the number of bits currently stored, <b>not</b> the highset set bit! */
    public int numBits(){
        return bits.length << 6;
    }

    /**
     * Returns the "logical size" of this bitset: the index of the highest set bit in the bitset plus one. Returns zero if the
     * bitset contains no set bits.
     * @return the logical size of this bitset
     */
    public int length(){
        long[] bits = this.bits;
        for(int word = bits.length - 1; word >= 0; --word){
            long bitsAtWord = bits[word];
            if(bitsAtWord != 0){
                for(int bit = 63; bit >= 0; --bit){
                    if((bitsAtWord & (1L << (bit & 0x3F))) != 0L){
                        return (word << 6) + bit + 1;
                    }
                }
            }
        }
        return 0;
    }

    /** @return true if this bitset contains no bits that are set to true */
    public boolean isEmpty(){
        long[] bits = this.bits;
        int length = bits.length;
        for(int i = 0; i < length; i++){
            if(bits[i] != 0L){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index of the first bit that is set to true that occurs on or after the specified starting index. If no such bit
     * exists then -1 is returned.
     */
    public int nextSetBit(int fromIndex){
        long[] bits = this.bits;
        int word = fromIndex >>> 6;
        int bitsLength = bits.length;
        if(word >= bitsLength) return -1;
        long bitsAtWord = bits[word];
        if(bitsAtWord != 0){
            for(int i = fromIndex & 0x3f; i < 64; i++){
                if((bitsAtWord & (1L << (i & 0x3F))) != 0L){
                    return (word << 6) + i;
                }
            }
        }
        for(word++; word < bitsLength; word++){
            if(word != 0){
                bitsAtWord = bits[word];
                if(bitsAtWord != 0){
                    for(int i = 0; i < 64; i++){
                        if((bitsAtWord & (1L << (i & 0x3F))) != 0L){
                            return (word << 6) + i;
                        }
                    }
                }
            }
        }
        return -1;
    }

    /** Returns the index of the first bit that is set to false that occurs on or after the specified starting index. */
    public int nextClearBit(int fromIndex){
        long[] bits = this.bits;
        int word = fromIndex >>> 6;
        int bitsLength = bits.length;
        if(word >= bitsLength) return bits.length << 6;
        long bitsAtWord = bits[word];
        for(int i = fromIndex & 0x3f; i < 64; i++){
            if((bitsAtWord & (1L << (i & 0x3F))) == 0L){
                return (word << 6) + i;
            }
        }
        for(word++; word < bitsLength; word++){
            if(word == 0){
                return word << 6;
            }
            bitsAtWord = bits[word];
            for(int i = 0; i < 64; i++){
                if((bitsAtWord & (1L << (i & 0x3F))) == 0L){
                    return (word << 6) + i;
                }
            }
        }
        return bits.length << 6;
    }

    /**
     * Performs a logical <b>AND</b> of this target bit set with the argument bit set. This bit set is modified so that each bit in
     * it has the value true if and only if it both initially had the value true and the corresponding bit in the bit set argument
     * also had the value true.
     * @param other a bit set
     */
    public void and(Bits other){
        int commonWords = Math.min(bits.length, other.bits.length);
        for(int i = 0; commonWords > i; i++){
            bits[i] &= other.bits[i];
        }

        if(bits.length > commonWords){
            for(int i = commonWords, s = bits.length; s > i; i++){
                bits[i] = 0L;
            }
        }
    }

    /**
     * Clears all of the bits in this bit set whose corresponding bit is set in the specified bit set.
     * @param other a bit set
     */
    public void andNot(Bits other){
        for(int i = 0, j = bits.length, k = other.bits.length; i < j && i < k; i++){
            bits[i] &= ~other.bits[i];
        }
    }

    /**
     * Performs a logical <b>OR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has the
     * value true if and only if it either already had the value true or the corresponding bit in the bit set argument has the
     * value true.
     * @param other a bit set
     */
    public void or(Bits other){
        int commonWords = Math.min(bits.length, other.bits.length);
        for(int i = 0; commonWords > i; i++){
            bits[i] |= other.bits[i];
        }

        if(commonWords < other.bits.length){
            checkCapacity(other.bits.length);
            for(int i = commonWords, s = other.bits.length; s > i; i++){
                bits[i] = other.bits[i];
            }
        }
    }

    /**
     * Performs a logical <b>XOR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has
     * the value true if and only if one of the following statements holds:
     * <ul>
     * <li>The bit initially has the value true, and the corresponding bit in the argument has the value false.</li>
     * <li>The bit initially has the value false, and the corresponding bit in the argument has the value true.</li>
     * </ul>
     */
    public void xor(Bits other){
        int commonWords = Math.min(bits.length, other.bits.length);

        for(int i = 0; commonWords > i; i++){
            bits[i] ^= other.bits[i];
        }

        if(commonWords < other.bits.length){
            checkCapacity(other.bits.length);
            for(int i = commonWords, s = other.bits.length; s > i; i++){
                bits[i] = other.bits[i];
            }
        }
    }

    /**
     * Returns true if the specified BitSet has any bits set to true that are also set to true in this BitSet.
     * @param other a bit set
     * @return boolean indicating whether this bit set intersects the specified bit set
     */
    public boolean intersects(Bits other){
        long[] bits = this.bits;
        long[] otherBits = other.bits;
        for(int i = Math.min(bits.length, otherBits.length) - 1; i >= 0; i--){
            if((bits[i] & otherBits[i]) != 0){
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this bit set is a super set of the specified set, i.e. it has all bits set to true that are also set to true
     * in the specified BitSet.
     * @param other a bit set
     * @return boolean indicating whether this bit set is a super set of the specified set
     */
    public boolean containsAll(Bits other){
        long[] bits = this.bits;
        long[] otherBits = other.bits;
        int otherBitsLength = otherBits.length;
        int bitsLength = bits.length;

        for(int i = bitsLength; i < otherBitsLength; i++){
            if(otherBits[i] != 0){
                return false;
            }
        }
        for(int i = Math.min(bitsLength, otherBitsLength) - 1; i >= 0; i--){
            if((bits[i] & otherBits[i]) != otherBits[i]){
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode(){
        final int word = length() >>> 6;
        int hash = 0;
        for(int i = 0; word >= i; i++){
            hash = 127 * hash + (int)(bits[i] ^ (bits[i] >>> 32));
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj){
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;

        Bits other = (Bits)obj;
        long[] otherBits = other.bits;

        int commonWords = Math.min(bits.length, otherBits.length);
        for(int i = 0; commonWords > i; i++){
            if(bits[i] != otherBits[i])
                return false;
        }

        if(bits.length == otherBits.length)
            return true;

        return length() == other.length();
    }
}
