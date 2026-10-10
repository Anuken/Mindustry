package arc.math;

import arc.struct.Seq;

/**
 * This class represents a cumulative distribution.
 * It can be used in scenarios where there are values with different probabilities
 * and it's required to pick one of those respecting the probability.
 * For example one could represent the frequency of the alphabet letters using a cumulative distribution
 * and use it to randomly pick a letter respecting their probabilities (useful when generating random words).
 * Another example could be point generation on a mesh surface: one could generate a cumulative distribution using
 * triangles areas as interval size, in this way triangles with a large area will be picked more often than triangles with a smaller one.
 * See <a href="http://en.wikipedia.org/wiki/Cumulative_distribution_function">Wikipedia</a> for a detailed explanation.
 * @author Inferno
 */
public class CumulativeDistribution<T>{
    private Seq<CumulativeValue> values;

    public CumulativeDistribution(){
        values = new Seq<>(false, 10, CumulativeValue.class);
    }

    /** Adds a value with a given interval size to the distribution */
    public void add(T value, float intervalSize){
        values.add(new CumulativeValue(value, 0, intervalSize));
    }

    /** Adds a value with interval size equal to zero to the distribution */
    public void add(T value){
        values.add(new CumulativeValue(value, 0, 0));
    }

    /** Generate the cumulative distribution */
    public void generate(){
        float sum = 0;
        for(int i = 0; i < values.size; ++i){
            sum += values.items[i].interval;
            values.items[i].frequency = sum;
        }
    }

    /** Generate the cumulative distribution in [0,1] where each interval will get a frequency between [0,1] */
    public void generateNormalized(){
        float sum = 0;
        for(int i = 0; i < values.size; ++i){
            sum += values.items[i].interval;
        }
        float intervalSum = 0;
        for(int i = 0; i < values.size; ++i){
            intervalSum += values.items[i].interval / sum;
            values.items[i].frequency = intervalSum;
        }
    }

    /** Generate the cumulative distribution in [0,1] where each value will have the same frequency and interval size */
    public void generateUniform(){
        float freq = 1f / values.size;
        for(int i = 0; i < values.size; ++i){
            //reset the interval to the normalized frequency
            values.items[i].interval = freq;
            values.items[i].frequency = (i + 1) * freq;
        }
    }

    /**
     * Finds the value whose interval contains the given probability
     * Binary search algorithm is used to find the value.
     * @return the value whose interval contains the probability
     */
    public T value(float probability){
        CumulativeValue value;
        int imax = values.size - 1, imin = 0, imid;
        while(imin <= imax){
            imid = imin + ((imax - imin) / 2);
            value = values.items[imid];
            if(probability < value.frequency)
                imax = imid - 1;
            else if(probability > value.frequency)
                imin = imid + 1;
            else break;
        }

        return values.items[imin].value;
    }

    /** @return the value whose interval contains a random probability in [0,1] */
    public T value(){
        return value(Mathf.random());
    }

    /** @return the amount of values */
    public int size(){
        return values.size;
    }

    /** @return the interval size for the value at the given position */
    public float getInterval(int index){
        return values.items[index].interval;
    }

    /** @return the value at the given position */
    public T getValue(int index){
        return values.items[index].value;
    }

    /**
     * Set the interval size on the passed in object.
     * The object must be present in the distribution.
     */
    public void setInterval(T obj, float intervalSize){
        for(CumulativeValue value : values)
            if(value.value == obj){
                value.interval = intervalSize;
                return;
            }
    }

    /** Sets the interval size for the value at the given index */
    public void setInterval(int index, float intervalSize){
        values.items[index].interval = intervalSize;
    }

    /** Removes all the values from the distribution */
    public void clear(){
        values.clear();
    }

    public class CumulativeValue{
        public T value;
        public float frequency;
        public float interval;

        public CumulativeValue(T value, float frequency, float interval){
            this.value = value;
            this.frequency = frequency;
            this.interval = interval;
        }
    }
}
