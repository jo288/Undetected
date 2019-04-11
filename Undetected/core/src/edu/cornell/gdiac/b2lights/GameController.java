/*
 * GameController.java
 *
 * This combines the WorldController with the mini-game specific PlatformController
 * in the last lab.  With that said, some of the work is now offloaded to the new
 * LevelModel class, which allows us to serialize and deserialize a level. 
 * 
 * This is a refactored version of WorldController from Lab 4.  It separate the 
 * level out into a new class called LevelModel.  This model is, in turn, read
 * from a JSON file.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * B2Lights version, 3/12/2016
 */
package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import edu.cornell.gdiac.util.*;

import edu.cornell.gdiac.physics.obstacle.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.security.Guard;
import java.util.ArrayList;

/**
 * Gameplay controller for the game.
 *
 * This class does not have the Box2d world.  That is stored inside of the
 * LevelModel object, as the world settings are determined by the JSON
 * file.  However, the class does have all of the controller functionality,
 * including collision listeners for the active level.
 *
 * You will notice that asset loading is very different.  It relies on the
 * singleton asset manager to manage the various assets.
 */
public class GameController implements Screen, ContactListener {
	/** 
	 * Tracks the asset state.  Otherwise subclasses will try to load assets 
	 */
	protected enum AssetState {
		/** No assets loaded */
		EMPTY,
		/** Still loading assets */
		LOADING,
		/** Assets are complete */
		COMPLETE
	}

	private LightController lightController;
	
	/** The reader to process JSON files */
	private JsonReader jsonReader;
	 /** The JSON asset directory */
	private JsonValue  assetDirectory;
	/** The JSON defining the level model */
	private JsonValue  levelFormat;

	/** The font for giving messages to the player */
	protected BitmapFont displayFont;
	
	/** Track asset loading from all instances and subclasses */
	private AssetState assetState = AssetState.EMPTY;
	
	/**
	 * Preloads the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time.  However, we still want the assets themselves to be static.  So
	 * we have an AssetState that determines the current loading state.  If the
	 * assets are already loaded, this method will do nothing.
	 * 
	 * //@param manager Reference to global asset manager.
	 */	
	public void preLoadContent() {
		if (assetState != AssetState.EMPTY) {
			return;
		}
		
		assetState = AssetState.LOADING;

		jsonReader = new JsonReader();
		assetDirectory = jsonReader.parse(Gdx.files.internal("jsons/assets.json"));

		JsonAssetManager.getInstance().loadDirectory(assetDirectory);
	}

	/**
	 * Load the assets for this controller.
	 *
	 * To make the game modes more for-loop friendly, we opted for nonstatic loaders
	 * this time.  However, we still want the assets themselves to be static.  So
	 * we have an AssetState that determines the current loading state.  If the
	 * assets are already loaded, this method will do nothing.
	 * 
	 * //@param manager Reference to global asset manager.
	 */
	public void loadContent() {
		if (assetState != AssetState.LOADING) {
			return;
		}
		
		JsonAssetManager.getInstance().allocateDirectory();
		displayFont = JsonAssetManager.getInstance().getEntry("display", BitmapFont.class);
		assetState = AssetState.COMPLETE;
	}
	

	/** 
	 * Unloads the assets for this game.
	 * 
	 * This method erases the static variables.  It also deletes the associated textures 
	 * from the asset manager. If no assets are loaded, this method does nothing.
	 * 
	 * //@param manager Reference to global asset manager.
	 */
	public void unloadContent() {
		JsonAssetManager.getInstance().unloadDirectory();
		JsonAssetManager.clearInstance();
	}

	
	/** Exit code for quitting the game */
	public static final int EXIT_QUIT = 0;
    /** How many frames after winning/losing do we continue? */
	public static final int EXIT_COUNT = 120;

	/** Reference to the game canvas */
	protected ObstacleCanvas canvas;
	/** Listener that will update the player mode when we are done */
	private ScreenListener listener;

	/** Reference to the game level */
	protected LevelModel level;
		
	/** Whether or not this is an active controller */
	private boolean active;
	/** Whether we have completed this level */
	private boolean complete;
	/** Whether we have failed at this world (and need a reset) */
	private boolean failed;
	/** Whether the level is paused or not */
	private boolean paused;
	/** Countdown active for winning or losing */
	private int countdown;
	/** Whether the player reached the objective or not */
	private boolean hasObjective;
	private GuardModel guardCollided = null;
	private SwitchModel switchCollided = null;
	private FileHandle currentFile = Gdx.files.internal("jsons/level1.json");


	/** Mark set to handle more sophisticated collision callbacks */
	protected ObjectSet<Fixture> sensorFixtures;
	/** Whether the player collided with a box */
	protected boolean avatarBoxCollision;
	/** Whether the player collided with a laser */
	protected boolean avatarLaserCollision;

	/**
	 * Returns true if the level is completed.
	 *
	 * If true, the level will advance after a countdown
	 *
	 * @return true if the level is completed.
	 */
	public boolean isComplete( ) {
		return complete;
	}

	/**
	 * Sets whether the level is completed.
	 *
	 * If true, the level will advance after a countdown
	 *
	 * @param value whether the level is completed.
	 */
	public void setComplete(boolean value) {
		if (value) {
			countdown = EXIT_COUNT;
		}
		complete = value;
	}

	/**
	 * Returns true if the level is failed.
	 *
	 * If true, the level will reset after a countdown
	 *
	 * @return true if the level is failed.
	 */
	public boolean isFailure( ) {
		return failed;
	}

	/**
	 * Sets whether the level is failed.
	 *
	 * If true, the level will reset after a countdown
	 *
	 * @param value whether the level is failed.
	 */
	public void setFailure(boolean value) {
		if (value) {
			countdown = EXIT_COUNT;
		}
		failed = value;
	}
	
	/**
	 * Returns true if this is the active screen
	 *
	 * @return true if this is the active screen
	 */
	public boolean isActive( ) {
		return active;
	}

	/**
	 * Returns the canvas associated with this controller
	 *
	 * The canvas is shared across all controllers
	 *
	 * //@param the canvas associated with this controller
	 */
	public ObstacleCanvas getCanvas() {
		return canvas;
	}
	
	/**
	 * Sets the canvas associated with this controller
	 *
	 * The canvas is shared across all controllers.  Setting this value will compute
	 * the drawing scale from the canvas size.
	 *
	 * //@param value the canvas associated with this controller
	 */
	public void setCanvas(ObstacleCanvas canvas) {
		this.canvas = canvas;
	}
	
	/**
	 * Creates a new game world 
	 *
	 * The physics bounds and drawing scale are now stored in the LevelModel and
	 * defined by the appropriate JSON file.
	 */
	public GameController() {
		jsonReader = new JsonReader();
		level = new LevelModel();
		lightController = new LightController(level);
		complete = false;
		failed = false;
		active = false;
		countdown = -1;
		avatarBoxCollision = false;
		avatarLaserCollision = false;
		hasObjective = false;

		setComplete(false);
		setFailure(false);
		sensorFixtures = new ObjectSet<Fixture>();
	}
	
	/**
	 * Dispose of all (non-static) resources allocated to this mode.
	 */
	public void dispose() {
		level.dispose();
		level  = null;
		canvas = null;
	}
	
	/**
	 * Resets the status of the game so that we can play again.
	 *
	 * This method disposes of the level and creates a new one. It will 
	 * reread from the JSON file, allowing us to make changes on the fly.
	 */
	public void reset() {
		level.dispose();
		
		setComplete(false);
		setFailure(false);
		hasObjective = false;
		countdown = -1;
		
		// Reload the json each time
		levelFormat = jsonReader.parse(currentFile);
		level.populate(levelFormat);
		level.getWorld().setContactListener(this);
		resetCamera();

		guardCollided = null;
	}

	public void loadLevel(){
		// Reload the json each time
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"JSON level files", "json");
		chooser.setFileFilter(filter);
		chooser.setCurrentDirectory(Gdx.files.internal("jsons").file());
		String loadFile = "";
		JFrame f = new JFrame();
		f.setVisible(true);
		f.toFront();
		f.setVisible(false);
		int returnVal = chooser.showOpenDialog(f);
		f.dispose();
		if(returnVal == JFileChooser.APPROVE_OPTION) {
			System.out.println("You chose to open this file: " +
					chooser.getSelectedFile().getPath());
			loadFile = chooser.getSelectedFile().getPath();
		}

		try{
			levelFormat = jsonReader.parse(Gdx.files.absolute(loadFile));
			currentFile = Gdx.files.absolute(loadFile);
			LevelModel newLoad = new LevelModel();
			newLoad.populate(levelFormat);
			level.dispose();
			level = newLoad;
			level.getWorld().setContactListener(this);

			setComplete(false);
			setFailure(false);
			hasObjective = false;
			countdown = -1;
			guardCollided = null;
			lightController = new LightController(level);

			resetCamera();
		}
		catch (Exception e){
			System.out.println("WRONG FILE");
		}
	}


	public void cameraPan(float dt){
//		float playerX = level.getAvatar().getX();
//		float playerY = level.getAvatar().getY();
//		//pan the canvas camera
//		OrthographicCamera cam = canvas.getCamera();
//		float cx = canvas.getWidth()/2;
//		float cy = canvas.getHeight()/2;
//
//		float vw = cam.viewportWidth;
//		float vh = cam.viewportHeight;
//		float effectiveVW = vw * cam.zoom;
//		float effectiveVH = vh * cam.zoom;
//		float dx = (vw - effectiveVW)/2;
//		float dy = (vh - effectiveVH)/2;
//		float sx = vw / level.bounds.width;
//		float sy = vh / level.bounds.height;
//		cam.position.x = MathUtils.clamp(playerX*sx, cx-dx, cx + dx);
//		cam.position.y = MathUtils.clamp(playerY*sy, cy-dy, cy + dy);

		///*WORKING
		float playerX = level.getAvatar().getX();
		float playerY = level.getAvatar().getY();
		//pan the canvas camera
		OrthographicCamera cam = canvas.getCamera();
//		float cx = canvas.getWidth()/2;
//		float cy = canvas.getHeight()/2;
		float mincx = canvas.getHeight()/2;
		float mincy = canvas.getHeight()/2;
		float maxcx = level.bounds.width*level.scale.x - mincx;
		float maxcy = level.bounds.height*level.scale.y - mincy;

		//cam.translate(input.getHorizontal(), input.getVertical());
		float vw = cam.viewportWidth;
		float vh = cam.viewportHeight;
		float effectiveVW = vw * cam.zoom;
		float effectiveVH = vh * cam.zoom;
		float dx = (vw - effectiveVW)/2;
		float dy = (vh - effectiveVH)/2;
//		float sx = vw / level.bounds.width;
//		float sy = vh / level.bounds.height;
		float sx = 32;
		float sy = 32;
//		cam.position.set(playerX*sx, playerY*sy, 0);
		cam.position.x += (MathUtils.clamp(playerX*sx, mincx-dx, maxcx + dx) - cam.position.x)*dt*2.8;
		cam.position.y += (MathUtils.clamp(playerY*sy, mincy-dy, maxcy + dy)  - cam.position.y)*dt*2.8;
//		cam.position.y = 600;


		//pan the rayhandler camera
		OrthographicCamera rcam = level.raycamera;
		float rcx = level.bounds.width/2;
		float rcy = level.bounds.height/2;
		float minrcx = mincx/32f;
		float minrcy = mincy/32f;
		float maxrcx = maxcx/32f;
		float maxrcy = maxcy/32f;
//		float reffectiveVW = level.bounds.width*rcam.zoom;
//		float reffectiveVH = level.bounds.height*rcam.zoom;
		float reffectiveVW = (canvas.getWidth()/32f)*rcam.zoom;
		float reffectiveVH = (canvas.getHeight()/32f)*rcam.zoom;
		float rdx = (level.bounds.width - reffectiveVW)/2;
		float rdy = (level.bounds.height - reffectiveVH)/2;
//		rcam.position.set(playerX, playerY , 0);
//		rcam.position.x = MathUtils.clamp(playerX, minrcx-rdx, maxrcx + rdx);
//		rcam.position.y = MathUtils.clamp(playerY, minrcy-rdy, maxrcy + rdy);
		rcam.position.x += (MathUtils.clamp(playerX, (mincx-dx)/32f, (maxcx+dx)/32f)-rcam.position.x)*dt*2.8;
		rcam.position.y += (MathUtils.clamp(playerY, (mincy-dy)/32f, (maxcy+dy)/32f)-rcam.position.y)*dt*2.8;
//		rcam.zoom = cam.zoom * ()
		rcam.update();
		level.rayhandler.setCombinedMatrix(rcam);
		level.rayhandler.updateAndRender();
	}

	public void resetCamera(){
		float cx = canvas.getCamera().position.x;
		float cy = canvas.getCamera().position.y;
		canvas.getCamera().translate(canvas.getWidth()/2-cx, canvas.getHeight()/2-cy);
		canvas.getCamera().zoom=1;
	}
	
	/**
	 * Returns whether to process the update loop
	 *
	 * At the start of the update loop, we check if it is time
	 * to switch to a new game mode.  If not, the update proceeds
	 * normally.
	 *
	 * //@param delta Number of seconds since last animation frame
	 * 
	 * @return whether to process the update loop
	 */
	public boolean preUpdate(float dt) {
		InputController input = InputController.getInstance();
		input.readInput();
		if (listener == null) {
			return true;
		}

		// Toggle debug
		if (input.didDebug()) {
			level.setDebug(!level.getDebug());
		}
		if(input.zoomIn()){
			canvas.getCamera().zoom = MathUtils.clamp(canvas.getCamera().zoom-0.01f, 0.4f, 1f);
			level.raycamera.zoom = MathUtils.clamp(level.raycamera.zoom-0.01f, 0.4f, 1f);
			level.raycamera.update();
			level.rayhandler.setCombinedMatrix(level.raycamera);
			level.rayhandler.updateAndRender();
		}
		else if(input.zoomOut()){
			canvas.getCamera().zoom = MathUtils.clamp(canvas.getCamera().zoom+0.01f, 1f, 1.2f);
			level.raycamera.zoom = MathUtils.clamp(level.raycamera.zoom+0.01f, 1f, 1.2f);

			level.raycamera.update();
			level.rayhandler.setCombinedMatrix(level.raycamera);
			level.rayhandler.updateAndRender();
		}

		GuardModel guard = level.getGuards().get(0);
		float degree = guard.getLight().getConeDegree();
		if(input.increaseView()){
			guard.getLight().setConeDegree(degree+0.5f);
		}
		else if(input.decreaseView()){
			guard.getLight().setConeDegree(degree-0.5f);
		}
		
		// Handle resets
		if (input.didReset()) {
			reset();
		}


		if (input.didLoad()){
			loadLevel();
		}
		
		// Now it is time to maybe switch screens.
		if (input.didExit()) {
			listener.exitScreen(this, EXIT_QUIT);
			return false;
		} else if (countdown > 0) {
			countdown--;
		} else if (countdown == 0) {
			reset();
		}
		
		return true;
	}
	
	private Vector2 angleCache = new Vector2();
	/**
	 * The core gameplay loop of this world.
	 *
	 * This method contains the specific update code for this mini-game. It does
	 * not handle collisions, as those are managed by the parent class WorldController.
	 * This method is called after input is read, but before collisions are resolved.
	 * The very last thing that it should do is apply forces to the appropriate objects.
	 *
	 * //@param delta Number of seconds since last animation frame
	 */
	public void update(float dt) {
		// Process actions in object model
		DudeModel avatar = level.getAvatar();
		ArrayList<GuardModel> guards = level.getGuards();
		InputController input = InputController.getInstance();

		if (input.didPause() && !paused) {
			pause();
		}

		// LASER CHECK

		/*if (input.didForward()) {
			level.activateNextLight();
		} else if (input.didBack()){
			level.activatePrevLight();
		}*/

		if(input.didAction() && avatar.getHasBox()){
			level.placeBox(avatar);
		} else if(input.didAction() && !avatar.getHasBox() && avatarBoxCollision){
			avatar.setBoxHeld(avatar.getBoxInContact());
			avatar.pickupBox();
			level.queueDisabled(avatar.getBoxInContact());
		} else if(input.didAction() && switchCollided != null) {
			switchCollided.switchMode();
		}
		//camera follow player
		//canvas.getCamera().translate(input.getHorizontal(), input.getVertical());
		int cx = (int)canvas.getCamera().position.x;
		int cy = (int)canvas.getCamera().position.y;
		//level.rayhandler.useCustomViewport(cx,cy,800,600);
		//level.raycamera.translate(input.getHorizontal(), input.getVertical());
		//level.raycamera.update();
		// Rotate the avatar to face the direction of movement
		angleCache.set(input.getHorizontal(),input.getVertical());
		if (angleCache.len2() > 0.0f) {
			float angle = angleCache.angle();
			// Convert to radians with up as 0
			angle = (float)Math.PI*(angle-90.0f)/180.0f;
//			avatar.setAngle(angle);
			avatar.setDirection(angle);
		}
		if(angleCache.x!=0f &&angleCache.y!=0f)
			angleCache.set(angleCache.x*0.7071f,angleCache.y*0.7071f);
		angleCache.scl(avatar.getForce());
		avatar.setMovement(angleCache.x,angleCache.y);
		System.out.println(avatar.getMovement());
		avatar.applyForce();

		//only used if we are manually controlling one guard for demo purposes
		GuardModel guard = guards.get(0);
		Vector2 guardAngle = new Vector2(input.getHorizontalG(),input.getVerticalG());
		if (guardAngle.len2() > 0.0f) {
			float angle = guardAngle.angle();
			// Convert to radians with up as 0
			angle = (float)Math.PI*(angle-90.0f)/180.0f;
			guard.setDirection(angle);
		}
		guardAngle.scl(guard.getForce());
		guard.setMovement(guardAngle.x,guardAngle.y);
		guard.applyForce();

		cameraPan(dt);

		if (guardCollided != null) {
			guardCollided.collidedAvatar(avatar);
		}

		// Turn the physics engine crank.
		level.update(dt);
		if(lightController.detect() && !failed){
			setFailure(true);
		}
	}
	
	/**
	 * Draw the physics objects to the canvas
	 *
	 * For simple worlds, this method is enough by itself.  It will need
	 * to be overriden if the world needs fancy backgrounds or the like.
	 *
	 * The method draws all objects in the order that they were added.
	 *
	 * //@param canvas The drawing context
	 */
	public void draw(float delta) {
		canvas.clear();
		
		level.draw(canvas);

//		framerate display
//        displayFont.setColor(Color.YELLOW);
//        canvas.begin(); // DO NOT SCALE
//        canvas.drawTextCentered(""+(int)(1f/delta), displayFont, 0.0f);
//        canvas.end();

		// Final message
		if (complete && !failed) {
			displayFont.setColor(Color.YELLOW);
			canvas.begin(); // DO NOT SCALE
			canvas.drawTextCentered("VICTORY!", displayFont, 0.0f);
			canvas.end();
		} else if (failed) {
			displayFont.setColor(Color.RED);
			canvas.begin(); // DO NOT SCALE
			canvas.drawTextCentered("FAILURE!", displayFont, 0.0f);
			canvas.end();
		}

		if (paused) {
			Texture pauseButton = new Texture(assetDirectory.get("textures").get("pause").getString("file"));
			canvas.begin();
			canvas.draw(pauseButton, new Color(Color.GRAY), pauseButton.getWidth()/2, pauseButton.getHeight()/2,
					canvas.getWidth()/2, canvas.getHeight()/2, 0, 0.3f, 0.3f);
			canvas.end();
		}
	}
	
	/**
	 * Called when the Screen is resized. 
	 *
	 * This can happen at any point during a non-paused state but will never happen 
	 * before a call to show().
	 *
	 * @param width  The new width in pixels
	 * @param height The new height in pixels
	 */
	public void resize(int width, int height) {
		// IGNORE FOR NOW
	}

	/**
	 * Called when the Screen should render itself.
	 *
	 * We defer to the other methods update() and draw().  However, it is VERY important
	 * that we only quit AFTER a draw.
	 *
	 * @param delta Number of seconds since last animation frame
	 */
	public void render(float delta) {
		if (active) {
			if (!paused) {
				if (preUpdate(delta)) {
					update(delta);
				}
				draw(delta);
			}
			else {
				InputController input = InputController.getInstance();
				input.readInput();
				if (input.didPause()) {
					resume();
				}
				draw(delta);
			}
		}
	}

	/**
	 * Called when the Screen is paused.
	 * 
	 * This is usually when it's not active or visible on screen. An Application is 
	 * also paused before it is destroyed.
	 */
	public void pause() {
		paused = true;
		ArrayList<Laser> lasers = level.getLasers();
		for (Laser l : lasers) {
			l.pause();
		}
	}

	/**
	 * Called when the Screen is resumed from a paused state.
	 *
	 * This is usually when it regains focus.
	 */
	public void resume() {
		paused = false;
		ArrayList<Laser> lasers = level.getLasers();
		for (Laser l : lasers) {
			l.resume();
		}
	}
	
	/**
	 * Called when this screen becomes the current screen for a Game.
	 */
	public void show() {
		// Useless if called in outside animation loop
		active = true;
	}

	/**
	 * Called when this screen is no longer the current screen for a Game.
	 */
	public void hide() {
		// Useless if called in outside animation loop
		active = false;
	}

	/**
	 * Sets the ScreenListener for this mode
	 *
	 * The ScreenListener will respond to requests to quit.
	 */
	public void setScreenListener(ScreenListener listener) {
		this.listener = listener;
	}

	/**
	 * Callback method for the start of a collision
	 *
	 * This method is called when we first get a collision between two objects.  We use 
	 * this method to test if it is the "right" kind of collision.  In particular, we
	 * use it to test if we made it to the win door.
	 *
	 * @param contact The two bodies that collided
	 */
	public void beginContact(Contact contact) {
		Fixture fix1 = contact.getFixtureA();
		Fixture fix2 = contact.getFixtureB();

		Body body1 = fix1.getBody();
		Body body2 = fix2.getBody();

		Object fd1 = fix1.getUserData();
		Object fd2 = fix2.getUserData();
		
		try {
			Obstacle bd1 = (Obstacle)body1.getUserData();
			Obstacle bd2 = (Obstacle)body2.getUserData();

			DudeModel avatar = level.getAvatar();
			ExitModel exit   = level.getExit();
			ArrayList<GuardModel> guards = level.getGuards();
			ArrayList<SwitchModel> switches = level.getSwtiches();
			ObjectiveModel objective = level.getObjective();

            if((guards.contains(bd1) && bd2==avatar ) || (bd1 == avatar && guards.contains(bd2))){
				if (guards.contains(bd1)) {
					GuardModel guard = guards.get(guards.indexOf(bd1));
					guardCollided = guard;
				} else if (guards.contains(bd2)) {
					GuardModel guard = guards.get(guards.indexOf(bd2));
					guardCollided = guard;
				}
                setFailure(true);
            }

			if((switches.contains(bd1) && bd2==avatar ) || (bd1 == avatar && switches.contains(bd2))){
				if (switches.contains(bd1)) {
					SwitchModel switchi = switches.get(switches.indexOf(bd1));
					switchCollided = switchi;
				} else if (switches.contains(bd2)) {
					SwitchModel switchi = switches.get(switches.indexOf(bd2));
					switchCollided = switchi;
				}
			}

			if((bd1==avatar && bd2 instanceof MoveableBox ) || (bd1 instanceof MoveableBox && bd2==avatar)){
				avatarBoxCollision = true;
				if (bd1 instanceof  MoveableBox) {
				    avatar.setBoxInContact(bd1);
                } else if (bd2 instanceof  MoveableBox) {
				    avatar.setBoxInContact(bd2);
                }
			}

			// Check for objective
			if ((bd1 == avatar && bd2 == objective) || (bd1 == objective && bd2== avatar)){
			    if(bd1 instanceof ObjectiveModel){
			        ((ObjectiveModel) bd1).stealCard();
                }if(bd2 instanceof ObjectiveModel){
			        ((ObjectiveModel) bd2).stealCard();
                }
				hasObjective = true;
				exit.open();
//				level.queueDestroyed(objective);
			}

			// Check for win condition
			if ((bd1 == avatar && bd2 == exit && hasObjective ) ||
				(bd1 == exit   && bd2 == avatar && hasObjective)) {
				setComplete(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/** Unused ContactListener method */
	public void endContact(Contact contact) {
		Fixture fix1 = contact.getFixtureA();
		Fixture fix2 = contact.getFixtureB();

		Body body1 = fix1.getBody();
		Body body2 = fix2.getBody();

		Object fd1 = fix1.getUserData();
		Object fd2 = fix2.getUserData();

		Obstacle bd1 = (Obstacle)body1.getUserData();
		Obstacle bd2 = (Obstacle)body2.getUserData();

		DudeModel avatar = level.getAvatar();
		ArrayList<SwitchModel> switches = level.getSwtiches();

		if((bd1 == avatar && bd2 instanceof MoveableBox) || (bd1 instanceof MoveableBox && bd2==avatar)){
			avatarBoxCollision = false;
            avatar.setBoxInContact(null);
		}

		if((switches.contains(bd1) && bd2==avatar ) || (bd1 == avatar && switches.contains(bd2))){
			switchCollided = null;
		}

		if((bd1 == avatar && bd2 instanceof Laser) || (bd1 instanceof Laser && bd2==avatar)){
			avatarLaserCollision = false;
		}
	}

	/** Unused ContactListener method */
	public void postSolve(Contact contact, ContactImpulse impulse) {

	}
	/** Unused ContactListener method */
	public void preSolve(Contact contact, Manifold oldManifold) {
		Fixture fix1 = contact.getFixtureA();
		Fixture fix2 = contact.getFixtureB();

		Body body1 = fix1.getBody();
		Body body2 = fix2.getBody();

		Object fd1 = fix1.getUserData();
		Object fd2 = fix2.getUserData();

		Obstacle bd1 = (Obstacle)body1.getUserData();
		Obstacle bd2 = (Obstacle)body2.getUserData();
		DudeModel avatar = level.getAvatar();

		if (((bd1 == avatar && bd2 instanceof Laser) || (bd1 instanceof Laser && bd2==avatar)) ||
				((bd1 instanceof MoveableBox && bd2 instanceof Laser) ||
						(bd1 instanceof Laser && bd2 instanceof MoveableBox))){
			contact.setEnabled(false);
			if (bd1 instanceof Laser) {
				if (((Laser) bd1).isTurnedOn()) {
					avatarLaserCollision = true;
//					if(!failed){
//						setFailure(true);
//					}
					for (AIController ai : level.getControl()) {
						if (Math.abs(bd1.getX()-ai.getGuardX())<10) {
							ai.setAlarmed();
							ai.setProtect(bd1);
							System.out.println("laser");
						}
					}
				}
				else{
					avatarLaserCollision =false;
				}
			}
			else{
				if (((Laser) bd2).isTurnedOn()) {
					avatarLaserCollision = true;
//					if(!failed){
//						setFailure(true);
//					}
					for (AIController ai : level.getControl()) {

						if (Math.abs(bd2.getX()-ai.getGuardX())<10) {
							ai.setAlarmed();
							ai.setProtect(bd2);
							System.out.println("laser");
						}
					}
				}
				else{
					avatarLaserCollision = false;
				}
			}
		}
	}
}