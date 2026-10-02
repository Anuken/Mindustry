// SevenZip/CRC.java

package arc.util.io;

public class CRC{
    public static int[] table = new int[256];

    static{
        for(int i = 0; i < 256; i++){
            int r = i;
            for(int j = 0; j < 8; j++)
                if((r & 1) != 0)
                    r = (r >>> 1) ^ 0xEDB88320;
                else
                    r >>>= 1;
            table[i] = r;
        }
    }

    int _value = -1;

    public void init(){
        _value = -1;
    }

    public void update(byte[] data, int offset, int size){
        for(int i = 0; i < size; i++)
            _value = table[(_value ^ data[offset + i]) & 0xFF] ^ (_value >>> 8);
    }

    public void update(byte[] data){
        int size = data.length;
        for(byte datum : data) _value = table[(_value ^ datum) & 0xFF] ^ (_value >>> 8);
    }

    public void updateByte(int b){
        _value = table[(_value ^ b) & 0xFF] ^ (_value >>> 8);
    }

    public int getDigest(){
        return ~_value;
    }
}

