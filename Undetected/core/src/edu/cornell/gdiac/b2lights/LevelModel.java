/*
 * LevelMode.java
 *
 * This stores all of the information to define a level for a top down game with light
 * and shadows.  As with Lab 2, it has an avatar, some walls, and an exit.  This model
 * supports JSON loading, and so the world is part of this object as well.  See the
 * JSON demo for more information.
 *
 * There are two major differences from JSON Demo.  First is the fixStep method.  This
 * ensures that the physics engine is really moving at the same rate as the visual 
 * framerate. You can usually survive without this addition.  However, when the physics
 * adjusts shadows, it is very important.  See this website for more information about
 * what is going on here.
 *
 * http://gafferongames.com/game-physics/fix-your-timestep/
 *
 * The second addition is the RayHandler.  This is an attachment to the physics world
 * for drawing shadows.  Technically, this is a view, and really should be part of 
 * GameCanvas.  However, in true graphics programmer garbage design, this is tightly 
 * coupled the the physics world and cannot be separated.  So we store it here and
 * make it part of the draw method.  This is the best of many bad options.
 *
 * TODO: Refactor this design to decouple the RayHandler as much as possible.  Next
 * year, maybe.
 *
 * Author: Walker M. White
 * Initial version, 3/12/2016
 */
package edu.cornell.gdiac.b2lights;

import box2dLight.*;

import java.security.Guard;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.physics.box2d.*;

// import com.sun.media.sound.AiffFileReader;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.physics.lights.*;
import edu.cornell.gdiac.physics.obstacle.*;

/**
 * Represents a single level in our game
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.  To reset a level, dispose it and reread the JSON.
 *
 * The level contains its own Box2d World, as the World settings are defined by the
 * JSON file.  There is generally no controller code in this class, except for the
 * update method for moving ahead one timestep.  All of the other methods are getters 
 * and setters.  The getters allow the GameController class to modify the level elements.
 */
public class LevelModel {

	/** Number of velocity iterations for the constrain solvers */
	public static final int WORLD_VELOC = 6;
	/** Number of position iterations for the constrain solvers */
	public static final int WORLD_POSIT = 2;
	/** Exclude bits for raycasting */
	public static final short LIGHT_COLLIDEBITS = (short)0x1000;
	public static final short LIGHT_MASKBITS = (short)0xefaf;
	public static final short CAMERA_LIGHT_COLLIDEBITS = (short)0x2000;
	public static final short CAMERA_LIGHT_MASKBITS = (short)0xefa7;

	// Physics objects for the game
	/** Reference to the character avatar */
	private DudeModel avatar;
	/** Reference to the goalDoor (for collision detection) */
	private ExitModel goalDoor;
	/** Reference to the objective (for collision detection) */
	private ObjectiveModel objective;
	/** Reference to all the lasers */
	private ArrayList<Laser> lasers;
	/** Reference to all the doors */
	private ArrayList<DoorModel> doors;
	/** Reference to all the switches */
	private ArrayList<SwitchModel> switches;
	private ArrayList<CameraModel> cameras;
	/** Reference to all the guards (for line-of-sight checks) */
	private ArrayList<GuardModel> guards;
	/** Guard AI */
	private ArrayList<AIController> controls;
	private ArrayList<MoveableBox> boxes;

	/** Whether or not the level is in debug more (showing off physics) */	
	private boolean debug;
	
	/** All the objects in the world. */
	protected PooledList<Obstacle> objects  = new PooledList<Obstacle>();
	/** Objects to be destroyed*/
	protected LinkedList<Obstacle> destroyed = new LinkedList<Obstacle>();
	/** Objects temporarily disabled*/
	protected LinkedList<Obstacle> disabled = new LinkedList<Obstacle>();
	/** Objects to be enabled*/
	protected LinkedList<Obstacle> enabled = new LinkedList<Obstacle>();

	// LET THE TIGHT COUPLING BEGIN
	/** The Box2D world */
	protected World world;
	/** The boundary of the world */
	protected Rectangle bounds;
	/** The world scale */
	protected Vector2 scale;
	/** The Board */
	protected Board board;
	/** Alarm */
	protected Alarm alarm;
	/** The camera defining the RayHandler view; scale is in physics coordinates */
	protected OrthographicCamera raycamera;
	/** The rayhandler for storing lights, and drawing them (SIGH) */
	protected RayHandler rayhandler;
	/** All of the active lights that we loaded from the JSON file */
	private Array<ConeSource> lights = new Array<ConeSource>();
	/** The current light source being used.  If -1, there are no shadows */
	private int activeLight;
	
	// TO FIX THE TIMESTEP
	/** The maximum frames per second setting for this level */
	protected int maxFPS;
	/** The minimum frames per second setting for this level */
	protected int minFPS;
	/** The amount of time in to cover a single animation frame */
	protected float timeStep;
	/** The maximum number of steps allowed before moving physics forward */
	protected float maxSteps;
	/** The maximum amount of time allowed in a frame */
	protected float maxTimePerFrame;
	/** The amount of time that has passed without updating the frame */
	protected float physicsTimeLeft;

	/**
	 * Returns the bounding rectangle for the physics world
	 * 
	 * The size of the rectangle is in physics, coordinates, not screen coordinates
	 *
	 * @return the bounding rectangle for the physics world
	 */
	public Rectangle getBounds() {
		return bounds;
	}

	/**
	 * Returns the scaling factor to convert physics coordinates to screen coordinates
	 *
	 * @return the scaling factor to convert physics coordinates to screen coordinates
	 */
	public Vector2 getScale() {
		return scale;
	}

	/**
	 * Returns a reference to the Box2D World
	 *
	 * @return a reference to the Box2D World
	 */
	public World getWorld() {
		return world;
	}

	/**
	 * Returns a reference to the Board
	 *
	 * @return a reference to the Board
	 */
	public Board getBoard() {
		return board;
	}

	public Alarm getAlarm(){ return alarm;}
	
	/**
	 * Returns a reference to the lighting rayhandler
	 *
	 * @return a reference to the lighting rayhandler
	 */
	public RayHandler getRayHandler() {
		return rayhandler;
	}
	
	/**
	 * Returns a reference to the player avatar
	 *
	 * @return a reference to the player avatar
	 */
	public DudeModel getAvatar() {
		return avatar;
	}

	/**
	 * Returns a reference to all the guards
	 */
	public ArrayList<GuardModel> getGuards() { return guards; }

	/**
	 * Returns a reference to all the guards
	 */
	public ArrayList<DoorModel> getDoors() { return doors; }

	/**
	 * Returns a reference to all the guards
	 */
	public ArrayList<SwitchModel> getSwtiches() { return switches; }
	public ArrayList<CameraModel> getCameras() { return cameras;}

    /**
     * Returns a reference to all AI
     *
     */
	public ArrayList<AIController> getControl() {return controls; }

	/**
	 * Returns a reference to the exit door
	 * 
	 * @return a reference to the exit door
	 */
	public ExitModel getExit() {
		return goalDoor;
	}

	/**
	 * Returns a reference to the objective
	 *
	 * @return a reference to the objective
	 */
	public ObjectiveModel getObjective() {
		return objective;
	}

	/**
	 * Returns an array of all the lasers
	 *
	 * @return an array of the lasers
	 */
	public ArrayList<Laser> getLasers() {
		return lasers;
	}

	public boolean alarmStoppedPlaying() {
		for (Laser l : lasers) {
			if (l.stillPlaying()) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns a reference to a box
	 *
	 * @return a reference to a box
	 */
	public ArrayList<MoveableBox> getBoxes() {return boxes;}

	/**
	 * Returns whether this level is currently in debug node
	 *
	 * If the level is in debug mode, then the physics bodies will all be drawn as
	 * wireframes onscreen
	 *
	 * @return whether this level is currently in debug node
	 */	
	public boolean getDebug() {
		return debug;
	}
	
	/**
	 * Sets whether this level is currently in debug node
	 *
	 * If the level is in debug mode, then the physics bodies will all be drawn as
	 * wireframes onscreen
	 *
	 * @param value	whether this level is currently in debug node
	 */	
	public void setDebug(boolean value) {
		debug = value;
	}
	
	/**
	 * Returns the maximum FPS supported by this level
	 *
	 * This value is used by the rayhandler to fix the physics timestep.
	 *
	 * @return the maximum FPS supported by this level
	 */
	public int getMaxFPS() {
		return maxFPS;
	}
	
	/**
	 * Sets the maximum FPS supported by this level
	 *
	 * This value is used by the rayhandler to fix the physics timestep.
	 *
	 * @param value the maximum FPS supported by this level
	 */
	public void setMaxFPS(int value) {
		maxFPS = value;
	}

	/**
	 * Returns the minimum FPS supported by this level
	 *
	 * This value is used by the rayhandler to fix the physics timestep.
	 *
	 * @return the minimum FPS supported by this level
	 */
	public int getMinFPS() {
		return minFPS;
	}

	/**
	 * Sets the minimum FPS supported by this level
	 *
	 * This value is used by the rayhandler to fix the physics timestep.
	 *
	 * @param value the minimum FPS supported by this level
	 */
	public void setMinFPS(int value) {
		minFPS = value;
	}

	/**
	 * Creates a new LevelModel
	 * 
	 * The level is empty and there is no active physics world.  You must read
	 * the JSON file to initialize the level
	 */
	public LevelModel() {
		world  = null;
		bounds = new Rectangle(0,0,1,1);
		scale = new Vector2(1,1);
		debug  = false;
		board = null;
		alarm = new Alarm();
	}
	
	/**
	 * Lays out the game geography from the given JSON file
	 *
	 * @param levelFormat	the JSON tree defining the level
	 */
	public void populate(JsonValue levelFormat) {
//		float[] pSize = levelFormat.get("physicsSize").asFloatArray();
		int[] gSize = levelFormat.get("graphicSize").asIntArray();
		int[] bSize = levelFormat.get("boardSize").asIntArray();
		int tSize = levelFormat.get("tileSize").asInt();
		float[] pSize = {((float)gSize[0])/(float)tSize,((float)gSize[1])/(float)tSize};
		BoxObstacle.setTileOffset(tSize/2f);

		alarm = new Alarm();

		world = new World(Vector2.Zero,false);
		board = new Board(bSize[0],bSize[1],tSize);
		board.setTileTexture(JsonAssetManager.getInstance().getEntry("floors", TextureRegion.class));
//		int[] invalidTiles = levelFormat.get("invalidTiles").asIntArray();
//		board.setInvalidTiles(invalidTiles);
		board.setTiles(levelFormat.get("tiles").asIntArray());
		bounds = new Rectangle(0,0,pSize[0],pSize[1]);
		scale.x = gSize[0]/pSize[0];
		scale.y = gSize[1]/pSize[1];

		
		// Compute the FPS
		int[] fps = levelFormat.get("fpsRange").asIntArray();
		maxFPS = fps[1]; minFPS = fps[0];
		timeStep = 1.0f/maxFPS;
		maxSteps = 1.0f + maxFPS/minFPS;
		maxTimePerFrame = timeStep*maxSteps;


		// Create the lighting if appropriate
		if (levelFormat.has("lighting")) {
			initLighting(levelFormat.get("lighting"));
		}
		//createPointLights(levelFormat.get("pointlights"));
		createConeLights(levelFormat.get("lights"));
		
		// Add level goal
		if (levelFormat.has("exit")) {
			goalDoor = new ExitModel();
			goalDoor.initialize(levelFormat.get("exit"));
			if (goalDoor.getTexture().getRegionWidth() < tSize)
				goalDoor.setWidth(goalDoor.getTexture().getRegionWidth() / scale.x);
			if (goalDoor.getTexture().getRegionHeight() < tSize)
				goalDoor.setHeight(goalDoor.getTexture().getRegionHeight() / scale.y);
			goalDoor.setDrawScale(scale);
			//Check for goal door direction
			int tx = board.physicsToBoard(goalDoor.getX());
			int ty = board.physicsToBoard(goalDoor.getY());
			if (!board.isSafeAt(tx+1,ty)){
				goalDoor.setExitDirection(-(float)Math.PI/2);
			}else if (!board.isSafeAt(tx-1,ty)){
				goalDoor.setExitDirection((float)Math.PI/2);
			}else if (!board.isSafeAt(tx,ty+1)){
				goalDoor.setExitDirection(0);
			}else{
				goalDoor.setExitDirection((float)Math.PI);
			}
			activate(goalDoor);
		}

		JsonValue bounds = levelFormat.get("exteriorwall");
		ExteriorWall ew = new ExteriorWall();
		ew.initialize(bounds);
		ew.setDrawScale(scale);
		for (Obstacle o: ew.bodies){
			activate(o);
		}

		JsonValue walls = levelFormat.get("interiorwall");
		InteriorWall iw = new InteriorWall();
		iw.initialize(walls);
		iw.setDrawScale(scale);
		for (Obstacle o: iw.bodies){
			activate(o);
		}

		// Create the dude and attach light sources
	    avatar = new DudeModel();
	    JsonValue avdata = levelFormat.get("avatar");
	    avatar.initialize(avdata);
		if (avatar.getTexture().getRegionWidth()<tSize)
			avatar.setWidth(avatar.getTexture().getRegionWidth()/scale.x);
	    avatar.setDrawScale(scale);
		activate(avatar);

		//Guard List
		guards = new ArrayList<GuardModel>();
		//AIController List
		controls = new ArrayList<AIController>();

		GuardModel guard;
		AIController ai;
		JsonValue guardData = levelFormat.getChild("guards");
		int ii = 0;
		while (guardData!=null){
			guard = new GuardModel();
			guard.initialize(guardData);
			if (guard.getTexture().getRegionWidth()<tSize)
				guard.setWidth(guard.getTexture().getRegionWidth()/scale.x);
			if (guard.getTexture().getRegionHeight()<tSize)
				guard.setHeight(guard.getTexture().getRegionHeight()/scale.y);
			guard.setDrawScale(scale);
			activate(guard);
			guard.addLight(lights.get(guardData.get("lightIndex").asInt()));
			attachLights(guard, lights.get(guardData.get("lightIndex").asInt()));

			// Testing AIController
			ai = new AIController(board, guard);
			controls.add(ai);
			ai.initialize(guardData);
			this.guards.add(guard);

			guardData = guardData.next();
		}


		JsonValue boxdata = levelFormat.getChild("boxes");
		boxes = new ArrayList<MoveableBox>();
		while (boxdata!=null){
			MoveableBox box = new MoveableBox();
			boxes.add(box);
			box.initialize(boxdata);
			if (box.getTexture().getRegionWidth()<tSize)
				box.setWidth(box.getTexture().getRegionWidth()/scale.x);
			if (box.getTexture().getRegionHeight()<tSize)
				box.setHeight(box.getTexture().getRegionHeight()/scale.y);
			box.setDrawScale(scale);
			activate(box);
			boxdata = boxdata.next();
		}

		JsonValue decodata = levelFormat.getChild("decoratives");
		while (decodata!=null){
			DecorativeModel deco = new DecorativeModel();
			deco.initialize(decodata);
			if (deco.getTexture().getRegionWidth()<tSize)
				deco.setWidth(deco.getTexture().getRegionWidth()/scale.x);
			if (deco.getTexture().getRegionHeight()<tSize)
				deco.setHeight(deco.getTexture().getRegionHeight()/scale.y);
			deco.setDrawScale(scale);
			activate(deco);
			decodata = decodata.next();
		}

        cameras = new ArrayList<CameraModel>();
        JsonValue cameraData = levelFormat.getChild("cameras");
        while (cameraData!=null){
            CameraModel camera = new CameraModel();
            camera.initialize(cameraData);
            camera.setDrawScale(scale);
            activate(camera);
			ConeSource camera_light = lights.get(cameraData.get("lightIndex").asInt());
			Filter f = new Filter();
			f.categoryBits = CAMERA_LIGHT_COLLIDEBITS;
			f.maskBits = CAMERA_LIGHT_MASKBITS;
			camera_light.setContactFilter(f);
            camera.addLight(camera_light);
            attachLights(camera, lights.get(cameraData.get("lightIndex").asInt()));
            this.cameras.add(camera);
            //switches.get(0).addCamera(camera);
            cameraData = cameraData.next();
        }

		HashMap<String, Laser> laserMap = new HashMap<>();
		lasers = new ArrayList<Laser>();
		JsonValue laserdata = levelFormat.getChild("lasers");
		while (laserdata!=null){
			String laserName = laserdata.get("name").asString();
			Laser l = new Laser();
			laserMap.put(laserName, l);
			l.setTimeToLive(laserdata.get("timetolive").asInt());
			l.setLiveTimeReference();
			lasers.add(l);
			l.initialize(laserdata);
			if (l.getTexture().getRegionWidth()<tSize)
				l.setWidth(l.getTexture().getRegionWidth()/scale.x);
			l.setDrawScale(scale);
			activate(l);
			l.start();
			laserdata = laserdata.next();
		}

		HashMap<String, DoorModel> doorMap = new HashMap<>();
		doors = new ArrayList<DoorModel>();
		JsonValue doordata = levelFormat.getChild("doors");
		int[] doorPositions;
		while (doordata!=null){
			String doorName = doordata.get("name").asString();
			DoorModel door = new DoorModel();
			doorMap.put(doorName, door);
			doors.add(door);
			door.initialize(doordata);
			if (door.getTexture().getRegionWidth()<tSize)
				door.setWidth(door.getTexture().getRegionWidth()/scale.x);
			if (door.getTexture().getRegionHeight()<tSize)
				door.setHeight(door.getTexture().getRegionHeight()/scale.y);
			door.setDrawScale(scale);
			activate(door);
			doordata = doordata.next();
		}

		switches = new ArrayList<SwitchModel>();
		JsonValue switchdata = levelFormat.getChild("switches");
		String[] switchDoor;
		String[] switchLaser;
		int[] switchPositions;
		while (switchdata!=null){
			switchPositions = switchdata.get("pos").asIntArray();
			switchDoor = switchdata.get("doors").asStringArray();
			switchLaser = switchdata.get("lasers").asStringArray();
			SwitchModel switchi = new SwitchModel();
			switchi.setSwitch(switchdata.get("switched").asBoolean());
			switches.add(switchi);
			switchi.initialize(switchdata);
			if (switchi.getTexture().getRegionWidth()<tSize)
				switchi.setWidth(switchi.getTexture().getRegionWidth()/scale.x);
			if (switchi.getTexture().getRegionHeight()<tSize)
				switchi.setHeight(switchi.getTexture().getRegionHeight()/scale.y);
			switchi.setPosition(switchPositions[0]+0.5f, switchPositions[1]+0.5f);
			switchi.setDrawScale(scale);
			for (int i = 0; i < switchDoor.length; i++) {
				switchi.addDoor(doorMap.get(switchDoor[i]));
			}
			for (int i = 0; i < switchLaser.length; i++) {
				switchi.addLaser(laserMap.get(switchLaser[i]));
			}
			activate(switchi);
			switchdata = switchdata.next();
		}

		if (levelFormat.has("objective")) {
			String[] objDoors;
			String[] objLasers;
			objective = new ObjectiveModel();
			objDoors = levelFormat.get("objective").get("doors").asStringArray();
			objLasers = levelFormat.get("objective").get("lasers").asStringArray();
			objective.initialize(levelFormat.get("objective"));
			if (objective.getTexture().getRegionWidth() < tSize)
				objective.setWidth(objective.getTexture().getRegionWidth() / scale.x);
			if (objective.getTexture().getRegionHeight() < tSize)
				objective.setHeight(objective.getTexture().getRegionHeight() / scale.y);
			objective.setDrawScale(scale);
			for (int i = 0; i < objDoors.length; i++) {
				objective.addDoor(doorMap.get(objDoors[i]));
			}
			for (int i = 0; i < objLasers.length; i++) {
				objective.addLaser(laserMap.get(objLasers[i]));
			}
			activate(objective);
		}
	}

	public void placeBox(DudeModel player) {
		float dir = player.getDirection();
		MoveableBox b = player.getBoxHeld();
		float bx = (board.getTileSize() * board.screenToBoard(player.getX()-(float)Math.sin(dir)) + 0.5f);
		float by = (board.getTileSize() * board.screenToBoard(player.getY()+(float)Math.cos(dir)) + 0.5f);
		int o = board.getOccupantAt(board.physicsToBoard(bx), board.physicsToBoard(by));
		if (o==0||o==3||o==4) {
			b.setPosition(bx, by);
			b.playDrop();
			avatar.dropBox();
			queueEnabled(b);
		}
    }

    public boolean canPlaceBoxAt(){
		float dir = avatar.getDirection();
		float bx = (board.getTileSize() * board.screenToBoard(avatar.getX()-(float)Math.sin(dir)) + 0.5f);
		float by = (board.getTileSize() * board.screenToBoard(avatar.getY()+(float)Math.cos(dir)) + 0.5f);
		int o = board.getOccupantAt(board.physicsToBoard(bx), board.physicsToBoard(by));
		if (o==0||o==3||o==4) {
			return true;
		}
		return false;
	}

    public Vector2 boxGhost() {
		float dir = avatar.getDirection();
		float bx = (board.getTileSize() * board.screenToBoard(avatar.getX()-(float)Math.sin(dir)) + 0.5f);
		float by = (board.getTileSize() * board.screenToBoard(avatar.getY()+(float)Math.cos(dir)) + 0.5f);
		return new Vector2(bx, by);
	}
	
	/**
	 * Creates the ambient lighting for the level
	 *
	 * This is the amount of lighting that the level has without any light sources.
	 * However, if activeLight is -1, this will be ignored and the level will be
	 * completely visible.
	 *
	 * @param  light	the JSON tree defining the light
	 */
	private void initLighting(JsonValue light) {
//		raycamera = new OrthographicCamera(bounds.width,bounds.height);
		//Just hardcoded the constants
		raycamera = new OrthographicCamera(800f/32f,600f/32f);
		raycamera.position.set(bounds.width/2.0f, bounds.height/2.0f, 0);
		raycamera.update();

		RayHandler.setGammaCorrection(light.getBoolean("gamma"));
		RayHandler.useDiffuseLight(light.getBoolean("diffuse"));
		rayhandler = new RayHandler(world, Gdx.graphics.getWidth(), Gdx.graphics.getWidth());
		rayhandler.setCombinedMatrix(raycamera);
			
		float[] color = light.get("color").asFloatArray();
		rayhandler.setAmbientLight(color[0], color[1], color[2], color[3]);
		int blur = light.getInt("blur");
		rayhandler.setBlur(blur > 0);
		rayhandler.setBlurNum(blur);
	}

	/**
	 * Creates the points lights for the level
	 *
	 * Point lights show light in all direction.  We treat them differently from cone
	 * lights because they have different defining attributes.  However, all lights are
	 * added to the lights array.  This allows us to cycle through both the point lights
	 * and the cone lights with activateNextLight().
	 *
	 * All lights are deactivated initially.  We only want one active light at a time.
	 *
	 * @param  json	the JSON tree defining the list of point lights
	 */
	/*private void createPointLights(JsonValue json) {
		JsonValue light = json.child();
	    while (light != null) {
	    	float[] color = light.get("color").asFloatArray();
	    	float[] pos = light.get("pos").asFloatArray();
	    	float dist  = light.getFloat("distance");
	    	int rays = light.getInt("rays");

			PointSource point = new PointSource(rayhandler, rays, Color.WHITE, dist, pos[0], pos[1]);
			point.setColor(color[0],color[1],color[2],color[3]);
			point.setSoft(light.getBoolean("soft"));
			
			// Create a filter to exclude see through items
			Filter f = new Filter();
			f.maskBits = bitStringToComplement(light.getString("excludeBits"));
			point.setContactFilter(f);
			point.setActive(false); // TURN ON LATER
			lights.add(point);
	        light = light.next();
	    }
	}*/

	/**
	 * Creates the cone lights for the level
	 *
	 * Cone lights show light in a cone with a direction.  We treat them differently from 
	 * point lights because they have different defining attributes.  However, all lights
	 * are added to the lights array.  This allows us to cycle through both the point 
	 * lights and the cone lights with activateNextLight().
	 *
	 * All lights are deactivated initially.  We only want one active light at a time.
	 *
	 * @param  json	the JSON tree defining the list of point lights
	 */
	private void createConeLights(JsonValue json) {
		JsonValue light = json.child();
	    while (light != null) {
	    	float[] color = light.get("color").asFloatArray();
			//float[] pos = light.get("pos").asFloatArray();
			float[] pos = {0,0};
	    	float dist  = light.getFloat("distance");
	    	//float face  = light.getFloat("facing");
			float face = 90;
	    	float angle = light.getFloat("angle");
	    	//int rays = light.getInt("rays");
			int rays = 512;
	    	
			ConeSource cone = new ConeSource(rayhandler, rays, Color.WHITE, dist, pos[0], pos[1], face, angle);
			cone.setColor(color[0],color[1],color[2],color[3]);
			//cone.setSoft(light.getBoolean("soft"));
			cone.setSoft(false);
			
			// Create a filter to exclude see through items
			Filter f = new Filter();
			f.categoryBits = LIGHT_COLLIDEBITS;
			f.maskBits = LIGHT_MASKBITS;
			System.out.println("Dude mask bits "+f.maskBits);
			cone.setContactFilter(f);
			cone.setActive(false); // TURN ON LATER
			lights.add(cone);
	        light = light.next();
	    }
	}
	
	/**
	 * Attaches all lights to the avatar.
	 * 
	 * Lights are offset form the center of the avatar according to the initial position.
	 * By default, a light ignores the body.  This means that putting the light inside
	 * of these bodies fixtures will not block the light.  However, if a light source is
	 * offset outside of the bodies fixtures, then they will cast a shadow.
	 *
	 * The activeLight is set to be the first element of lights, assuming it is not empty.
	 */
	public void attachLights(GuardModel guard, LightSource light) {
		light.setActive(true);
		light.attachToBody(guard.getBody(), light.getX(), light.getY(), light.getDirection());
		if (lights.size > 0) {
			activeLight = 0;
			lights.get(0).setActive(true);
		} else {
			activeLight = -1;
		}
	}
	public void attachLights(CameraModel camera, LightSource light) {
		light.setActive(true);
		light.attachToBody(camera.getBody(), light.getX(), light.getY(), light.getDirection());
		if (lights.size > 0) {
			activeLight = 0;
			lights.get(0).setActive(true);
		} else {
			activeLight = -1;
		}
	}

	/**
	 * Activates the next light in the light list.
	 *
	 * If activeLight is at the end of the list, it sets the value to -1, disabling
	 * all shadows.  If activeLight is -1, it activates the first light in the list.
	 */
	public void activateNextLight() {
		if (activeLight != -1) {
			lights.get(activeLight).setActive(false);
		}
		activeLight++;
		if (activeLight >= lights.size) {
			activeLight = -1;
		} else {
			lights.get(activeLight).setActive(true);
		}
	}

	/**
	 * Activates the previous light in the light list.
	 *
	 * If activeLight is at the start of the list, it sets the value to -1, disabling
	 * all shadows.  If activeLight is -1, it activates the last light in the list.
	 */
	public void activatePrevLight() {
		if (activeLight != -1) {
			lights.get(activeLight).setActive(false);
		}
		activeLight--;
		if (activeLight < -1) {
			activeLight = lights.size-1;
		} else if (activeLight > -1) {
			lights.get(activeLight).setActive(true);
		}		
	}

	/**
	 * Disposes of all resources for this model.
	 *
	 * Because of all the heavy weight physics stuff, this method is absolutely 
	 * necessary whenever we reset a level.
	 */
	public void dispose() {
		for(LightSource light : lights) {
			light.remove();
		}
		lights.clear();

		if (rayhandler != null) {
			rayhandler.dispose();
			rayhandler = null;
		}
		
		for(Obstacle obj : objects) {
			if (avatar.getHasBox()) {
				avatar.dropBox();
				objects.add(avatar.getLastBoxHeld());
			}
//			if (obj.getClass().equals(Laser.class)) {
//
//			}
			obj.deactivatePhysics(world);
			obj.dispose();
		}
		objects.clear();
		if (world != null) {
			world.dispose();
			world = null;
		}
	}

	/**
	 * Immediately adds the object to the physics world
	 *
	 * param obj The object to add
	 */
	protected void activate(Obstacle obj) {
		assert inBounds(obj) : "Object is not in bounds";
		objects.add(obj);
		obj.activatePhysics(world);
	}

	/**
	 * Queue the object to the to-be-destroyed object queue
	 */
	protected void queueDestroyed(Obstacle obj){
        if (!destroyed.contains(obj))
	        destroyed.add(obj);
	}

	/**
	 * Queue the object to the disabled object queue
	 */
	protected void queueDisabled(Obstacle obj){
		if (!disabled.contains(obj))
			disabled.add(obj);
	}

	/**
	 * Queue the object to the disabled object queue
	 */
	protected void queueEnabled(Obstacle obj){
		if (disabled.contains(obj))
			disabled.remove(obj);
		if (!enabled.contains(obj))
			enabled.add(obj);
	}

    /**
     * Destroy the objects in the to-be-destroyed queue
     */
    protected void destroyObjects(){
        for (Obstacle o : destroyed){
//            if(o.getBody()!=null)
//                world.destroyBody(o.getBody());
            o.deactivatePhysics(world);
            o.dispose();
            objects.remove(o);
        }
    }

	/**
	 * Disable the objects in the disabled queue
	 */
	protected void disableObjects(){
		for (Obstacle o : disabled){
			o.setActive(false);
			objects.remove(o);
		}
	}

	/**
	 * Enable the objects in the enabled queue
	 */
	protected void enableObjects(){
		for (Obstacle o : enabled) {
			o.setActive(true);
			objects.add(o);
			enabled.remove(o);
		}
	}

	/**
	 * Returns true if the object is in bounds.
	 *
	 * This assertion is useful for debugging the physics.
	 *
	 * @param obj The object to check.
	 *
	 * @return true if the object is in bounds.
	 */
	private boolean inBounds(Obstacle obj) {
		boolean horiz = (bounds.x <= obj.getX() && obj.getX() <= bounds.x+bounds.width);
		boolean vert  = (bounds.y <= obj.getY() && obj.getY() <= bounds.y+bounds.height);
		return horiz && vert;
	}

	/**
	 * Add object information to the board
	 *
	 */
	private void updateBoard(){
		board.resetOccupants();
		for(Obstacle o: objects){
			if(o instanceof ExteriorWall.WallBlock){
				board.setOccupiedTiles(board.physicsToBoard(o.getX()),
						board.physicsToBoard(o.getY()),1);
				board.setOccupiedTiles(board.physicsToBoard(o.getX()),
						board.physicsToBoard(o.getY())+1,1);
			}
			if(o instanceof InteriorWall.WallBlock){
				board.setOccupiedTiles(board.physicsToBoard(o.getX()),
						board.physicsToBoard(o.getY()),1);
			}
			if(o instanceof DecorativeModel){
				board.setOccupiedTiles(board.physicsToBoard(o.getX()),
						board.physicsToBoard(o.getY()),1);
			}
			if(o instanceof DudeModel){
				board.setOccupiedTiles(board.physicsToBoard(o.getX()+((DudeModel) o).getWidth()/2),
						board.physicsToBoard(o.getY()+((DudeModel) o).getHeight()/2),3);
			}
			if(o instanceof MoveableBox){
				board.setOccupiedTiles(board.physicsToBoard(o.getX()),board.physicsToBoard(o.getY()),5);
			}
			if(o instanceof DoorModel){
				if (((DoorModel) o).getOpen()) {
					board.setOccupiedTiles(board.physicsToBoard(o.getX()), board.physicsToBoard(o.getY()), 6);
				} else {
					board.setOccupiedTiles(board.physicsToBoard(o.getX()), board.physicsToBoard(o.getY()), 7);
				}
			}
			if(o instanceof SwitchModel){
				board.setOccupiedTiles(board.physicsToBoard(o.getX()),board.physicsToBoard(o.getY()),8);
			}
			if(o instanceof Laser){
				if (((Laser) o).isHorizontal()){
					for (int i=(int)o.getX();i<(int)o.getX()+((Laser) o).getHeight();i++){
						board.setOccupiedTiles(board.physicsToBoard(i), board.physicsToBoard(o.getY()), 4);
					}
				}else {
					for (int i = (int) o.getY(); i < (int) (o.getY() + ((Laser) o).getHeight()); i++) {
						board.setOccupiedTiles(board.physicsToBoard(o.getX()), board.physicsToBoard(i), 4);
					}
				}
			}
			if(o instanceof GuardModel){
				board.setOccupiedTiles(board.physicsToBoard(o.getX()+((GuardModel) o).getWidth()/2),
						board.physicsToBoard(o.getY()+((GuardModel) o).getHeight()/2),2);
			}
			if (o instanceof  ExitModel) {
				board.setOccupiedTiles(board.physicsToBoard(o.getX()),board.physicsToBoard(o.getY()),9);
				board.setOccupiedTiles(board.physicsToBoard(o.getX()+1),board.physicsToBoard(o.getY()),9);
			}
		}
	}
	
	/**
	 * Updates all of the models in the level.
	 *
	 * This is borderline controller functionality.  However, we have to do this because
	 * of how tightly coupled everything is.
	 *
	 * @param dt the time passed since the last frame
	 */
	public boolean update(float dt) {
		if (fixedStep(dt)) {
			if (rayhandler != null) {
				rayhandler.update();
			}
			avatar.update(dt);

			for (Obstacle o: objects){
				if(o instanceof DecorativeModel){
					o.update(dt);
				}
			}

			for (SwitchModel sw : switches) {
				sw.update(dt);
			}

			for(DoorModel door: doors){
				door.update(dt);
			}

			if(goalDoor!=null)
				goalDoor.update(dt);
			for(Laser l: lasers){
				l.update(dt);
			}
			destroyObjects();
			disableObjects();
			enableObjects();
			updateBoard();

            // System.out.println(board.isSafeAt(board.screenToBoard(avatar.getX()), board.screenToBoard(avatar.getY())));
			//Test for displaying board states
//			board.update();

			for (AIController ai : controls) {
				ai.update();
//				if (ai.getCurrentGoal() != null) {
//					System.out.println();
//					System.out.println(ai.getCurrentGoal().toString());
//					System.out.println();
//				}
			}

			for (GuardModel g : guards) {
				g.update(dt);
			}

			for(CameraModel camera: cameras){
				camera.update();
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Fixes the physics frame rate to be in sync with the animation framerate
	 *
	 * http://gafferongames.com/game-physics/fix-your-timestep/
	 *
	 * @param dt the time passed since the last frame
	 */
	private boolean fixedStep(float dt) {
		if (world == null) return false;
		
		physicsTimeLeft += dt;
		if (physicsTimeLeft > maxTimePerFrame) {
			physicsTimeLeft = maxTimePerFrame;
		}
		
		boolean stepped = false;
		while (physicsTimeLeft >= timeStep) {
			world.step(timeStep, WORLD_VELOC, WORLD_POSIT);
			physicsTimeLeft -= timeStep;
			stepped = true;
		}
		return stepped;
	}
	
	/**
	 * Draws the level to the given game canvas
	 *
	 * If debug mode is true, it will outline all physics bodies as wireframes. Otherwise
	 * it will only draw the sprite representations.
	 *
	 * @param canvas	the drawing context
	 */
	public void draw(ObstacleCanvas canvas) {
		boolean avatarOnTop = false;

		canvas.clear();

		boolean avatarDrawn = false;

		objects.sort((Obstacle o1, Obstacle o2) -> Float.compare(o2.getY(),o1.getY()));
		
		// Draw the sprites first (will be hidden by shadows)
		canvas.begin();

		board.draw(canvas);

		for(SwitchModel s : switches) {
			s.draw(canvas);
		}
		for(Obstacle obj : objects) {
//			if (!obj.getClass().equals(SwitchModel.class)) {
//			    obj.draw(canvas);
//			}
//			if(!obj.equals(avatar) && !obj.getClass().equals(GuardModel.class)){
			if(obj.getClass().equals(SwitchModel.class)||obj.getClass().equals(DoorModel.class)||obj.getClass().equals(CameraModel.class)){
				obj.draw(canvas);
					if(Math.abs(board.physicsToBoard(obj.getX())-board.physicsToBoard(avatar.getX()))<=1 &&
					board.physicsToBoard(obj.getY())==board.physicsToBoard(avatar.getY())) {
						avatar.draw(canvas);
						avatarDrawn = true;
					}
					for(GuardModel g: guards){
						if(Math.abs(board.physicsToBoard(obj.getX())-board.physicsToBoard(g.getX()))<=1 &&
								board.physicsToBoard(obj.getY())==board.physicsToBoard(g.getY())) {
							g.draw(canvas);
						}
					}
			} else if (obj.getClass().equals(Laser.class)){
				obj.draw(canvas);
				if (Math.abs(board.physicsToBoard(obj.getX()) - board.physicsToBoard(avatar.getX()))<=1 &&
						Math.abs(board.physicsToBoard(obj.getY()+((Laser) obj).getLaserHeight()/2f) - board.physicsToBoard(avatar.getY()))<=((Laser) obj).getLaserHeight()/2f) {
					avatar.draw(canvas);
					avatarDrawn = true;
				}
				for(GuardModel g: guards){
					if(Math.abs(board.physicsToBoard(obj.getX()) - board.physicsToBoard(g.getX()))<=1.5f &&
							Math.abs(board.physicsToBoard(obj.getY()+((Laser) obj).getLaserHeight()/2f) - board.physicsToBoard(g.getY()))<=((Laser) obj).getLaserHeight()/2f) {
						g.draw(canvas);
					}
				}
			} else if (!obj.equals(avatar)||!avatarDrawn){
				obj.draw(canvas);
			}
//			if (!obj.equals(avatar) && board.physicsToBoard(obj.getX()) == board.physicsToBoard(avatar.getX())
//								&& board.physicsToBoard(obj.getY()) == board.physicsToBoard(avatar.getY())) {
//				obj.draw(canvas);
//				avatar.draw(canvas);
//				avatarOnTop = true;
//			} else {
//				obj.draw(canvas);
//			}
//			if (obj.equals(avatar) && !avatarOnTop) {
//				obj.draw(canvas);
//			}
		}
		canvas.end();

		// Now draw the shadows
		if (rayhandler != null && activeLight != -1) {
			rayhandler.render();
		}
		
		// Draw debugging on top of everything.
		if (debug) {
			canvas.beginDebug();
			for(Obstacle obj : objects) {
				obj.drawDebug(canvas);
			}
			canvas.endDebug();
		}
	}
	
	
	/**
	 * Returns a string equivalent to the sequence of bits in s
	 *
	 * This function assumes that s is a string of 0s and 1s of length < 16.
	 * This function allows the JSON file to specify bit arrays in a readable 
	 * format.
	 *
	 * @param s the string representation of the bit array
	 * 
	 * @return a string equivalent to the sequence of bits in s
	 */
	public static short bitStringToShort(String s) {
		short value = 0;
		short pos = 1;
		for(int ii = s.length()-1; ii >= 0; ii--) {
			if (s.charAt(ii) == '1') {
				value += pos;
			}
			pos *= 2;
		}
		return value;
	}
	
	/**
	 * Returns a string equivalent to the COMPLEMENT of bits in s
	 *
	 * This function assumes that s is a string of 0s and 1s of length < 16.
	 * This function allows the JSON file to specify exclusion bit arrays (for masking)
	 * in a readable format.
	 *
	 * @param s the string representation of the bit array
	 * 
	 * @return a string equivalent to the COMPLEMENT of bits in s
	 */
	public static short bitStringToComplement(String s) {
		short value = 0;
		short pos = 1;
		for(int ii = s.length()-1; ii >= 0; ii--) {
			if (s.charAt(ii) == '0') {
				value += pos;
			}
			pos *= 2;
		}
		return value;
	}

}
