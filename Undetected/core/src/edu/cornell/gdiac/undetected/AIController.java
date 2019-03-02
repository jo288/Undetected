/*
 * AIController.java
 *
 * This class is an inplementation of InputController that uses AI and pathfinding
 * algorithms to determine the choice of input.
 *
 * NOTE: This is the file that you need to modify.  You should not need to 
 * modify any other files (though you may need to read Board.java heavily).
 *
 * Author: Walker M. White, Cristian Zaloj
 * Based on original AI Game Lab by Yi Xu and Don Holden, 2007
 * LibGDX version, 1/24/2015
 */
package edu.cornell.gdiac.undetected;

import java.util.*;

/** 
 * InputController corresponding to AI control.
 * 
 * REMEMBER: As an implementation of InputController you will have access to
 * the control code constants in that interface.  You will want to use them.
 */
public class AIController implements InputController {
	/**
	 * Enumeration to encode the finite state machine.
	 */
	private static enum FSMState {
		/** The ship just spawned */
		SPAWN,
		/** The ship is patrolling around without a target */
		WANDER,
		/** The ship has a target, but must get closer */
		CHASE,
		/** The ship has a target and is attacking it */
		ATTACK
	}

	// Constants for chase algorithms
	/** How close a target must be for us to chase it */
	private static final int CHASE_DIST  = 9;
	/** How close a target must be for us to attack it */
	private static final int ATTACK_DIST = 4;

	// Instance Attributes
	/** The ship being controlled by this AIController */
	private Ship ship;
	/** The game board; used for pathfinding */
	private Board board;
	/** The other ships; used to find targets */
	private ShipList fleet;
	/** The ship's current state in the FSM */
	private FSMState state;
	/** The target ship (to chase or attack). */
	private Ship target; 
	/** The ship's next action (may include firing). */
	private int move; // A ControlCode
	/** The number of ticks since we started this controller */
	private long ticks;
	
	// Custom fields for AI algorithms
	//#region ADD YOUR CODE: 

	//#endregion
	
	/**
	 * Creates an AIController for the ship with the given id.
	 *
	 * @param id The unique ship identifier
	 * @param board The game board (for pathfinding)
	 * @param ships The list of ships (for targetting)
	 */
	public AIController(int id, Board board, ShipList ships) {
		this.ship = ships.get(id);
		this.board = board;
		this.fleet = ships;
		
		state = FSMState.SPAWN;
		move  = CONTROL_NO_ACTION;
		ticks = 0;

		// Select an initial target
		target = null;
		selectTarget();
	}

	/**
	 * Returns the action selected by this InputController
	 *
	 * The returned int is a bit-vector of more than one possible input 
	 * option. This is why we do not use an enumeration of Control Codes;
	 * Java does not (nicely) provide bitwise operation support for enums. 
	 *
	 * This function tests the environment and uses the FSM to chose the next
	 * action of the ship. This function SHOULD NOT need to be modified.  It
	 * just contains code that drives the functions that you need to implement.
	 *
	 * @return the action selected by this InputController
	 */
	public int getAction() {
		// Increment the number of ticks.
		ticks++;

		// Do not need to rework ourselves every frame. Just every 10 ticks.
		if ((ship.getId() + ticks) % 10 == 0) {
			// Process the FSM
			changeStateIfApplicable();

			// Pathfinding
			markGoalTiles();
			move = getMoveAlongPathToGoalTile();
		}

		int action = move;

		// If we're attacking someone and we can shoot him now, then do so.
		if (state == FSMState.ATTACK && canShootTarget()) {
			action |= CONTROL_FIRE;
		}

		return action;
	}
	
	// FSM Code for Targeting (MODIFY ALL THE FOLLOWING METHODS)

	/**
	 * Change the state of the ship.
	 *
	 * A Finite State Machine (FSM) is just a collection of rules that,
	 * given a current state, and given certain observations about the
	 * environment, chooses a new state. For example, if we are currently
	 * in the ATTACK state, we may want to switch to the CHASE state if the
	 * target gets out of range.
	 */
	private void changeStateIfApplicable() {
		// Add initialization code as necessary
		//#region PUT YOUR CODE HERE

		int x = board.screenToBoard(ship.getX());
		int y = board.screenToBoard(ship.getY());
	
		//#endregion

		// Next state depends on current state.
		switch (state) {
		case SPAWN: // Do not pre-empt with FSMState in a case
			// Insert checks and spawning-to-??? transition code here
			//#region PUT YOUR CODE HERE

			// a little delay before the ship starts wandering
			if ((ship.getId()+ticks)%50==0)
			state=FSMState.WANDER;
			//#endregion
			break;

		case WANDER: // Do not pre-empt with FSMState in a case
			// Insert checks and moving-to-??? transition code here
			//#region PUT YOUR CODE HERE

			// a little delay so that the ship doesn't immediately target anything that comes its way
			if ((ship.getId()+ticks)%50!=0){
				break;
			}

			selectTarget();

			if (target!=null)
				state=FSMState.CHASE;

			//#endregion			
			break;

		case CHASE: // Do not pre-empt with FSMState in a case
			// insert checks and chasing-to-??? transition code here
			//#region PUT YOUR CODE HERE
			selectTarget();
			if (target==null) {
				state = FSMState.WANDER;
				break;
			}

			//every 50 ticks trigger the ship to go back to wander state with 50% chance
			if ((ship.getId()+ticks)%50==0&&Math.random()<0.5){
				state=FSMState.WANDER;
				break;
			}
			if (board.screenToBoard((float)checkDistance(target.getX(),target.getY(),ship.getX(),ship.getY()))<=ATTACK_DIST)
				state=FSMState.ATTACK;
			//#endregion			
			break;

		case ATTACK: // Do not pre-empt with FSMState in a case
			// insert checks and attacking-to-??? transition code here
			//#region PUT YOUR CODE HERE
			//#endregion
			selectTarget();
			if(target==null) {
				state = FSMState.WANDER;
				break;
			}

			//every 50 ticks trigger the ship to go back to wander state with 50% chance
			if((ship.getId()+ticks)%50==0&&Math.random()<0.5){
				state=FSMState.WANDER;
				break;
			}
			if (board.screenToBoard((float)checkDistance(target.getX(),target.getY(),ship.getX(),ship.getY()))>ATTACK_DIST)
				state=FSMState.CHASE;

			break;

		default:
			// Unknown or unhandled state, should never get here
			assert (false);
			state = FSMState.WANDER; // If debugging is off
			break;
		}
	}

	/**
	 * Acquire a target to attack (and put it in field target).
	 *
	 * Insert your checking and target selection code here. Note that this
	 * code does not need to reassign <c>target</c> every single time it is
	 * called. Like all other methods, make sure it works with any number
	 * of players (between 0 and 32 players will be checked). Also, it is a
	 * good idea to make sure the ship does not target itself or an
	 * already-fallen (e.g. inactive) ship.
	 */
	private void selectTarget() {
		//#region PUT YOUR CODE HERE
		//It chooses the closest ship as the target
		boolean selectTarget = false;

		Iterator<Ship> fi = fleet.iterator();
		int minD=1000;
		int temp;
		while (fi.hasNext()){
			Ship t = fi.next();
			if (t.isActive()&&t.getId()!=ship.getId()) {
				temp = board.screenToBoard((float)checkDistance(t.getX(),t.getY(),ship.getX(),ship.getY()));
				if (temp<=CHASE_DIST&&temp<=minD) {
					target = t;
					minD = temp;
					selectTarget = true;
				}
			}
		}

		if (!selectTarget)
			target = null;

		//#endregion			
	}

	/**
	 * Returns true if we can hit a target from here.
	 *
	 * Insert code to return true if a shot fired from the given (x,y) would
	 * be likely to hit the target. We can hit a target if it is in a straight
	 * line from this tile and within attack range. The implementation must take
	 * into consideration whether or not the source tile is a Power Tile.
	 *
	 * @param x The x-index of the source tile
	 * @param y The y-index of the source tile
	 *
	 * @return true if we can hit a target from here.
	 */
	private boolean canShootTargetFrom(int x, int y) {
		//#region PUT YOUR CODE HERE
		if (target==null) return false;

		int dx = Math.abs(x-board.screenToBoard(target.getX()));
		int dy = Math.abs(y-board.screenToBoard(target.getY()));

		if ((dx<=ATTACK_DIST && dy==0) || (dy<=ATTACK_DIST && dx==0))
			return true;

		if (board.isPowerTileAtScreen(target.getX(),target.getY())&&(dx==dy)&&(dx<=ATTACK_DIST))
			return true;

		return false;
		//#endregion			
	}

	/**
	 * Returns true if we can both fire and hit our target
	 *
	 * If we can fire now, and we could hit the target from where we are, 
	 * we should hit the target now.
	 *
	 * @return true if we can both fire and hit our target
	 */
	private boolean canShootTarget() {
		//#region PUT YOUR CODE HERE
		return ship.canFire() && canShootTargetFrom(board.screenToBoard(ship.getX()),board.screenToBoard(ship.getY()));
		//#endregion			
	}

	// Pathfinding Code (MODIFY ALL THE FOLLOWING METHODS)

	/** 
	 * Mark all desirable tiles to move to.
	 *
	 * This method implements pathfinding through the use of goal tiles.
	 * It searches for all desirable tiles to move to (there may be more than
	 * one), and marks each one as a goal. Then, the pathfinding method
	 * getMoveAlongPathToGoalTile() moves the ship towards the closest one.
	 *
	 * POSTCONDITION: There is guaranteed to be at least one goal tile
     * when completed.
     */
	private void markGoalTiles() {
		// Clear out previous pathfinding data.
		board.clearMarks(); 
		boolean setGoal = false; // Until we find a goal
		
		// Add initialization code as necessary
		//#region PUT YOUR CODE HERE

		int x = board.screenToBoard(ship.getX());
		int y = board.screenToBoard(ship.getY());
		int tx = (target==null?-1:board.screenToBoard(target.getX()));
		int ty = (target==null?-1:board.screenToBoard(target.getY()));
		
		//#endregion
		
		switch (state) {
		case SPAWN: // Do not pre-empt with FSMState in a case
			// insert code here to mark tiles (if any) that spawning ships
			// want to go to, and set setGoal to true if we marked any.
			// Ships in the spawning state will immediately move to another
			// state, so there is no need for goal tiles here.

			//#region PUT YOUR CODE HERE

			//NO GOAL TILES FOR SPAWN

			//#endregion
			break;

		case WANDER: // Do not pre-empt with FSMState in a case
			// Insert code to mark tiles that will cause us to move around;
			// set setGoal to true if we marked any tiles.
			// NOTE: this case must work even if the ship has no target
			// (and changeStateIfApplicable should make sure we are never
			// in a state that won't work at the time)
			
			//#region PUT YOUR CODE HERE

			switch ((int)(Math.random()*4)) {
				case 0:
					if (board.isSafeAt(x, y - 1) && board.inBounds(x, y - 1)) {
						board.setGoal(x, y - 1);
						setGoal = true;
					}
					break;
				case 1:
					if (board.isSafeAt(x, y + 1) && board.inBounds(x, y + 1)) {
						board.setGoal(x, y + 1);
						setGoal = true;
					}
					break;
				case 2:
					if (board.isSafeAt(x - 1, y) && board.inBounds(x - 1, y)) {
						board.setGoal(x - 1, y);
						setGoal = true;
					}
					break;
				case 3:
					if (board.isSafeAt(x + 1, y) && board.inBounds(x + 1, y)) {
						board.setGoal(x + 1, y);
						setGoal = true;
					}
					break;
				default:
					board.setGoal(x, y);
					break;
			}

			//#endregion
			break;

		case CHASE: // Do not pre-empt with FSMState in a case
			// Insert code to mark tiles that will cause us to chase the target;
			// set setGoal to true if we marked any tiles.
			
			//#region PUT YOUR CODE HERE
			board.setGoal(tx,ty);
			setGoal=true;

			//#endregion
			break;

		case ATTACK: // Do not pre-empt with FSMState in a case
			// Insert code here to mark tiles we can attack from, (see
			// canShootTargetFrom); set setGoal to true if we marked any tiles.

			//#region PUT YOUR CODE HERE

			//get to the nearest horizontally vertically aligned position or if neither one is close enough just move towards the target
			if (canShootTargetFrom(x,ty)&&board.isSafeAt(x,ty))
				board.setGoal(x,ty);
			if (canShootTargetFrom(tx,y)&&board.isSafeAt(tx,y))
				board.setGoal(tx,y);

			board.setGoal(tx,ty);

			setGoal=true;

			
			//#endregion
			break;
		}

		// If we have no goals, mark current position as a goal
		// so we do not spend time looking for nothing:
		if (!setGoal) {
			int sx = board.screenToBoard(ship.getX());
			int sy = board.screenToBoard(ship.getY());
			board.setGoal(sx, sy);
		}
	}
	
	/**
 	 * Returns a movement direction that moves towards a goal tile.
 	 *
 	 * This is one of the longest parts of the assignment. Implement
	 * breadth-first search (from 2110) to find the best goal tile
	 * to move to. However, just return the movement direction for
	 * the next step, not the entire path.
	 * 
	 * The value returned should be a control code.  See PlayerController
	 * for more information on how to use control codes.
	 *
 	 * @return a movement direction that moves towards a goal tile.
 	 */
	private int getMoveAlongPathToGoalTile() {		
		//#region PUT YOUR CODE HERE

		//CHECKED (tested with goal set to 0,0)

		//CHECK for WANDER state and delay, so that wandering ships don't turn around five times a second
		if (state==FSMState.WANDER&&((ship.getId()+ticks)%30!=0)) return move;

		//whether the path exists or not
		int movenum = -1;

		//north, south, east, west vectors
		int[] mx = {0, 0, -1, +1};
		int[] my = {-1, +1, 0, 0};

		//record what FIRST MOVE led it to this tile, -1:none 0:up 1:down 2:left 3:right
		int[][] firstmove = new int[board.getWidth()][board.getHeight()];

		LinkedList<Integer> xq = new LinkedList<Integer>();
		LinkedList<Integer> yq = new LinkedList<Integer>();

		//original ship coordinates
		int ox = board.screenToBoard(ship.getX());
		int oy = board.screenToBoard(ship.getY());

		//If origin is goal then don't do anything
		if (board.isGoal(ox,oy)) return CONTROL_NO_ACTION;

		//Initalize BFS: add the first four tiles (if valid) to the queue and check the first moves
		board.setVisited(ox,oy);
		firstmove[ox][oy] = -1;
		for (int i=0;i<4;i++) {
			int ttx = ox + mx[i];
			int tty = oy + my[i];

			//If in bounds, safe at, and not visited then queue it, set visited, and mark first move
			if (board.inBounds(ttx, tty) && board.isSafeAt(ttx, tty) && !board.isVisited(ttx, tty)) {
				xq.add(ttx);
				yq.add(tty);
				board.setVisited(ttx, tty);
				firstmove[ttx][tty] = i;
			}
		}

		//BFS LOOP
		while(xq.size()>0&&yq.size()>0){
			int tx = xq.remove();
			int ty = yq.remove();
			//if reached the goal
			if (board.isGoal(tx,ty)) {
				movenum = firstmove[tx][ty];
				break;
			}
			//if not, go through the near tiles
			for (int i=0;i<4;i++) {
				int ttx = tx + mx[i];
				int tty = ty + my[i];

				//If in bounds, safe at, and not visited then queue it, set visited, and mark first move
				if (board.inBounds(ttx, tty) && board.isSafeAt(ttx, tty) && !board.isVisited(ttx, tty)) {
					xq.add(ttx);
					yq.add(tty);
					board.setVisited(ttx, tty);
					firstmove[ttx][tty] = firstmove[tx][ty];
				}
			}
		}

		board.clearMarks();

		switch (movenum) {
			case 0:
				return CONTROL_MOVE_UP;
			case 1:
				return CONTROL_MOVE_DOWN;
			case 2:
				return CONTROL_MOVE_LEFT;
			case 3:
				return CONTROL_MOVE_RIGHT;
			default:
				return CONTROL_NO_ACTION;

		}

		//#endregion
	}

	// Add any auxiliary methods or data structures here
	//#region PUT YOUR CODE HERE

	/** Returns the distance between two positions in terms of screen coordinates
	 *
	 * @param x x coordinate of position1
	 * @param y y coordinate of position1
	 * @param x2 x coordinate of position2
	 * @param y2 y coordinate of position2
	 * @return the distance between (x,y) and (x2,y2)
	 */
	private double checkDistance (float x, float y, float x2, float y2){
		float dx = x-x2;
		float dy = y-y2;
		return Math.sqrt((double)(dx*dx + dy*dy));
	}


	//#endregion
}