package mindustry.world.meta;

import arc.func.*;

import java.util.*;

/** An immutable set of {@link Env}s. May also be the special "any" set, which contains every Env. */
public final class Environments{
    public static final Environments
    none = new Environments(new long[0], false),
    any = new Environments(new long[0], true);

    private final long[] words;
    private final boolean anyFlag;

    private Environments(long[] words, boolean anyFlag){
        int len = words.length;
        while(len > 0 && words[len - 1] == 0) len--;

        this.words = len == words.length ? words : Arrays.copyOf(words, len);
        this.anyFlag = anyFlag;
    }

    public static Environments of(Env... envs){
        if(envs.length == 0) return none;

        long[] words = new long[(Env.all.size + 63) >>> 6];
        for(Env env : envs){
            words[env.id >>> 6] |= 1L << (env.id & 63);
        }
        return new Environments(words, false);
    }

    /** Union of all the specified sets. */
    public static Environments of(Environments... sets){
        Environments result = none;
        for(Environments set : sets){
            result = result.with(set);
        }
        return result;
    }

    /** Converts a legacy int bitmask; bit i maps to the Env with id i. All bits set means {@link #any}. */
    public static Environments fromBits(int legacy){
        if(legacy == -1) return any;

        long[] words = new long[1];
        for(int i = 0; i < 32 && i < Env.all.size; i++){
            if((legacy & (1 << i)) != 0) words[0] |= 1L << i;
        }
        return new Environments(words, false);
    }

    /** @return a new Environment with the specified flag added. */
    public Environments with(Env env){
        if(anyFlag || has(env)) return this;

        int word = env.id >>> 6;
        long[] out = Arrays.copyOf(words, Math.max(words.length, word + 1));
        out[word] |= 1L << (env.id & 63);
        return new Environments(out, false);
    }

    /** @return a new Environment with the specified flags added. */
    public Environments with(Environments other){
        if(anyFlag || other.isEmpty() || other == this) return this;
        if(other.anyFlag) return other;

        long[] out = Arrays.copyOf(words, Math.max(words.length, other.words.length));
        for(int i = 0; i < other.words.length; i++){
            out[i] |= other.words[i];
        }
        Environments result = new Environments(out, false);
        return result.equals(this) ? this : result;
    }

    /** @return a new Environment without the specified flag. */
    public Environments without(Env env){
        if(anyFlag) throw new UnsupportedOperationException("Cannot remove an Env from 'any'.");
        if(!has(env)) return this;

        long[] out = words.clone();
        out[env.id >>> 6] &= ~(1L << (env.id & 63));
        return new Environments(out, false);
    }

    /** @return a new Environment without the specified flags. */
    public Environments without(Environments other){
        if(other.isEmpty()) return this;
        if(anyFlag) throw new UnsupportedOperationException("Cannot remove Envs from 'any'.");
        if(other.anyFlag) return none;
        if(!containsAny(other)) return this;

        long[] out = words.clone();
        for(int i = 0; i < Math.min(out.length, other.words.length); i++){
            out[i] &= ~other.words[i];
        }
        return new Environments(out, false);
    }

    public boolean has(Env env){
        if(anyFlag) return true;

        int word = env.id >>> 6;
        return word < words.length && (words[word] & (1L << (env.id & 63))) != 0;
    }

    public boolean isEmpty(){
        return !anyFlag && words.length == 0;
    }

    public boolean isAny(){
        return anyFlag;
    }

    /** @return whether this set and the other share at least one Env. */
    public boolean containsAny(Environments other){
        if(anyFlag) return !other.isEmpty();
        if(other.anyFlag) return !isEmpty();

        if(words.length == 1 && other.words.length == 1){
            return (words[0] & other.words[0]) != 0;
        }

        int len = Math.min(words.length, other.words.length);
        for(int i = 0; i < len; i++){
            if((words[i] & other.words[i]) != 0) return true;
        }
        return false;
    }

    /** @return whether this set contains every Env in the other. */
    public boolean containsAll(Environments other){
        if(anyFlag) return true;
        if(other.anyFlag) return false;

        if(words.length == 1 && other.words.length == 1){
            return (words[0] & other.words[0]) == other.words[0];
        }

        //trailing zero words are trimmed, so a longer other always has a bit this set lacks
        if(other.words.length > words.length) return false;
        for(int i = 0; i < other.words.length; i++){
            if((words[i] & other.words[i]) != other.words[i]) return false;
        }
        return true;
    }

    /** Iterates the contained Envs in ascending id order. Does nothing for {@link #any}. */
    public void each(Cons<Env> cons){
        if(anyFlag) return;

        for(int i = 0; i < words.length; i++){
            long word = words[i];
            while(word != 0){
                int bit = Long.numberOfTrailingZeros(word);
                word &= word - 1;
                cons.get(Env.all.get((i << 6) + bit));
            }
        }
    }

    @Override
    public boolean equals(Object o){
        return o == this || (o instanceof Environments e && e.anyFlag == anyFlag && (anyFlag || Arrays.equals(words, e.words)));
    }

    @Override
    public int hashCode(){
        return anyFlag ? Integer.MAX_VALUE : Arrays.hashCode(words);
    }

    @Override
    public String toString(){
        if(anyFlag) return "any";

        StringBuilder out = new StringBuilder("[");
        each(e -> {
            if(out.length() > 1) out.append(", ");
            out.append(e.name);
        });
        return out.append("]").toString();
    }
}
