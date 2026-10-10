package arc.math;


import arc.math.geom.*;
import arc.struct.*;

public class LinearRegression{
    public float intercept, slope;

    /** Performs a linear regression on the data points. */
    public void calculate(Seq<Vec2> v){
        int n = v.size;

        float sumx = 0f, sumy = 0f;
        for(int i = 0; i < n; i++){
            sumx += v.get(i).x;
            sumy += v.get(i).y;
        }
        float xbar = sumx / n;
        float ybar = sumy / n;

        float xxbar = 0f, xybar = 0f;
        for(int i = 0; i < n; i++){
            xxbar += (v.get(i).x - xbar) * (v.get(i).x - xbar);
            xybar += (v.get(i).x - xbar) * (v.get(i).y - ybar);
        }

        slope = xybar / xxbar;
        intercept = ybar - slope * xbar;
    }

    /** @return the expected response {@code y} given the value of the predictor variable {@code x}. */
    public float predict(float x){
        return slope * x + intercept;
    }
}
