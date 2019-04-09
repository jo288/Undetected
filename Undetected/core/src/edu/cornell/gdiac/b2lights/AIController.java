package edu.cornell.gdiac.b2lights;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.Obstacle;

import java.util.*;

public class AIController {
    /** Current Guard */
    private GuardModel guard;

    /** ObjectSet the guard has been alerted to */
    private HashSet<Obstacle> itemlist;

    /** Board */
    private Board board;

    /** Current State of Guard */
    public FSMState state;

    /** Patrol Path of the Guard, Vector2 values are the respective patrol points in x,y screen coordinates */
    private Vector2[] path;

    /** Index of the path the Guard is currently going towards */
    private int pathIndex;

    /** The current position the guard is trying to reach in x,y screen coordinates */
    private Vector2 currentGoal;

    /** Int that keeps track of whether the guard should update its movement */
    private int prev;

    /** Possible States of a Guard, sleeping, patrolling, in alert */
    private enum FSMState {
        SLEEP,
        PATROL,
        ALERT
    }

    /** Initialize with current Guard */
    public AIController(Board board, GuardModel guard) {
        this.board = board;
        this.guard = guard;
        state = FSMState.SLEEP;
        itemlist = new HashSet<>();
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

    /** Sets the Guard to Patrol state */
    public void setPatrol() {
        state = FSMState.PATROL;
    }

    /** Sets the Guard to Alarmed state */
    public void setAlarmed() { state = FSMState.ALERT; }

    /** Sets the object the Guard is protecting */
    public void setProtect(Obstacle item) {
        if (!itemlist.contains(item)) {
            itemlist.add(item);
            currentGoal = new Vector2(item.getX(), item.getY());
            Vector2[] newPath = new Vector2[path.length + 1];
            for (int i = 0; i < path.length; i++) {
                newPath[i] = path[i];
            }
            newPath[newPath.length - 1] = currentGoal;
            path = newPath;
            pathIndex = newPath.length - 1;
        }
    }

    /** Sets the tile the Guard is protecting*/
    public void setProtect(float x, float y) {
        currentGoal = new Vector2(x, y);
        Vector2[] newPath = new Vector2[path.length + 1];
        for (int i = 0; i < path.length; i++) {
            newPath[i] = path[i];
        }
        newPath[newPath.length-1] = currentGoal;
        path = newPath;
        pathIndex = newPath.length-1;
    }

    /** Main function to update the guard's velocity */
    public void update(){
        switch (state) {
            case SLEEP:
                // Do Nothing
                break;
            case PATROL:
            case ALERT:
                // Check if Guard is at patrol
                int goalx = board.physicsToBoard(currentGoal.x);
                int goaly = board.physicsToBoard(currentGoal.y);
                int guardx = board.physicsToBoard(guard.getX());
                int guardy = board.physicsToBoard(guard.getY());
                if (board.getOccupant(goalx, goaly) == 5) {
                    PriorityQueue<Node> queue = new PriorityQueue<>(
                            new Comparator<Node>(){
                                @Override
                                public int compare(Node n1, Node n2) {return n1.priority - n2.priority; }});
                    int prev = pathIndex-1 < 0 ? path.length-1 : pathIndex-1;
                    Vector2 prevgoal = path[prev];
                    if (board.isSafeAt(goalx+1, goaly) && (board.getOccupant(goalx+1,goaly) == 0 || board.getOccupant(goalx+1,goaly) == 4 || board.getOccupant(goalx+1,goaly) == 3 || board.getOccupant(goalx+1,goaly) == 6 || board.getOccupant(goalx+1,goaly) == 8)) {
                        Node n1 = new Node(goalx+1, goaly, 0);
                        n1.priority = manDist(board.screenToBoard(prevgoal.x), board.screenToBoard(prevgoal.y), goalx+1, goaly);
                        queue.add(n1);
                    }
                    if (board.isSafeAt(goalx-1, goaly) && (board.getOccupant(goalx-1, goaly) == 0 || board.getOccupant(goalx-1, goaly) == 4 || board.getOccupant(goalx-1, goaly) == 3 || board.getOccupant(goalx-1, goaly) == 6 || board.getOccupant(goalx-1, goaly) == 8)) {
                        Node n1 = new Node(goalx-1, goaly, 0);
                        n1.priority = manDist(board.screenToBoard(prevgoal.x), board.screenToBoard(prevgoal.y), goalx-1, goaly);
                        queue.add(n1);
                    }
                    if (board.isSafeAt(goalx, goaly+1) && (board.getOccupant(goalx, goaly+1) == 0 || board.getOccupant(goalx, goaly+1) == 4 || board.getOccupant(goalx, goaly+1) == 3 || board.getOccupant(goalx, goaly+1) == 6 || board.getOccupant(goalx, goaly+1) == 8)) {
                        Node n1 = new Node(goalx, goaly+1, 0);
                        n1.priority = manDist(board.screenToBoard(prevgoal.x), board.screenToBoard(prevgoal.y), goalx, goaly+1);
                        queue.add(n1);
                    }
                    if (board.isSafeAt(goalx, goaly-1) && (board.getOccupant(goalx, goaly-1) == 0 || board.getOccupant(goalx, goaly-1) == 4 || board.getOccupant(goalx, goaly-1) == 3 || board.getOccupant(goalx, goaly-1) == 6 || board.getOccupant(goalx, goaly-1) == 8)) {
                        Node n1 = new Node(goalx, goaly-1, 0);
                        n1.priority = manDist(board.screenToBoard(prevgoal.x), board.screenToBoard(prevgoal.y), goalx, goaly-1);
                        queue.add(n1);
                    }
                    Node closest = queue.poll();
                    currentGoal = new Vector2(board.boardToPhysics(closest.x), board.boardToPhysics(closest.y));
                    board.resetTiles();
                    board.setGoal(closest.x, closest.y);
                } else {
                    if (goalx == guardx && goaly == guardy) {
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

    /** Finds the shortest path to goal tile and changes the guard's velocity accordingly
     *
     */
    private void pathFind () {
        int guardx = board.physicsToBoard(guard.getX());
        int guardy = board.physicsToBoard(guard.getY());
        int i;

        if (!isGrid(guard)) {
            i = prev;
        } else {
            i = bfs(guardx, guardy);
        }

        if (i == 0) {
            guard.setMovement(0,0);
        } else if (i == 1) {
            guard.setDirection(-(float) Math.PI/2);
            guard.setMovement(25,0);
            guard.applyForce();
        } else if (i == 2) {
            guard.setDirection(0);
            guard.setMovement(0, 25);
            guard.applyForce();
        } else if (i == -1) {
            guard.setDirection((float) Math.PI/2);
            guard.setMovement(-25,0);
            guard.applyForce();
        } else if (i == -2){
            guard.setDirection((float) Math.PI);
            guard.setMovement(0,-25);
            guard.applyForce();
        }
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
        Queue<Node> queue = new LinkedList<Node>(
//                new Comparator<Node>(){
//                    @Override
//                    public int compare(Node n1, Node n2){
//                        return n1.priority - n2.priority;
//                    }
//                }
        );
        Node start = new Node(startX, startY, 0);
        queue.add(start);
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            board.setVisited(n.x, n.y);
            if (board.isGoal(n.x, n.y)) {
                prev = n.act;
                return n.act;
            }
            if (board.isSafeAt(n.x+1, n.y) && (board.getOccupant(n.x+1,n.y) == 0 || board.getOccupant(n.x+1,n.y) == 4 || board.getOccupant(n.x+1,n.y) == 3 || board.getOccupant(n.x+1,n.y) == 6 || board.getOccupant(n.x+1,n.y) == 8) && !board.isVisited(n.x+1, n.y))  {
                int act = n.act == 0 ? 1 : n.act;
                Node n1 = new Node(n.x+1, n.y, act);
                queue.add(n1);
            }
            if (board.isSafeAt(n.x-1, n.y) && (board.getOccupant(n.x-1,n.y) == 0 || board.getOccupant(n.x-1,n.y) == 4 || board.getOccupant(n.x-1,n.y) == 3 || board.getOccupant(n.x-1,n.y) == 6 || board.getOccupant(n.x-1,n.y) == 8) && !board.isVisited(n.x-1, n.y)) {
                int act = n.act == 0 ? -1 : n.act;
                Node n1 = new Node(n.x-1, n.y, act);
                queue.add(n1);
            }
            if (board.isSafeAt(n.x, n.y+1) && (board.getOccupant(n.x,n.y+1) == 0 || board.getOccupant(n.x,n.y+1) == 4 || board.getOccupant(n.x,n.y+1) == 3 || board.getOccupant(n.x,n.y+1) == 6 || board.getOccupant(n.x,n.y+1) == 8) && !board.isVisited(n.x, n.y+1)) {
                int act = n.act == 0 ? 2 : n.act;
                Node n1 = new Node(n.x, n.y+1, act);
                queue.add(n1);
            }
            if (board.isSafeAt(n.x, n.y-1) && (board.getOccupant(n.x,n.y-1) == 0 || board.getOccupant(n.x,n.y-1) == 4 || board.getOccupant(n.x,n.y-1) == 3 || board.getOccupant(n.x,n.y-1) == 6 || board.getOccupant(n.x,n.y-1) == 8) && !board.isVisited(n.x, n.y-1)) {
                int act = n.act == 0 ? -2 : n.act;
                Node n1 = new Node(n.x, n.y-1, act);
                queue.add(n1);
            }
        }
        return 0;
    }

    /** Private class for keeping track of board tiles */
    private class Node {
        public int x;
        public int y;

        // -1 for -x, 1 for +x, -2 for -y, 2 for +y
        public int act;
        public int priority;

        public Node(int x, int y, int act) {
            this.x = x;
            this.y = y;
            this.act = act;
            int goalx = board.physicsToBoard(currentGoal.x);
            int goaly = board.physicsToBoard(currentGoal.y);
            this.priority = manDist(x,y,goalx,goaly);
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

        String status = json.get("status").asString();
        if (status.equals("sleep")) {
            state = FSMState.SLEEP;
        }
        if (status.equals("patrol")) {
            state = FSMState.PATROL;
            currentGoal = path[0];
            pathIndex = 0;
        }
    }

    /** Returns the Manhattan Distance of two points */
    private int manDist(int x0, int y0, int x1, int y1) {
        return Math.abs(x1 - x0) + Math.abs(y1 - y0);
    }
}
