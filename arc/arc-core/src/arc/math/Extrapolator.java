package arc.math;

import java.util.*;

public class Extrapolator{
    private float[] snapPos, snapVel, aimPos, lastPacketPos;
    private float[] tmpArr, tmpArr2;
    private double snapTime, aimTime, lastPacketTime, latency, updateTime;
    private int size;

    public Extrapolator(int size){
        this.size = size;
        snapPos = new float[size];
        snapVel = new float[size];
        aimPos = new float[size];
        lastPacketPos = new float[size];
        tmpArr = new float[size];
        tmpArr2 = new float[size];
    }

    public boolean addSample(double packetTime, double curTime, float[] pos){
        // The best guess I can make for velocity is the difference between
        // this sample and the last registered sample.
        float[] vel = tmpArr2;
        if(Math.abs(packetTime - lastPacketTime) > 1e-4){
            double dt = 1.0 / (packetTime - lastPacketTime);
            for(int i = 0; i < size; ++i){
                vel[i] = (float)((pos[i] - lastPacketPos[i]) * dt);
            }
        }else{
            clear(vel);
        }

        return addSample(packetTime, curTime, pos, vel);
    }

    public boolean addSample(double packetTime, double curTime, float[] pos, float[] vel){
        if(!estimates(packetTime, curTime)){
            return false;
        }

        copyArray(lastPacketPos, pos);
        lastPacketTime = packetTime;
        readPosition(curTime, snapPos);
        aimTime = curTime + updateTime;
        double dt = aimTime - packetTime;
        snapTime = curTime;
        for(int i = 0; i < size; ++i){
            aimPos[i] = (float)(pos[i] + vel[i] * dt);
        }

        // I now have two positions and two times:
        //   1. aimPos / aimTime
        //   2. snapPos / snapTime
        // I must generate the interpolation velocity based on these two samples.
        // However, if aimTime is the same as snapTime, I'm in trouble.
        // In that case, use the supplied velocity.
        if(Math.abs(aimTime - snapTime) < 1e-4){
            copyArray(snapVel, vel);
        }else{
            dt = 1.0 / (aimTime - snapTime);
            for(int i = 0; i < size; ++i){
                snapVel[i] = (float)((aimPos[i] - snapPos[i]) * dt);
            }
        }

        return true;
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     */
    public boolean addSample(double packetTime, double curTime, float pos){
        checkArraySizeOne();

        tmpArr[0] = pos;
        return addSample(packetTime, curTime, tmpArr);
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     */
    public boolean addSample(double packetTime, double curTime, float pos, float vel){
        checkArraySizeOne();

        tmpArr[0] = pos;
        tmpArr2[0] = vel;
        return addSample(packetTime, curTime, tmpArr, tmpArr2);
    }

    public void reset(double packetTime, double curTime, float[] pos){
        reset(packetTime, curTime, pos, clear(tmpArr));
    }

    public void reset(double packetTime, double curTime, float[] pos, float[] vel){
        lastPacketTime = packetTime;
        copyArray(lastPacketPos, pos);
        snapTime = curTime;
        copyArray(snapPos, pos);
        updateTime = curTime - packetTime;
        latency = updateTime;
        aimTime = curTime + updateTime;
        copyArray(snapVel, vel);

        for(int i = 0; i < size; ++i){
            aimPos[i] = (float)(snapPos[i] + snapVel[i] * updateTime);
        }
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     */
    public void reset(double packetTime, double curTime, float pos){
        checkArraySizeOne();

        reset(packetTime, curTime, pos, 0);
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     */
    public void reset(double packetTime, double curTime, float pos, float vel){
        checkArraySizeOne();

        lastPacketTime = packetTime;
        lastPacketPos[0] = pos;
        snapTime = curTime;
        snapPos[0] = pos;
        updateTime = curTime - packetTime;
        latency = updateTime;
        aimTime = curTime + updateTime;
        snapVel[0] = vel;
        aimPos[0] = (float)(snapPos[0] + snapVel[0] * updateTime);
    }

    public boolean readPosition(double forTime, float[] outPos){
        return readPosition(forTime, outPos, null);
    }

    public boolean readPosition(double forTime, float[] outPos, float[] outVel){
        boolean isOk = true;

        // asking for something before allowable time?
        if(forTime < snapTime){
            forTime = snapTime;
            isOk = false;
        }

        // asking for something very far in the future?
        double maxRange = aimTime + updateTime;
        if(forTime > maxRange){
            forTime = maxRange;
            isOk = false;
        }

        // calculate the interpolated position
        for(int i = 0; i < size; ++i){
            if(outVel != null){
                outVel[i] = snapVel[i];
            }

            outPos[i] = (float)(snapPos[i] + snapVel[i] * (forTime - snapTime));
        }

        if(!isOk && outVel != null){
            clear(outVel);
        }

        return isOk;
    }

    /**
     * Version for extrapolator of {@code size = 1}.
     */
    public float readPosition(double forTime){
        checkArraySizeOne();

        if(readPosition(forTime, tmpArr)){
            return tmpArr[0];
        }

        return 0;
    }

    public double estimateLatency(){
        return latency;
    }

    public double estimateUpdateTime(){
        return updateTime;
    }

    private boolean estimates(double packet, double cur){
        if(packet <= lastPacketTime){
            return false;
        }

        // The theory is that, if latency increases, quickly
        // compensate for it, but if latency decreases, be a
        // little more resilient; this is intended to compensate
        // for jittery delivery.
        double lat = cur - packet;
        if(lat < 0){
            lat = 0;
        }
        if(lat > latency){
            latency = (latency + lat) * 0.5;
        }else{
            latency = (latency * 7 + lat) * 0.125;
        }

        // Do the same running average for update time.
        // Again, the theory is that a lossy connection wants
        // an average of a higher update time.
        double tick = packet - lastPacketTime;
        if(tick > updateTime){
            updateTime = (updateTime + tick) * 0.5;
        }else{
            updateTime = (updateTime * 7 + tick) * 0.125;
        }

        return true;
    }

    private float[] clear(float[] arr){
        Arrays.fill(arr, 0);
        return arr;
    }

    private void copyArray(float[] dest, float[] src){
        for(int i = 0, n = src.length; i < n; ++i){
            dest[i] = src[i];
        }
    }

    private void checkArraySizeOne(){
        if(size != 1){
            throw new UnsupportedOperationException("This function should be called only when size = 1!");
        }
    }
}