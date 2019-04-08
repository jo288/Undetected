package edu.cornell.gdiac.b2lights;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.Obstacle;

import java.util.*;

public class AIController {
    /** Current Guard */
    private GuardModel guard;

    /** Object the guard is attempting to protect */
    private Obstacle item;

    /** Board */
    private Board board;

    /** Current State of Guard */
    private FSMState state;

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
        this.item = item;
    }

    /** Sets the tile the Guard is protecting*/
    public void setProtect(float x, float y) {
        currentGoal = new Vector2(board.physicsToBoard(x+guard.getWidth()/2), board.physicsToBoard(y+guard.getHeight()/2));
    }

    /** Main function to update the guard's velocity */
    public void update(){
        switch (state) {
            case SLEEP:
                // Do Nothing
                break;
            case PATROL:
                // Check if Guard is at patrol
                int goalx = board.physicsToBoard(currentGoal.x);
                int goaly = board.physicsToBoard(currentGoal.y);
                int guardx = board.physicsToBoard(guard.getX());
                int guardy = board.physicsToBoard(guard.getY());
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
                pathFind();
                board.resetTiles();
                break;
            case ALERT:
                // Set Goal tile to be the object, and find it
                if (item != null) {
                    int obx = board.physicsToBoard(item.getX());
                    int oby = board.physicsToBoard(item.getY());
                    board.setGoal(obx, oby);
                    currentGoal = new Vector2(obx, oby);
                } else {
                    board.setGoal((int) currentGoal.x, (int) currentGoal.y);
                }
                // System.out.println(currentGoal);
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
            guard.setMovement(50,0);
            guard.applyForce();
        } else if (i == 2) {
            guard.setDirection(0);
            guard.setMovement(0, 50);
            guard.applyForce();
        } else if (i == -1) {
            guard.setDirection((float) Math.PI/2);
            guard.setMovement(-50,0);
            guard.applyForce();
        } else if (i == -2){
            guard.setDirection((float) Math.PI);
            guard.setMovement(0,-50);
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
        PriorityQueue<Node> queue = new PriorityQueue<Node>(
                new Comparator<Node>(){
                    @Override
                    public int compare(Node n1, Node n2){
                        return n1.priority - n2.priority;
                    }
                }
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
            if (board.isSafeAt(n.x+1, n.y) && (board.getOccupant(n.x+1,n.y) == 0 || board.getOccupant(n.x+1,n.y) == 4 || board.getOccupant(n.x+1,n.y) == 3) && !board.isVisited(n.x+1, n.y))  {
                int act = n.act == 0 ? 1 : n.act;
                Node n1 = new Node(n.x+1, n.y, act);
                queue.add(n1);
            }
            if (board.isSafeAt(n.x-1, n.y) && (board.getOccupant(n.x-1,n.y) == 0 || board.getOccupant(n.x-1,n.y) == 4 || board.getOccupant(n.x-1,n.y) == 3) && !board.isVisited(n.x-1, n.y)) {
                int act = n.act == 0 ? -1 : n.act;
                Node n1 = new Node(n.x-1, n.y, act);
                queue.add(n1);
            }
            if (board.isSafeAt(n.x, n.y+1) && (board.getOccupant(n.x,n.y+1) == 0 || board.getOccupant(n.x,n.y+1) == 4 || board.getOccupant(n.x,n.y+1) == 3) && !board.isVisited(n.x, n.y+1)) {
                int act = n.act == 0 ? 2 : n.act;
                Node n1 = new Node(n.x, n.y+1, act);
                queue.add(n1);
            }
            if (board.isSafeAt(n.x, n.y-1) && (board.getOccupant(n.x,n.y-1) == 0 || board.getOccupant(n.x,n.y-1) == 4 || board.getOccupant(n.x,n.y-1) == 3) && !board.isVisited(n.x, n.y-1)) {
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
                path[i] = new Vector2(paths[j], paths[j + 1]);
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
