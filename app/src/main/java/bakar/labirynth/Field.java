package bakar.labirynth;

import android.graphics.Point;

import java.util.Random;
import java.util.Stack;

/**
 * Created by Bakar on 12.03.2018.
 */

enum Direction {Left, Up, Right, Down;
Direction getOpposite(){
    if (this == Up) return Down;
    if (this == Right) return Left;
    if (this == Down) return Up;
    if (this == Left) return Right;
    return Up;
    }
}

class Field {
    private boolean[][] cells;
    private int xSize = 3;
    private int ySize = 3;
    private int persOfDeadEnds = 70;
    public CPoint.Field startPos;
    public CPoint.Field exitPos;

     Field(int xSize_, int ySize_){
         xSize = xSize_;
         ySize = ySize_;
         if (xSize % 2 == 0){
             xSize++;
         }
         if (ySize % 2 == 0){
             ySize++;
         }
         cells = new boolean[xSize][ySize];
     }

     void init(long seed){
        for (int x = 0; x < xSize; ++x){
            for (int y = 0; y < ySize; ++y){
                set(x, y, false);
            }
        }

         Architect architect = new Architect();
         architect.createLabyrinth(seed);
     }

     public CPoint.Field ptAt(CPoint.Field point, Direction direction){
         CPoint.Field result = new CPoint.Field(point);
         switch (direction){
             case Left:
                 result.offset(-1, 0);
                 break;
             case Up:
                 result.offset(0, -1);
                 break;
             case Right:
                 result.offset(1, 0);
                 break;
             case Down:
                 result.offset(0, 1);
                 break;
         }
         return result;
     }
     boolean isNode(CPoint.Field point){
         CPoint.Field pt = new CPoint.Field(point);
         if ((point.x < 0) || (point.x > xSize - 1) || (point.y < 0) || (point.y > ySize - 1))
             return false;

         boolean isWallLeft = !get(ptAt(pt, Direction.Left));
         boolean isWallRight = !get(ptAt(pt, Direction.Right));
         boolean isWallUp = !get(ptAt(pt, Direction.Up));
         boolean isWallDown = !get(ptAt(pt, Direction.Down));

         if (((isWallLeft && isWallRight) && (isWallUp == isWallDown)) ||
                 ((isWallDown && isWallUp) && (isWallLeft == isWallRight)))
             return false;
         else return true;
     }

     boolean get(CPoint.Field point){
         if (point.x < getxSize() && point.x >= 0 &&
                 point.y < getySize() && point.y >= 0)
            return cells[point.x][point.y];
         else
             return false;
     }
     boolean get(int x, int y){
         return cells[x][y];
     }
     void set(CPoint.Field point, boolean value){
         cells[point.x][point.y] = value;
     }
     void set(int x, int y, boolean value){cells[x][y] = value;}
     int getxSize(){return xSize;}
     int getySize(){return ySize;}

     class Architect{
         CPoint.Field cursor;
         Stack<CPoint.Field> path = new Stack<>();

         Architect(){
             cursor = new CPoint.Field(1, 1);
             path.push(cursor);
             set(cursor, true);
         }

         public void createLabyrinth(long seed){
             Random random = new Random(seed);

             while (path.size() == 1)
                 goTo(Direction.values()[random.nextInt(Direction.values().length)]);
             while (path.size() > 1)
                 goTo(Direction.values()[random.nextInt(Direction.values().length)]);
             placeExtraConnectors(seed);

             placeExit(seed);
             placeStartPos(seed);
         }
         private void goTo(Direction direction){
             if (canGo(direction)){
                 set(ptAt(cursor, direction), true);
                 cursor = ptAt(ptAt(cursor, direction), direction);
                 path.push(cursor);
                 set(cursor, true);
             }
             else {
                 if (isNoWayOut()){
                     path.pop();
                     cursor = path.peek();
                 }
             }
         }
         private boolean canGo(Direction direction){
             CPoint.Field point = new CPoint.Field(ptAt(ptAt(cursor, direction), direction));
             if ((point.x < 0) || (point.x > xSize - 1) || (point.y < 0) || (point.y > ySize - 1))
                 return false;
             return !get(point); // negative
         }
         private boolean isNoWayOut(){
             if (!canGo(Direction.Left) &&
                 !canGo(Direction.Up) &&
                 !canGo(Direction.Right) &&
                 !canGo(Direction.Down))
                 return true;
             else
                 return false;
         }

         private void placeExtraConnectors(long seed){
             Random random = new Random(seed);

             for (int x = 1; x < getxSize() - 1; ++x){
                 for (int y = 1; y < getySize() - 1; ++y){
                     cursor.set(x,y);
                     if (isCursorAtDeadEnd()){
                         Direction dir = Direction.values()[random.nextInt(Direction.values().length)];
                         while ((get(ptAt(cursor, dir))) ||
                                 (ptAt(cursor, dir).x == 0) || (ptAt(cursor, dir).x == getxSize() - 1) ||
                                 (ptAt(cursor, dir).y == 0) || (ptAt(cursor, dir).y == getySize() - 1)){
                             dir = Direction.values()[random.nextInt(Direction.values().length)];
                         }

                         if (random.nextInt(100) > persOfDeadEnds)
                            set(ptAt(cursor, dir), true);
                     }
                 }
             }
         }
         private boolean isCursorAtDeadEnd(){
             int outsCount = 0;
             for (int i = 0; i < Direction.values().length; ++i){
                 if (get(ptAt(cursor, Direction.values()[i])))
                     outsCount++;
             }
             if (1 == outsCount)
                 return true;
             else
                 return false;
         }

         private void placeStartPos(long seed){
             Random random = new Random(seed);
             float reqDist = (float)Math.sqrt(getxSize() * getxSize() + getySize() * getySize()) / 2;

             int x = exitPos.x, y = exitPos.y;
             while (Math.sqrt((x - exitPos.x)*(x - exitPos.x) + (y - exitPos.y)*(y - exitPos.y)) < reqDist){
                 x = random.nextInt(getxSize() - 1);
                 y = random.nextInt(getySize() - 1);

                 if (!get(x, y)){
                     x = exitPos.x;
                     y = exitPos.y;
                 }
             }

             startPos = new CPoint.Field(x, y);

         }
         private void placeExit(long seed){
             Random random = new Random(seed);
             double rad = Math.sqrt(getxSize()*getxSize() + getySize()*getySize()) / 2;

             CPoint.Field exit = new CPoint.Field(getxSize() / 2, getySize() / 2);
             while (GameLogic.distance(exit, new Point(getxSize() / 2, getySize() / 2)) < rad / 2){
                 exit.set(random.nextInt(getxSize() - 1), random.nextInt(getySize() - 1));

                 if (!get(exit.x, exit.y)){
                     exit.set(getxSize() / 2, getySize() / 2);
                 }
             }

             exitPos = exit;
         }
    }
}
