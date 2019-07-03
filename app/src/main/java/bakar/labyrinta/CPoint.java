package bakar.labyrinta;

import android.graphics.Point;
import android.graphics.PointF;

public final class CPoint {

    static final class Screen extends PointF {
        Screen(){
            super();
        }
        Screen(float x, float y){
            super(x,y);
        }
        Screen(PointF pt){
            super(pt.x, pt.y);
        }
    }
    static final class Game extends PointF {
        Game(){
            super();
        }
        Game(float x, float y){
            super(x,y);
        }
    }
    static final class Field extends Point {
        Field(){
            super();
        }
        Field(int x, int y){
            super(x, y);
        }
        Field(Field pt) {
            super(pt.x, pt.y);
        }
    }
}
