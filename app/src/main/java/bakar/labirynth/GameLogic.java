package bakar.labirynth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;


/**
 * Created by Bakar on 10.03.2018.
 */

class GameLogic {
    boolean isInited = false;
    boolean usesJoystick;

    boolean pointerActive = false;
    boolean pathfinderActive = false;
    boolean teleportActive = false;

    int pointerAmount = 0;
    int teleportAmount = 0;
    int pathfinderAmount = 0;

    private GameRenderer gameRenderer;
    Field field;
    Node currentNode;
    private CPoint.Game playerPt;
    long seed = 0;
    Joystick joystick;
    LinkedList<CPoint.Game> finded_path;
    EntityFactory eFactory = new EntityFactory();
    int earnedGold = 0;

    CPoint.Game debugTouchGameCoord = new CPoint.Game(0, 0);

    Queue<CPoint.Field> traces = new LinkedList<>();// fieldCoords
    int tracesSize = 10;

    GameLogic(GameRenderer gameRenderer_, long _seed, int _xsize, int _ysize){
        gameRenderer = gameRenderer_;
        init(_seed, _xsize, _ysize);
    }

    void init(int xsize, int ysize){
        field = new Field(xsize, ysize);
        field.init(seed);

        playerPt = gameRenderer.field2game(field.startPos); // gameCoords

        currentNode = new Node(gameRenderer.game2field(playerPt));
        currentNode.updateLinks();

        traces.clear();
        traces.add(gameRenderer.game2field(playerPt));

        eFactory.reset();
        eFactory.makeExit(field.exitPos);
        //eFactory.makePointer(new Point(1, 1));
        //eFactory.makeTeleport(new Point(3, 3));
        //eFactory.makePathfinder(new Point(5, 5));
        eFactory.dropCoins();
        eFactory.dropBonuses();

        isInited = true;
    }
    void init(long _seed, int xsize, int ysize){
        seed = _seed;
        init(xsize, ysize);
    }
    void createJoystick(CPoint.Screen mainPos, float mainRadius){
        joystick = new Joystick(mainPos, mainRadius);
    }
    LinkedList<CPoint.Game> getPath(CPoint.Game from, CPoint.Game to){
        AStarAlg alg = new AStarAlg(from, to);
        finded_path = alg.AStar();
        return finded_path;
    }

    void startEndActivity(){
        Intent intent = new Intent(gameRenderer.getContext(), EndActivity.class);
        intent.putExtra("earned_gold", earnedGold);
        ((Activity)gameRenderer.getContext()).startActivityForResult(intent, EndActivity.class.toString().hashCode());
    }

    void activatePointer(){
        pointerActive = true;
        pointerAmount--;
    }
    void activatePathfinder(CPoint.Game gameCoord){
        getPath(playerCoords(), getClosestFloorCoord(gameCoord));
        pathfinderAmount--;
    }
    void activateTeleport(CPoint.Game gameCoord){
        playerPt = getClosestFloorCoord(gameCoord);
        currentNode = new Node(gameRenderer.game2field(playerCoords()));
        currentNode.updateLinks();
        gameRenderer.lightFog(playerCoords());
        eFactory.intersectsWith(gameRenderer.game2field(playerCoords()));
        teleportAmount--;
    }
    float getTeleportRadius(){
        return gameRenderer.cellSize * 5; // game
    }
    float getPathfinderRadius(){
        return gameRenderer.cellSize * 18; // game
    }
    CPoint.Game getClosestFloorCoord(CPoint.Game gm){
        CPoint.Field fi = gameRenderer.game2field(gm);
        if (field.get(fi))
            return gameRenderer.field2game(gameRenderer.game2field(gm)); // получаем центр клетки

        for (Direction dir : Direction.values()
             ) {
            if (field.get(field.ptAt(fi, dir)))
                return gameRenderer.field2game(field.ptAt(fi, dir));
        }
        return gameRenderer.field2game(new CPoint.Field(1, 1));
    }

    void onExitReached(){
        earnedGold += field.getxSize() + field.getySize();
        gameRenderer.onTouchUp(null);
        seed = System.currentTimeMillis();
        finded_path = null;
        pointerActive = false;

        startEndActivity();
    }
    void onCoinPickedUp(){
        earnedGold += 50;
        gameRenderer.addPickUpAnimation(playerCoords(), "+50");
    }
    void onPointerPickedUp(){
        pointerAmount++;
        gameRenderer.addPickUpAnimation(playerCoords(), "+1");
    }
    void onTeleportPickedUp(){
        teleportAmount++;
        gameRenderer.addPickUpAnimation(playerCoords(), "+1");
    }
    void onPathfinderPickedUp(){
        pathfinderAmount++;
        gameRenderer.addPickUpAnimation(playerCoords(), "+1");
    }
    void onCellChanged(){
        gameRenderer.lightFog(playerCoords());
    }

    void updateTraces(CPoint.Game pointF){

        if ((((LinkedList<CPoint.Field>)traces).getLast().x != gameRenderer.game2field(pointF).x) ||
                (((LinkedList<CPoint.Field>)traces).getLast().y != gameRenderer.game2field(pointF).y)){
            if (traces.size() >= tracesSize)
                traces.poll();
            traces.add(gameRenderer.game2field(pointF));

            onCellChanged();
        }
    }
    boolean canMovePlayer(CPoint.Screen pointF){ // screencoord
        if (usesJoystick)
            return joystick.isTouched(pointF);
        else
            return distance(pointF, gameRenderer.game2screen(playerCoords())) < gameRenderer.playerHitbox;
    }
    void remoteMove(){
        if (usesJoystick && gameRenderer.isMovingPlayer)
            movePlayerTo(joystick.lastTouch); //TODO тут был косяк с типом | done
    }
    void movePlayerTo(CPoint.Game pointF){ // gameCoord rly
        if (!gameRenderer.isPlayerInSight())
            gameRenderer.buttons.get(0).onClick();

        CPoint.Game input;
        if (usesJoystick){
            CPoint.Game offsetp = joystick.getPlayerOffsetOnMove(pointF);
            input = playerCoords();
            if (0 != offsetp.x && 0 != offsetp.y)
                input.offset(offsetp.x, offsetp.y);
        }
        else
            input = pointF;

        debugTouchGameCoord = input;

        Line nearestRail = new Line();
        float distanceToRail = (float)1e6;
        PointF newPlayerPt = new PointF();//nearestRail.projection(pointF);

        for (int i = 0; i < currentNode.availableDirections.size(); ++i){
            nearestRail.set(gameRenderer.field2game(currentNode.pos),
                    gameRenderer.field2game(currentNode.links.get(currentNode.availableDirections.get(i)).pos));

            if (distance(nearestRail.projection(input), input) < distanceToRail){

                newPlayerPt = nearestRail.projection(input);
                distanceToRail = distance(nearestRail.projection(input), input);
            }
        }

        if (distanceToRail > gameRenderer.getCellSize() * 3)
            gameRenderer.isMovingPlayer = false;
        else{
            playerPt.x = newPlayerPt.x;
            playerPt.y = newPlayerPt.y;
            updateTraces(playerPt);
        }

        for (int i = 0; i < currentNode.availableDirections.size(); ++i){
            if (distance(newPlayerPt,
                    gameRenderer.field2game(currentNode.links.get(currentNode.availableDirections.get(i)).pos)) <
                    gameRenderer.getCellSize() * (3/2)){
                currentNode = currentNode.links.get(currentNode.availableDirections.get(i));
                currentNode.updateLinks();
            }
        }
        eFactory.intersectsWith(gameRenderer.game2field(playerCoords()));
    }
    CPoint.Game playerCoords(){ // gameCoord
        return new CPoint.Game(playerPt.x, playerPt.y);
    }
    CPoint.Game exitCoords(){
        return new CPoint.Game(gameRenderer.field2game(field.exitPos).x, gameRenderer.field2game(field.exitPos).y);
    }
    static float distance(PointF point1, PointF point2){
        PointF vec = new PointF(point2.x - point1.x, point2.y - point1.y);
        return (float)Math.sqrt(vec.x * vec.x + vec.y * vec.y);
    }
    static float distance(Point point1, Point point2){
        Point vec = new Point(point2.x - point1.x, point2.y - point1.y);
        return (float)Math.sqrt(vec.x * vec.x + vec.y * vec.y);
    }

    class Node {
        CPoint.Field pos;
        ArrayMap<Direction, Node> links = new ArrayMap<>();
        ArrayList<Direction> availableDirections = new ArrayList<>();

        Node(){
            pos = new CPoint.Field(0, 0);
        }
        Node (CPoint.Field point){
            pos = new CPoint.Field(point);
        }
        boolean equals(Node node1, Node node2){
            return node1.pos.x == node2.pos.x && node1.pos.y == node2.pos.y;
        }
        void updateLinks(){
            for (int i = 0; i < Direction.values().length; ++i){
                if (!field.get(field.ptAt(pos, Direction.values()[i])))
                    continue;

                if (links.containsKey(Direction.values()[i]))
                    continue;

                availableDirections.add(Direction.values()[i]);

                CPoint.Field node_pos = new CPoint.Field(field.ptAt(pos, Direction.values()[i]));
                while (!field.isNode(node_pos)){
                    node_pos = field.ptAt(node_pos, Direction.values()[i]);
                }

                links.put(Direction.values()[i], new Node(node_pos));
                links.get(Direction.values()[i]).availableDirections.add(Direction.values()[i].getOpposite());
                links.get(Direction.values()[i]).links.put(Direction.values()[i].getOpposite(), this);
            }
        }
        LinkedList<Node> getLinkedNodes(){
            updateLinks();
            LinkedList<Node> ret = new LinkedList<>();
            for (Direction dir : availableDirections){
                ret.add(links.get(dir));
                links.get(dir).availableDirections.add(dir.getOpposite());
                links.get(dir).links.put(dir.getOpposite(), this);
            }
            return ret;
        }
    }
    class Line { // vector
        PointF one;
        PointF two;

        Line(){
            one = new PointF(0, 0);
            two = new PointF(0, 0);
        }
        Line(PointF one_, PointF two_){
            one = one_;
            two = two_;
        }
        void set(PointF one_, PointF two_){
            one = one_;
            two = two_;
        }

        PointF vector(){
            return new PointF(two.x - one.x, two.y - one.y);
        }
        float cosOfAngleWith(PointF vec){
            PointF thisVec = vector();
            float scalarMult = thisVec.x * vec.x + thisVec.y * vec.y;

            return scalarMult / (module(thisVec) * module(vec));
        }
        float module(){
            return module(vector());
        }
        float module(PointF vec){
            return (float)Math.sqrt(vec.x * vec.x + vec.y * vec.y);
        }
        PointF normalizedVector(){
            PointF result = vector();
            if (result.x == 0 && result.y == 0) //Todo: сделать нормально
                return new PointF(0, 0);
            result.set(result.x / module(), result.y / module());
            return result;
        }

        PointF projection(PointF point){
            PointF result;
            Line otherLine = new Line(one, point);
            float cos = cosOfAngleWith(otherLine.vector());

            result = new PointF(one.x + vector().x * (otherLine.module() / module()) * cos,
                                one.y + vector().y * (otherLine.module() / module()) * cos);

            if (module() < distance(result, one) || module() < distance(result, two)){
                if (distance(result, one) < distance(result, two))
                    return one;
                else
                    return two;
            }
            else
                return result;
        }
    }
    class Joystick {
        CPoint.Screen mainPos;
        CPoint.Screen curPos;
        CPoint.Game lastTouch = new CPoint.Game(0, 0);
        float mainRadius;
        float stickRadius; // только для отрисовки
        float speed = (gameRenderer.cellSize * 20 / 70);

        boolean isInUse = false;

        Joystick(CPoint.Screen _mainPos, float _mainRadius){
            mainPos = _mainPos;
            curPos = new CPoint.Screen(_mainPos.x, _mainPos.y);
            mainRadius = _mainRadius;
            stickRadius = mainRadius / 2;
            touchReleased();
        }

        boolean isTouched(CPoint.Screen pointF){ // screencoord
            if (distance(pointF, mainPos) < mainRadius){
                isInUse = true;
                curPos = pointF;
                return true;
            }
            else
                return false;
        }

        CPoint.Game getPlayerOffsetOnMove(CPoint.Game pointF){ // gameCoord на инпуте // аккуратно с конвертацией
//            if (distance(gameRenderer.game2screen(pointF), mainPos) < mainRadius){
//                curPos = gameRenderer.game2screen(pointF);
//            }
//            else{
//                Line line = new Line(gameRenderer.game2screen(pointF), mainPos);
//                curPos.set(mainPos.x - line.normalizedVector().x * mainRadius,
//                        mainPos.y - line.normalizedVector().y * mainRadius);
//            }
            if (distance(gameRenderer.game2screen(pointF), mainPos) < mainRadius){
                curPos = gameRenderer.game2screen(pointF);
            }
            else{
                Line line = new Line(gameRenderer.game2screen(pointF), mainPos);
                curPos.set(mainPos.x - line.normalizedVector().x * mainRadius,
                        mainPos.y - line.normalizedVector().y * mainRadius);
            }

            float speedScale = distance(curPos, mainPos) / mainRadius;
            Line line = new Line(mainPos, curPos);

            PointF result = line.normalizedVector(); // ((-1) - 1; (-1) - 1);
            result.set(result.x * speed * speedScale, result.y * speed * speedScale);
            CPoint.Game result2 = new CPoint.Game();
            result2.x = result.x;
            result2.y = result.y;
            return result2;
        }

        void touchReleased(){
            isInUse = false;
            curPos.set(mainPos.x, mainPos.y);
            //lastTouch = playerCoords();
            lastTouch = gameRenderer.screen2game(mainPos);
        }
    }
    class AStarAlg{
        //Todo: сделать отдельным потоком
        Node starNode;
        Node crossNode;
        CPoint.Field star;
        CPoint.Field cross;
        LinkedList<AStarNode> open = new LinkedList<>();
        LinkedList<AStarNode> closed = new LinkedList<>();

        AStarAlg(CPoint.Game star_, CPoint.Game cross_){
            starNode = new Node(gameRenderer.game2field(star_));
            if (field.isNode(gameRenderer.game2field(cross_))){
                crossNode = new Node(gameRenderer.game2field(cross_));
            }
            else {
                crossNode = getClosestToPoint(new Node(gameRenderer.game2field(cross_)).getLinkedNodes(), gameRenderer.game2field(cross_));
            }
            cross = gameRenderer.game2field(cross_);
            star = gameRenderer.game2field(star_);
        }

        Node getClosestToPoint(LinkedList<Node> nodes, CPoint.Field pt){
            Node result = new Node(new CPoint.Field(0, 0));
            double min_dist = 10e3;
            for (Node node: nodes
                 ) {
                if (distance(gameRenderer.field2game(node.pos), gameRenderer.field2game(pt)) < min_dist){
                    min_dist = distance(gameRenderer.field2game(node.pos), gameRenderer.field2game(pt));
                    result = node;
                }
            }
            return new Node(result.pos);
        }

        boolean contains(LinkedList<Node> list, Node node){
            for (Node nd : list
                 ) {
                if (nd.pos.x == node.pos.x && nd.pos.y == node.pos.y)
                    return true;
            }
            return false;
        }
        boolean contains(LinkedList<AStarNode> list, AStarNode node){
            for (Node nd : list
                    ) {
                if (nd.pos.x == node.pos.x && nd.pos.y == node.pos.y)
                    return true;
            }
            return false;
        }
        AStarNode minFG(LinkedList<AStarNode> list){
            double minfg = 10e3;
            AStarNode result = null;
            for (AStarNode aStarNode : list
                 ) {
                double fg = aStarNode.getF() + aStarNode.getG();
                if (minfg > fg){
                    minfg = fg;
                    result = aStarNode;
                }
            }

            return result;
        }

        private LinkedList<CPoint.Game> AStar(){

            open.push(new AStarNode(starNode, null));

            AStarNode curr = open.getFirst();
            while (curr.heuristic() != 0){
                curr = minFG(open);
                open.remove(curr);
                closed.push(curr);
                open.addAll(curr.neighbours());
            }

            LinkedList<AStarNode> path = new LinkedList<>();
            path.push(curr);
            while (path.getFirst().cameFrom != null){
                path.push(path.getFirst().cameFrom);
            }

            LinkedList<CPoint.Game> result = new LinkedList<>();
            for (AStarNode aStarNode : path
                 ) {
                result.add(result.size(), gameRenderer.field2game(aStarNode.pos));
            }
            result.add(result.size(), gameRenderer.field2game(cross));

            if (contains(new Node(cross).getLinkedNodes(), new Node(gameRenderer.game2field(result.get(result.size() - 3)))))
                result.remove(result.size() - 2);

            return result;
        }

        class AStarNode extends Node{
            AStarNode cameFrom;

            AStarNode(Node node, AStarNode cameFrom_){
                pos = node.pos;
                cameFrom = cameFrom_;
            }
            int distanceTo(AStarNode aStarNode){
                return Math.abs(aStarNode.pos.x - pos.x) + Math.abs(aStarNode.pos.y - pos.y);
            }
            float heuristic(){
                return distance(pos, crossNode.pos);
            }
            LinkedList<AStarNode> neighbours(){
                LinkedList<Node> linked = getLinkedNodes();
                LinkedList<AStarNode> ret = new LinkedList<>();

                for (Node node: linked
                     ) {
                    ret.push(new AStarNode(node, this));
                }

                for (int i = 0; i < ret.size(); i++) {
                    if (contains(closed, ret.get(i))){
                        ret.remove(i);
                        i = 0;
                    }
                }

                for (int i = 0; i < ret.size(); i++) {
                    if (contains(open, ret.get(i))){
                        ret.remove(i);
                        i = 0;
                    }
                }

                return ret;
            }
            int getG(){
                if (cameFrom != null){
                    return cameFrom.getG() + cameFrom.distanceTo(this);
                }
                else
                    return 0;
            }
            float getF(){
                return getG() + heuristic();
            }

        }
    }

    class EntityFactory{
        // TODO check Glide
        LinkedList<Entity> entities = new LinkedList<>();
        ArrayList<Bitmap> exitTextures = new ArrayList<>();
        ArrayList<Bitmap> coinTextures = new ArrayList<>();
        ArrayList<Bitmap> teleportTextures = new ArrayList<>();
        ArrayList<Bitmap> pointerTextures = new ArrayList<>();
        ArrayList<Bitmap> pathfinderTextures = new ArrayList<>();
        void adjustBitmaps(ArrayList<Bitmap> list, boolean isLarge){

            for (Bitmap bitmap : list
                 ) {
                int localCellSize;
                if (isLarge)
                    localCellSize = Math.round(gameRenderer.cellSize) * 2;
                else
                    localCellSize = Math.round(gameRenderer.cellSize);

                Matrix matrix = new Matrix();
                matrix.postScale((localCellSize / bitmap.getHeight()),
                        (localCellSize / bitmap.getHeight()));
                bitmap = Bitmap.createScaledBitmap(bitmap, localCellSize, localCellSize, true);

//                CPoint.Game offset_g = gameRenderer.field2game(entity.pos);
//                offset_g.offset(-localCellSize / 2, -localCellSize / 2);
//                matrix.postTranslate(offset_g.x, offset_g.y);
            }

        }

        float pointerProbability = 80.f;
        float pathfinderProbability = 80.f;
        float teleportProbability = 80.f;

        void init(){
            if (exitTextures.size() == 0)
                setExitTextures(gameRenderer.getContext());
            if (coinTextures.size() == 0)
                setCoinTextures(gameRenderer.getContext());
            if (teleportTextures.size() == 0)
                setTeleportTextures(gameRenderer.getContext());
            if (pointerTextures.size() == 0)
                setPointerTextures(gameRenderer.getContext());
            if (pathfinderTextures.size() == 0)
                setPathfinderTextures(gameRenderer.getContext());

            adjustBitmaps(exitTextures, true);
            adjustBitmaps(coinTextures, false);
            adjustBitmaps(teleportTextures,false);
            adjustBitmaps(pointerTextures,false);
            adjustBitmaps(pathfinderTextures,false);
        }
        void setCoinTextures(Context context){
            coinTextures.clear();
            coinTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim1));
            coinTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim2));
            coinTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim3));
            coinTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim4));
            coinTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim5));
            coinTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.coin_anim6));
        }
        void setExitTextures(Context context){
            exitTextures.clear();
            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_00));
            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_01));
            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_02));
            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_03));
            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_04));
            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_05));
            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_06));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_07));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_08));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_09));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_10));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_11));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_12));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_13));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_14));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_15));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_16));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_17));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_18));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_19));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_20));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_21));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_22));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_23));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_24));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_25));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_26));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_27));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_28));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_29));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_30));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_31));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_32));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_33));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_34));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_35));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_36));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_37));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_38));
//            exitTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.vortex_frame_39));


        }
        void setTeleportTextures(Context context){
            teleportTextures.clear();
            teleportTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.teleport));
        }
        void setPointerTextures(Context context){
            pointerTextures.clear();
            pointerTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.pointer));
        }
        void setPathfinderTextures(Context context){
            pathfinderTextures.clear();
            pathfinderTextures.add(BitmapFactory.decodeResource(context.getResources(), R.drawable.pathfinder));
        }

        CPoint.Field getFreeCell(){
            Random random = new Random(seed);
            do{
                CPoint.Field point = new CPoint.Field();
                point.set(random.nextInt(field.getxSize()), random.nextInt(field.getySize()));

                if (field.get(point)){
                    boolean can_place = true;
                    for (Entity entity:entities
                            ) {
                        if (entity.pos.equals(point)){
                            can_place = false;
                            break;
                        }
                    }
                    if (can_place)
                        return point;
                }
            }while (true);
        }

        void reset(){
            entities.clear();
        }
        void makeExit(CPoint.Field point){
            entities.push(new Exit(point, exitTextures));
        }
        void makeCoin(CPoint.Field point){
            entities.push(new Coin(point, coinTextures));
        }
        void makeTeleport(CPoint.Field point){
            entities.push(new Teleport(point, teleportTextures));
        }
        void makePointer(CPoint.Field point){
            entities.push(new Pointer(point, pointerTextures));
        }
        void makePathfinder(CPoint.Field point){
            entities.push(new Pathfinder(point, pathfinderTextures));
        }

        void dropCoins(){
            Random random = new Random(seed);
            int amount_of_coins = (field.getxSize() + field.getySize()) / 10;
            for (int i = 0; i < amount_of_coins; ++i)
                makeCoin(getFreeCell());
        }
        void dropBonuses(){
            Random random = new Random(seed);
            int nextint = random.nextInt(1000);
            if ((float)(nextint) / 10.f < pointerProbability){
                makePointer(getFreeCell());
            }
            nextint = random.nextInt(1000);
            if ((float)(nextint) / 10.f < teleportProbability){
                makeTeleport(getFreeCell());
            }
            nextint = random.nextInt(1000);
            if ((float)(nextint) / 10.f < pathfinderProbability){
                makePathfinder(getFreeCell());
            }
        }
        Entity intersectsWith(Point point){
            Entity ret = null;
            for (Entity entity : entities
                 ) {
                if (point.x == entity.pos.x && point.y == entity.pos.y){
                    ret = entity;
                    break;
                }
            }
            if (ret == null)
                return ret;

            if ("Exit".equals(ret.whoami))
                onExitReached();

            if ("Coin".equals(ret.whoami)){
                onCoinPickedUp();
                entities.remove(ret);
            }
            if ("Teleport".equals(ret.whoami)){
                onTeleportPickedUp();
                entities.remove(ret);
            }
            if ("Pathfinder".equals(ret.whoami)){
                onPathfinderPickedUp();
                entities.remove(ret);
            }
            if ("Pointer".equals(ret.whoami)){
                onPointerPickedUp();
                entities.remove(ret);
            }

            return ret;
        }

        void makeRandom(){

        }
    }
}

abstract class Entity{
    CPoint.Field pos;
    String whoami;
    //int anim_interval = 1;
    int anim_frame = 0;
    ArrayList<Bitmap> textures;

    boolean isLarge = false;
    Entity(CPoint.Field _pos, ArrayList<Bitmap> _textures){
        pos = new CPoint.Field(_pos);
        textures = _textures;
    }

    Bitmap getDrawTexture(){
        anim_frame = (anim_frame + 1) % textures.size();
        return textures.get(anim_frame);
    }
}

class Coin extends Entity{
    Coin(CPoint.Field point, ArrayList<Bitmap> textures){
        super(point, textures);
        whoami = "Coin";
    }
}
class Exit extends Entity{
    Exit(CPoint.Field point, ArrayList<Bitmap> textures){
        super(point, textures);
        whoami = "Exit";
        isLarge = true;
    }
}
class Teleport extends Entity{
    Teleport(CPoint.Field point, ArrayList<Bitmap> textures){
        super(point, textures);
        whoami = "Teleport";
    }
}
class Pointer extends Entity{
    Pointer(CPoint.Field point, ArrayList<Bitmap> textures){
        super(point, textures);
        whoami = "Pointer";
    }
}
class Pathfinder extends Entity{
    Pathfinder(CPoint.Field point, ArrayList<Bitmap> textures){
        super(point, textures);
        whoami = "Pathfinder";
    }
}
class Trace extends Entity{
    Trace(CPoint.Field point, ArrayList<Bitmap> textures){
        super(point, textures);
        whoami = "Trace";
    }
}
