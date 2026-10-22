package arc.audio;

import static arc.audio.Soloud.*;

public class Filters{
    public static final int paramWet = 0;

    public static class BiquadFilter extends AudioFilter{

        public BiquadFilter(){
            super(filterBiquad());
        }

        public void set(int type, float frequency, float resonance){
            biquadSet(handle, type, frequency, resonance);
        }
    }

    public static class EchoFilter extends AudioFilter{

        public EchoFilter(){
            super(filterEcho());
        }

        public void set(float delay, float decay, float filter){
            echoSet(handle, delay, decay, filter);
        }
    }

    public static class LofiFilter extends AudioFilter{

        public LofiFilter(){
            super(filterLofi());
        }

        public void set(float sampleRate, float depth){
            lofiSet(handle, sampleRate, depth);
        }
    }

    public static class FlangerFilter extends AudioFilter{

        public FlangerFilter(){
            super(filterFlanger());
        }

        public void set(float delay, float frequency){
            flangerSet(handle, delay, frequency);
        }
    }

    public static class WaveShaperFilter extends AudioFilter{

        public WaveShaperFilter(){
            super(filterWaveShaper());
        }

        public void set(float amount){
            waveShaperSet(handle, amount);
        }
    }

    public static class BassBoostFilter extends AudioFilter{

        public BassBoostFilter(){
            super(filterBassBoost());
        }

        public void set(float amount){
            bassBoostSet(handle, amount);
        }
    }

    public static class RobotizeFilter extends AudioFilter{

        public RobotizeFilter(){
            super(filterRobotize());
        }

        public void set(float freq, int waveform){
            robotizeSet(handle, freq, waveform);
        }
    }

    public static class FreeverbFilter extends AudioFilter{

        public FreeverbFilter(){
            super(filterFreeverb());
        }

        public void set(float mode, float roomSize, float damp, float width){
            freeverbSet(handle, mode, roomSize, damp, width);
        }
    }
}
