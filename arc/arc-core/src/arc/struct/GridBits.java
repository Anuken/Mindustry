package arc.struct;

public class GridBits{
    private final Bits bits;
    private final int width, height;

    public GridBits(int width, int height){
        this.width = width;
        this.height = height;
        bits = new Bits(width * height);
    }

    public void set(GridBits other){
        bits.set(other.bits);
    }

    public boolean get(int x, int y){
        if(x >= width || y >= height || x < 0 || y < 0) return false;
        return bits.get(x + y*width);
    }

    public void set(int x, int y){
        bits.set(x + y * width);
    }

    public void set(int x, int y, boolean b){
        if(b){
            bits.set(x + y * width);
        }else{
            bits.clear(x + y * width);
        }
    }

    public void clear(){
        bits.clear();
    }

    public int width(){
        return width;
    }

    public int height(){
        return height;
    }
}
