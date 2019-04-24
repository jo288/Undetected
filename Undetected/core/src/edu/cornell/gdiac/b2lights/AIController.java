package edu.cornell.gdiac.b2lights;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import java.util.*;

public class AIController {
    /** Current Guard */
    private GuardModel guard;

    /** ObjectSet the guard has been alerted to */
    private HashSet<Obstacle> itemList;

    /** Board */
    private Board board;

    /** Current State of Guard */
    public FSMState state;

    /** Patrol Path of the Guard, Vector2 values are the respective patrol points in x,y screen coordinates */
    private Vector2[] path;
    private Vector2[] obPath;

    /** Index of the path the Guard is currently going towards */
    private int pathIndex;

    /** The current position the guard is trying to reach in x,y screen coordinates */
    private Vector2 currentGoal;

    /** Int that keeps track of whether the guard should update its movement */
    private int prev;

    /** Initial direction of the Guard */
    private float initialDirection;

    /** Priority Queue used for Path Finding*/
    private PriorityQueue<Node> queue;

    /** Timer for how long guard checks at an alarm */
    private int timer;

    /** Checker for the last time a guard was on a valid grid tile */
    private int gridTimer;

    /** Possible States of a Guard, sleeping, patrolling, in alert */
    private enum FSMState {
        SLEEP,
        PATROL,
        ALERT
    }

    /** Last Switch pulled by player*/
    private SwitchModel lastSwitch;

    /** Gets the guard's X position */
    public float getGuardX(){
        return guard.getX();
    }

    /** Gets the guard of the AIController */
    public GuardModel getGuard() {return guard;}

    /** Gets the guard's Y position */
    public float getGuardY() {return guard.getY();}

    /** Gets the guard's current Goal Tile */
    public Vector2 getCurrentGoal() {return currentGoal;}

    /** Initialize with current Guard */
    public AIController(Board board, GuardModel guard) {
        this.board = board;
        this.guard = guard;
        itemList = new HashSet<>();
        timer = 0;
        gridTimer = 0;
        queue = new PriorityQueue<>(new Comparator<Node>(){
            @Override
            public int compare(Node n1, Node n2){
                return n1.priority - n2.priority;
            }
        });
    }

    /** Sets the path for the guard, linear I.E. walk back and forth */
    public void setLinearPath(Vector2 start, Vector2 end) {
        path = new Vector2[2];
        path[0] = start;
        path[1] = end;
        currentGoal = start;
        pathIndex = 0;
    }

    /** Sets the path for the guard to be a generic array. The guard moves from array index 0 --> 1 --> 2 ... -> 0*/
    public void setPath(Vector2[] path) {
        this.path = path;
        currentGoal = path[0];
        pathIndex = 0;
    }

    public Vector2[] getObPath() {
        return obPath;
    }

    /** Sets the Guard to Patrol state */
    public void setPatrol() {
        state = FSMState.PATROL;
        guard.setAlarmed(false);
    }

    /** Sets the Guard to Alarmed state */
    public void setAlarmed() {
        state = FSMState.ALERT;
        guard.setAlarmed(true);
    }

    /** Sets the object the Guard is protecting */
    public void setProtect(Obstacle item) {
        itemList.add(item);
        currentGoal = new Vector2(item.getX(), item.getY());
        pathIndex = path.length - 1;
    }

    /** Sets the tile the Guard is protecting*/
    public void setProtect(float x, float y) {
        currentGoal = new Vector2(x, y);
        pathIndex = path.length - 1;
    }

    /** Main function to update the guard's velocity */
    public void update(){
//        System.out.println(guard.getX() + " " + guard.getY() + " : " + currentGoal);
        switch (state) {
            case SLEEP:
                // Do Nothing
                guard.setMovement(0,0);
                guard.applyForce();
                guard.walking = false; //TEMPORARY FIX
                currentGoal = path[0];
                pathIndex = 0;
                break;
            case PATROL:
                guard.walking = true;
            case ALERT:
                // Check if Guard is at patrol
                int goalx = board.physicsToBoard(currentGoal.x);
                int goaly = board.physicsToBoard(currentGoal.y);
                int guardx = board.physicsToBoard(guard.getX());
                int guardy = board.physicsToBoard(guard.getY());
                if (board.getOccupant(goalx, goaly) == 5) {
                    findClosest();
                    break;
                } else {
                    if (goalx == guardx && goaly == guardy) {
                        if (guard.getAlarmed() && path.length == 1) {
                            if (timer > 200) {
                                timer = 0;
                                guard.setAlarmed(false);
                            } else {
                                if (timer > 150) {
                                    guard.setDirection((float) Math.PI/2);
                                } else if (timer > 100) {
                                    guard.setDirection(0);
                                } else if (timer > 50) {
                                    guard.setDirection((float) -Math.PI/2);
                                } else if (timer > 0) {
                                    guard.setDirection((float) Math.PI);
                                }
                                guard.setMovement(0,0);
                                guard.applyForce();
                                guard.walking = false;
                                timer++;
                                break;
                            }
                        } else if (guard.getAlarmed()) {
                            if (timer > 200) {
                                timer = 0;
                                state = FSMState.PATROL;
                                guard.setAlarmed(false);
                            } else {
                                if (timer > 150) {
                                    guard.setDirection((float) Math.PI/2);
                                } else if (timer > 100) {
                                    guard.setDirection(0);
                                } else if (timer > 50) {
                                    guard.setDirection((float) -Math.PI/2);
                                } else if (timer > 0) {
                                    guard.setDirection((float) Math.PI);
                                }
                                guard.setMovement(0,0);
                                guard.applyForce();
                                guard.walking = false;
                                timer++;
                                break;
                            }
                        }
                        if (path.length == 1 && guardx == board.physicsToBoard(path[0].x) && guardy == board.physicsToBoard(path[0].y) && isGrid(guard)) {
                            guard.setDirection(initialDirection);
                            state = FSMState.SLEEP;
                        }
                        pathIndex = (pathIndex + 1) % path.length;
                        currentGoal = path[pathIndex];
                        goalx = board.physicsToBoard(currentGoal.x);
                        goaly = board.physicsToBoard(currentGoal.y);
                        board.setGoal(goalx, goaly);
                        break;
                    } else {
                        board.setGoal(goalx, goaly);
                    }
                }
                pathFind();
                board.resetTiles();
                break;
        }
    }

    /** Finds closest tile to current goal and sets it as current goal */
    private void findClosest() {
        PriorityQueue<Node> temp = new PriorityQueue<>(new Comparator<Node>(){
            @Override
            public int compare(Node n1, Node n2){
                return n1.priority - n2.priority;
            }
        });
        board.clearMarks();
        int goalx = board.physicsToBoard(currentGoal.x);
        int goaly = board.physicsToBoard(currentGoal.y);
        int guardx = board.physicsToBoard(guard.getX());
        int guardy = board.physicsToBoard(guard.getY());
        int prev = pathIndex-1 < 0 ? path.length-1 : pathIndex-1;
        Vector2 prevgoal = path[prev];
        lastSwitch = null;
        board.setGoal(goalx+1, goaly);
        if (board.isWalkable(goalx+1, goaly) && bfs(guardx, guardy) != 0) {
            Node n1 = new Node(goalx+1, goaly, 0, 1);
            n1.priority = manDist(board.physicsToBoard(prevgoal.x), board.physicsToBoard(prevgoal.y), goalx+1, goaly);
            temp.add(n1);
        }
        board.clearMarks();
        board.setGoal(goalx-1, goaly);
        if (board.isWalkable(goalx-1, goaly) && bfs(guardx, guardy) != 0) {
            Node n1 = new Node(goalx-1, goaly, 0, 1);
            n1.priority = manDist(board.physicsToBoard(prevgoal.x), board.physicsToBoard(prevgoal.y), goalx-1, goaly);
            temp.add(n1);
        }
        board.clearMarks();
        board.setGoal(goalx, goaly+1);
        if (board.isWalkable(goalx, goaly+1) && bfs(guardx, guardy) != 0) {
            Node n1 = new Node(goalx, goaly+1, 0, 1);
            n1.priority = manDist(board.physicsToBoard(prevgoal.x), board.physicsToBoard(prevgoal.y), goalx, goaly+1);
            temp.add(n1);

        }
        board.clearMarks();
        board.setGoal(goalx, goaly-1);
        if (board.isWalkable(goalx, goaly-1) && bfs(guardx, guardy) != 0) {
            Node n1 = new Node(goalx, goaly-1, 0, 1);
            n1.priority = manDist(board.physicsToBoard(prevgoal.x), board.physicsToBoard(prevgoal.y), goalx, goaly-1);
            temp.add(n1);

        }
        Node closest = temp.poll();
        if (closest != null) currentGoal = new Vector2(board.boardToScreen(closest.x) , board.boardToScreen(closest.y));
        board.clearMarks();
//        System.out.println(closest.x +  " " + closest.y);
    }

    /** Finds the shortest path to goal tile and changes the guard's velocity accordingly
     *
     */
    private void pathFind () {
        int guardx = board.physicsToBoard(guard.getX());
        int guardy = board.physicsToBoard(guard.getY());
        int i, spd;

        if (!isGrid(guard)) {
            i = prev;
            gridTimer++;
            if (gridTimer > 75) {
                i = -prev;
            }
        } else {
            if (gridTimer > 75) {
                if (timer > 200) {
                    if (guard.getAlarmed()) guard.setAlarmed(false);
                    currentGoal = path[0];
                    pathIndex = 0;
                    timer = 0;
                    gridTimer = 0;
                } else if (timer > 150) {
                    guard.setDirection((float) Math.PI/2);
                } else if (timer > 100) {
                    guard.setDirection(0);
                } else if (timer > 50) {
                    guard.setDirection((float) -Math.PI/2);
                } else if (timer > 0) {
                    guard.setDirection((float) Math.PI);
                }
                guard.setMovement(0,0);
                guard.applyForce();
                guard.walking = false;
                timer++;
                i = 0;
            } else {
                gridTimer = 0;
                i = bfs(guardx, guardy);
            }
        }

        if (guard.getAlarmed()) {
            spd = 30;
        } else {
            spd = 20;
        }

        if (i == 0) {
            guard.walking = false;
        } else if (i == 1) {
            guard.setDirection(-(float) Math.PI/2);
            guard.setMovement(spd,0);
            guard.applyForce();
            guard.walking = true;
        } else if (i == -1) {
            guard.setDirection((float) Math.PI/2);
            guard.setMovement(-spd,0);
            guard.applyForce();
            guard.walking = true;
        } else if (i == 2) {
            guard.setDirection(0);
            guard.setMovement(0, spd);
            guard.applyForce();
            guard.walking = true;
        } else if (i == -2){
            guard.setDirection((float) Math.PI);
            guard.setMovement(0,-spd);
            guard.applyForce();
            guard.walking = true;
        }
        board.clearMarks();
    }

    /** BFS that looks for shortest path to current goal tile
     *
     *  returns 0 if character is already on Goal (Should not happen)
     *         -1 if character needs to move left
     *          1 if character needs to move right
     *         -2 if character needs to move down
     *          2 if character needs to move up
     */
    private int bfs (int startX, int startY) {
        queue.clear();
        Node start = new Node(startX, startY, 0,0);
        queue.add(start);
        int debug = 0;
        if (board.isGoal(startX, startY)) return 0;
        while (!queue.isEmpty()) {
            debug++;
            if (debug > board.getHeight() * board.getWidth()) {
                if (lastSwitch != null) {
                    updateAISwitch();
                }
//                currentGoal = path[0];
                pathIndex = 0;
                return 0;
            }
            Node n = queue.poll();
            board.setVisited(n.x, n.y);
            if (board.isGoal(n.x, n.y)) {
                prev = n.act;
                return n.act;
            }
            if (board.isWalkable(n.x+1, n.y) && !board.isVisited(n.x+1, n.y))  {
                int act = n.act == 0 ? 1 : n.act;
                Node n1 = new Node(n.x+1, n.y, act, n.cost + 1);
                queue.add(n1);
            }
            if (board.isWalkable(n.x-1, n.y) && !board.isVisited(n.x-1, n.y)) {
                int act = n.act == 0 ? -1 : n.act;
                Node n1 = new Node(n.x-1, n.y, act, n.cost + 1);
                queue.add(n1);
            }
            if (board.isWalkable(n.x, n.y+1) && !board.isVisited(n.x, n.y+1)) {
                int act = n.act == 0 ? 2 : n.act;
                Node n1 = new Node(n.x, n.y+1, act, n.cost + 1);
                queue.add(n1);
            }
            if (board.isWalkable(n.x, n.y-1) && !board.isVisited(n.x, n.y-1)) {
                int act = n.act == 0 ? -2 : n.act;
                Node n1 = new Node(n.x, n.y-1, act, n.cost + 1);
                queue.add(n1);
            }
        }
        return 0;
    }

    /** Sets the last switched switch for the guards */
    public void setLastSwitch (SwitchModel swtch){
        lastSwitch = swtch;
    }

    /** Updates the guard's current goal to the closest tile of the closest door. */
    public void updateAISwitch() {
        if (lastSwitch == null) return;
        DoorModel closest = lastSwitch.getDoors().get(0);
        for (DoorModel door : lastSwitch.getDoors()) {
            if (manDist(board.physicsToBoard(door.getX()), board.physicsToBoard(door.getY()), board.physicsToBoard(guard.getX()), board.physicsToBoard(guard.getY()))
                    < manDist(board.physicsToBoard(closest.getX()), board.physicsToBoard(closest.getY()), board.physicsToBoard(guard.getX()), board.physicsToBoard(guard.getY()))) {
                closest = door;
            }
        }
        System.out.println(closest.getX() + " " + closest.getY());
        setProtect(closest);
        findClosest();
        System.out.println(currentGoal);
    }

    /** Private class for keeping track of board tiles */
    private class Node {
        public int x;
        public int y;
        public int cost;

        // -1 for -x, 1 for +x, -2 for -y, 2 for +y
        public int act;
        public int priority;

        public Node(int x, int y, int act, int cost) {
            this.x = x;
            this.y = y;
            this.cost = cost;
            this.act = act;
            int goalx = board.physicsToBoard(currentGoal.x);
            int goaly = board.physicsToBoard(currentGoal.y);
            this.priority = manDist(x,y,goalx,goaly) + cost;
        }

        // Debug
        public String toString(){
            String s = this.x+" "+this.y;
            return s;
        }
    }

    /** Checks if Guard is currently on a valid tile I.E. guards can only walk on the center of tiles.*/
    public boolean isGrid(GuardModel guard) {
        float gX = guard.getX() * 10;
        float gY = guard.getY() * 10;
        int bX = (int) (gX);
        int bY = (int) (gY);
        return (bX%10 == 5 && bY%10 == 5);
    }

    /** Initializes path values */
    public void initialize(JsonValue json) {
        if (json.has("path")) {
            float[] paths = json.get("path").asFloatArray();
            path = new Vector2[paths.length / 2];
            for (int i = 0; i < paths.length / 2; i++) {
                int j = i * 2;
                path[i] = new Vector2(paths[j] + 0.5f , paths[j + 1] + 0.5f );
            }
        }
        if (json.has("objectivepath")) {
            float[] obPaths = json.get("objectivepath").asFloatArray();
            obPath = new Vector2[obPaths.length / 2];
            for (int i = 0; i < obPaths.length / 2; i++) {
                int j = i * 2;
                obPath[i] = new Vector2(obPaths[j] + 0.5f , obPaths[j + 1] + 0.5f );
            }
        }

        String status = json.get("status").asString();
        if (status.equals("sleep")) {
            state = FSMState.SLEEP;
        }
        if (status.equals("patrol")) {
            state = FSMState.PATROL;
            currentGoal = path[0];
            pathIndex = 0;
            guard.walking = true;
        }

        String dir = json.get("direction").asString();

        if (dir.equals("left")) {
            guard.setDirection(( float) Math.PI/2);
        } else if (dir.equals("right")) {
            guard.setDirection((float) -Math.PI/2);
        } else if (dir.equals("up")) {
            guard.setDirection(0f);
        } else if (dir.equals("down")) {
            guard.setDirection((float) Math.PI);
        } else {
            guard.setDirection(0f);
        }

        initialDirection = guard.getDirectionFloat();
    }

    /** Returns the Manhattan Distance of two points */
    private int manDist(int x0, int y0, int x1, int y1) {
        return Math.abs(x1 - x0) + Math.abs(y1 - y0);
    }
}
