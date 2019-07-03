package bakar.labyrinta;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.ArrayMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.logging.Logger;

import static bakar.labyrinta.StoredProgress.getInstance;
import static bakar.labyrinta.StoredProgress.isNeedToLightBonusButton;
import static bakar.labyrinta.StoredProgress.isNeedToShowTutorialPointer;
import static bakar.labyrinta.TutorialKey.NextLevelBuyTutorial;

/**
 * Created by Bakar on 10.03.2018.
 */

class GameLogic {
    boolean isInited = false;
    boolean usesJoystick;
    boolean remote_move_flag = true;

    boolean pointerActive = false;
    boolean pathfinderActive = false;
    boolean teleportActive = false;

    int level_difficulty = 1;

    int pointerAmount = 0;
    int teleportAmount = 0;
    int pathfinderAmount = 0;

    float cellSize = 10;

    VelosityControllerInterface tiltControler = null;

    GameRenderer gameRenderer;
    Field field;
    Node currentNode;
    private CPoint.Game playerPt;
    long seed = 0;
    Joystick joystick;
    LinkedList<CPoint.Game> finded_path;
    EntityFactory eFactory = new EntityFactory();
    private int goldEarnedByCoins = 0;
    private int goldEarnedByLevel = 0;

    CPoint.Game debugTouchGameCoord = new CPoint.Game(0, 0);

    Queue<CPoint.Field> traces = new LinkedList<>();
    private int tracesSize = 10;

    GameLogic(GameRenderer gameRenderer_, long _seed, int _xsize, int _ysize){
        Logger.getAnonymousLogger().info("GameLogic.ctor");
        if (gameRenderer_ != null) {
            gameRenderer = gameRenderer_;
            cellSize = GameRenderer.cellSize;
        }
        init(_seed, _xsize, _ysize);
    }

    boolean isCoordIsWithinField(CPoint.Game coord){
        CPoint.Field fc = new CPoint.Field(game2field(coord));
        boolean[] flags ={
                fc.x <= 0,
                fc.x >= field.getxSize() - 1,
                fc.y <= 0,
                fc.y >= field.getySize() - 1
        };
        for (boolean f : flags){
            if (f){
                return false;
            }
        }
        return true;
    }
    public CPoint.Field game2field(CPoint.Game value){
        CPoint.Field result = new CPoint.Field();
        double x, y;
        x = Math.floor(value.x / cellSize);
        y = Math.floor(value.y / cellSize);
        result.set((int)x, (int)y);
        return result;
    }
    public CPoint.Game field2game(CPoint.Field value){
        CPoint.Game result = new CPoint.Game();
        result.set(value.x * cellSize + cellSize / 2, value.y * cellSize + cellSize / 2);
        return result;
    }

    void init(int xsize, int ysize){
        Logger.getAnonymousLogger().info("GameLogic.init()");
        field = new Field(xsize, ysize);
        field.init(seed);

        playerPt = field2game(field.startPos); // gameCoords

        currentNode = new Node(game2field(playerPt));
        currentNode.updateLinks();

        traces.clear();
        traces.add(game2field(playerPt));

        goldEarnedByCoins = 0;
        goldEarnedByLevel = 0;

        eFactory.reset();
        eFactory.makeExit(field.exitPos);
        //eFactory.makePointer(new Point(1, 1));
        //eFactory.makeTeleport(new Point(3, 3));
        //eFactory.makePathfinder(new Point(5, 5));

        eFactory.dropCoins();
        eFactory.dropBonuses();

        isInited = true;
        remote_move_flag = true;
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
    float getPathLength(LinkedList<CPoint.Game> path){
        float result = 0;

        for (int i = 1; i < path.size(); ++i){
            result += distance(path.get(i - 1), path.get(i));
        }

        return result;
    }

    void startEndActivity(){
        Intent intent = new Intent(gameRenderer.getContext(), EndActivity.class);
        intent.putExtra("goldEarnedByCoins", goldEarnedByCoins);
        intent.putExtra("goldEarnedByLevel", goldEarnedByLevel);
        ((Activity)gameRenderer.getContext()).startActivityForResult(intent, 42);
    }

    void startTutorialActivity(TutorialKey key){
        Intent intent = new Intent(gameRenderer.getContext(), TutorialActivity.class);
        intent.putExtra(TutorialKey.class.toString(), String.valueOf(key));
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
        currentNode = new Node(game2field(playerCoords()));
        currentNode.updateLinks();
        gameRenderer.lightFog(playerCoords());
        eFactory.intersectsWith(game2field(playerCoords()));
        teleportAmount--;
    }
    float getTeleportRadius(){
        return cellSize * (5 + 1.1f * StoredProgress.getInstance().getTeleportUpg()); // game
    }
    float getPathfinderRadius(){
        return cellSize * (8 + 1.3f * StoredProgress.getInstance().getPathfinderUpg()); // game
    }
    CPoint.Game getClosestFloorCoord(CPoint.Game gm){
        CPoint.Field fi = game2field(gm);
        if (field.get(fi))
            return field2game(game2field(gm)); // получаем центр клетки

        for (Direction dir : Direction.values()
             ) {
            if (field.get(field.ptAt(fi, dir)))
                return field2game(field.ptAt(fi, dir));
        }
        return field2game(new CPoint.Field(1, 1));
    }

    void onExitReached(){
        remote_move_flag = false;
        goldEarnedByLevel =
                Economist.getInstance().getLevelReward(
                        Economist.hypot(new Point(field.getxSize(), field.getySize())));

        gameRenderer.onTouchUp(null);
        seed = System.currentTimeMillis();
        finded_path = null;
        pointerActive = false;

        SoundCore.inst().playSound(Sounds.levelFinished);

        boolean flag = false;
        if (StoredProgress.getInstance().getValueBoolean(StoredProgress.isNeedToShowTutorialNextLevel)){

            String dataKey = StoredProgress.levelUpgKey;
            int level_value = getInstance().getValue(dataKey);
            int level_cost = Economist.getInstance().price_map.get(dataKey).apply(level_value);

            if ((StoredProgress.getInstance().getGoldAmount() + goldEarnedByLevel + goldEarnedByCoins) >= level_cost){
                flag = true;
                getInstance().
                        switchValueBoolean(StoredProgress.isNeedToShowTutorialNextLevel);
                Intent tutorialIntent = new Intent(gameRenderer.getContext(), TutorialActivity.class);
                tutorialIntent.putExtra(TutorialKey.class.toString(), String.valueOf(NextLevelBuyTutorial));
                ((Activity)gameRenderer.getContext()).startActivityForResult(tutorialIntent, 42);
            }
        }

        if (!flag){
            startEndActivity();
        }

    }
    void onCoinPickedUp(){
        int pickedUpCoinCost = Economist.getInstance().getCoinCostRand();
        goldEarnedByCoins += pickedUpCoinCost;
        gameRenderer.addPickUpAnimation(playerCoords(), "+" + String.valueOf(pickedUpCoinCost));
        SoundCore.inst().playSound(Sounds.coinPickedUp);
    }
    void onPointerPickedUp(){
        pointerAmount++;
        gameRenderer.addPickUpAnimation(playerCoords(), "+1");
        SoundCore.inst().playSound(Sounds.bonusPickedUp);
        if (StoredProgress.getInstance().getValueBoolean(
                isNeedToLightBonusButton)){
            gameRenderer.buttons.get(1).lightAnimationEnabled = true;
            StoredProgress.getInstance().
                    switchValueBoolean(StoredProgress.isNeedToLightBonusButton);
        }
        if (StoredProgress.getInstance().getValueBoolean(
                isNeedToShowTutorialPointer)){
            startTutorialActivity(TutorialKey.PointerTutorial);
            StoredProgress.getInstance().
                    switchValueBoolean(StoredProgress.isNeedToShowTutorialPointer);
        }
    }
    void onTeleportPickedUp(){
        teleportAmount++;
        gameRenderer.addPickUpAnimation(playerCoords(), "+1");
        SoundCore.inst().playSound(Sounds.bonusPickedUp);
        if (StoredProgress.getInstance().getValueBoolean(
                isNeedToLightBonusButton)){
            gameRenderer.buttons.get(1).lightAnimationEnabled = true;
            StoredProgress.getInstance().
                    switchValueBoolean(StoredProgress.isNeedToLightBonusButton);
        }
        if (StoredProgress.getInstance().getValueBoolean(
                StoredProgress.isNeedToShowTutorialTeleport)){
            startTutorialActivity(TutorialKey.TeleportTutorial);
            StoredProgress.getInstance().
                    switchValueBoolean(StoredProgress.isNeedToShowTutorialTeleport);
        }

    }
    void onPathfinderPickedUp(){
        pathfinderAmount++;
        gameRenderer.addPickUpAnimation(playerCoords(), "+1");
        SoundCore.inst().playSound(Sounds.bonusPickedUp);
        if (StoredProgress.getInstance().getValueBoolean(
                isNeedToLightBonusButton)){
            gameRenderer.buttons.get(1).lightAnimationEnabled = true;
            StoredProgress.getInstance().
                    switchValueBoolean(StoredProgress.isNeedToLightBonusButton);
        }
        if (StoredProgress.getInstance().getValueBoolean(
                StoredProgress.isNeedToShowTutorialPathfinder)){
            startTutorialActivity(TutorialKey.PathfinderTutorial);
            StoredProgress.getInstance().
                    switchValueBoolean(StoredProgress.isNeedToShowTutorialPathfinder);
        }
    }
    void onCellChanged(){
        gameRenderer.lightFog(playerCoords());
    }

    void updateTraces(CPoint.Game pointF){

        if ((((LinkedList<CPoint.Field>)traces).getLast().x != game2field(pointF).x) ||
                (((LinkedList<CPoint.Field>)traces).getLast().y != game2field(pointF).y)){
            if (traces.size() >= tracesSize)
                traces.poll();
            traces.add(game2field(pointF));

            onCellChanged();
        }
    }
    boolean canMovePlayer(CPoint.Screen pointF){
        if (usesJoystick)
            return joystick.isTouched(pointF);
        else
            return distance(pointF, gameRenderer.game2screen(playerCoords())) < gameRenderer.playerHitbox;
    }
    void remoteMove(){
        if (usesJoystick && gameRenderer.isMovingPlayer)
            movePlayerTo(joystick.lastTouch);
        if (!usesJoystick && remote_move_flag && tiltControler != null){
            CPoint.Game l_playerPt = new CPoint.Game(playerPt.x, playerPt.y);
            // FIXME: 5/2/19 Вылетит тут, если контроллер не подхватился
            CPoint.Game vel = tiltControler.getCurrentVelocity();
            l_playerPt.offset(vel.x, vel.y);
            movePlayerTo(l_playerPt);
        }
    }
    void movePlayerTo(CPoint.Game pointF){
        if (usesJoystick){
            gameRenderer.buttons.get(0).lightAnimationEnabled = false;
            if (!gameRenderer.isPlayerInSight())
                gameRenderer.buttons.get(0).onClick();
        }
        else{
            gameRenderer.buttons.get(0).lightAnimationEnabled = !gameRenderer.isPlayerInSight();
        }

        float exit_distance = distance(exitCoords(), playerCoords());
        if (exit_distance < cellSize * 1.1f){
            Line line = new Line(playerCoords(), exitCoords());
            PointF additional_movement = line.normalizedVector();

            float coef = (cellSize * 5) / exit_distance;

            additional_movement.x *= coef;
            additional_movement.y *= coef;

            pointF.x += additional_movement.x;
            pointF.y += additional_movement.y;
        }

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
            // FIXME: 4/24/19 java.lang.NullPointerException
            nearestRail.set(field2game(currentNode.pos),
                    field2game(currentNode.links.get(currentNode.availableDirections.get(i)).pos));

            if (distance(nearestRail.projection(input), input) < distanceToRail){

                newPlayerPt = nearestRail.projection(input);
                distanceToRail = distance(nearestRail.projection(input), input);
            }
        }

        if (distanceToRail > cellSize * 3)
            gameRenderer.isMovingPlayer = false;
        else{
            playerPt.x = newPlayerPt.x;
            playerPt.y = newPlayerPt.y;
            updateTraces(playerPt);
        }

        for (int i = 0; i < currentNode.availableDirections.size(); ++i){
            if (distance(newPlayerPt,
                    field2game(currentNode.links.get(currentNode.availableDirections.get(i)).pos)) <
                    gameRenderer.getCellSize() * (3 / 2)){
                currentNode = currentNode.links.get(currentNode.availableDirections.get(i));
                currentNode.updateLinks();
            }
        }
        eFactory.intersectsWith(game2field(playerCoords()));
    }
    CPoint.Game playerCoords(){ // gameCoord
        return new CPoint.Game(playerPt.x, playerPt.y);
    }
    CPoint.Game exitCoords(){
        return new CPoint.Game(field2game(field.exitPos).x, field2game(field.exitPos).y);
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
            if (result.x == 0 && result.y == 0)
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
        float speed = (cellSize * 20 / 70);

        Joystick(CPoint.Screen _mainPos, float _mainRadius){
            mainPos = _mainPos;
            curPos = new CPoint.Screen(_mainPos.x, _mainPos.y);
            mainRadius = _mainRadius;
            stickRadius = mainRadius / 2;
            touchReleased();
        }

        boolean isTouched(CPoint.Screen pointF){ // screencoord
            if (distance(pointF, mainPos) < mainRadius){
                curPos = pointF;
                return true;
            }
            else
                return false;
        }

        CPoint.Game getPlayerOffsetOnMove(CPoint.Game pointF){ // gameCoord на инпуте // аккуратно с конвертацией
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
            curPos.set(mainPos.x, mainPos.y);
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
            starNode = new Node(game2field(star_));
            if (field.isNode(game2field(cross_))){
                crossNode = new Node(game2field(cross_));
            }
            else {
                crossNode = getClosestToPoint(new Node(game2field(cross_)).getLinkedNodes(), game2field(cross_));
            }
            cross = game2field(cross_);
            star = game2field(star_);
        }

        Node getClosestToPoint(LinkedList<Node> nodes, CPoint.Field pt){
            Node result = new Node(new CPoint.Field(0, 0));
            double min_dist = 10e3;
            for (Node node: nodes
                 ) {
                if (distance(field2game(node.pos), field2game(pt)) < min_dist){
                    min_dist = distance(field2game(node.pos), field2game(pt));
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
                result.add(result.size(), field2game(aStarNode.pos));
            }
            result.add(result.size(), field2game(cross));

            if (contains(new Node(cross).getLinkedNodes(), new Node(game2field(result.get(result.size() - 3)))))
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
        private boolean isLocked = false;

        LinkedList<Entity> entities = new LinkedList<>();

        void init(){
        }
        void lock(){
            while (isLocked){
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
            isLocked = true;
        }
        void unlock(){
            isLocked = false;
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
            eFactory.lock();
            entities.clear();
            eFactory.unlock();
        }
        void makeExit(CPoint.Field point){
            entities.push(new Exit(point));
        }
        void makeCoin(CPoint.Field point){
            entities.push(new Coin(point));
        }
        void makeTeleport(CPoint.Field point){
            entities.push(new Teleport(point));
        }
        void makePointer(CPoint.Field point){
            entities.push(new Pointer(point));
        }
        void makePathfinder(CPoint.Field point){
            entities.push(new Pathfinder(point));
        }

        void dropCoins(){
            int amount_of_coins = Economist.getInstance().getCoinsAmountRand(field.getHypot());
            for (int i = 0; i < amount_of_coins; ++i)
                makeCoin(getFreeCell());
        }
        void dropBonuses(){
            Random random = new Random(seed);
            int bound = 10000;
            int nextint = random.nextInt(bound);
            if ((float)(nextint) / (float)bound < Economist.getInstance().getPointerPropability(field.getHypot())){
                makePointer(getFreeCell());
            }
            nextint = random.nextInt(bound);
            if ((float)(nextint) / (float)bound < Economist.getInstance().getTeleportPropability(field.getHypot())){
                makeTeleport(getFreeCell());
            }
            nextint = random.nextInt(bound);
            if ((float)(nextint) / (float)bound < Economist.getInstance().getPathfinderPropability(field.getHypot())){
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
                eFactory.lock();
                entities.remove(ret);
                eFactory.unlock();
            }
            if ("Teleport".equals(ret.whoami)){
                onTeleportPickedUp();
                eFactory.lock();
                entities.remove(ret);
                eFactory.unlock();
            }
            if ("Pathfinder".equals(ret.whoami)){
                onPathfinderPickedUp();
                eFactory.lock();
                entities.remove(ret);
                eFactory.unlock();
            }
            if ("Pointer".equals(ret.whoami)){
                onPointerPickedUp();
                eFactory.lock();
                entities.remove(ret);
                eFactory.unlock();
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
    int anim_frame = 0;
    boolean isLarge = false;

    int incrAnimFrame(int max){
        anim_frame = (anim_frame + 1) % max;
        return anim_frame;
    }

    Entity(CPoint.Field _pos){
        pos = new CPoint.Field(_pos);
    }
}

class Coin extends Entity{
    Coin(CPoint.Field point){
        super(point);
        whoami = "Coin";
    }
}
class Exit extends Entity{
    Exit(CPoint.Field point){
        super(point);
        whoami = "Exit";
        isLarge = true;
    }
}
class Teleport extends Entity{
    Teleport(CPoint.Field point){
        super(point);
        whoami = "Teleport";
    }
}
class Pointer extends Entity{
    Pointer(CPoint.Field point){
        super(point);
        whoami = "Pointer";
    }
}
class Pathfinder extends Entity{
    Pathfinder(CPoint.Field point){
        super(point);
        whoami = "Pathfinder";
    }
}
class Trace extends Entity{
    Trace(CPoint.Field point){
        super(point);
        whoami = "Trace";
    }
}