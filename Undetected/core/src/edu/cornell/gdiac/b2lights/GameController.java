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
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Timer;
import edu.cornell.gdiac.util.*;

import edu.cornell.gdiac.physics.obstacle.*;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.security.Guard;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashSet;

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
	private MiniMap miniMap;
	private boolean showMiniMap = false;
	private boolean showExit = false;
	private boolean panToPlayer = false; //pans back to player after showing exit

	private Music theme;
	private Music preHeist;
	private Music postHeist;
	private Music currentSong;

	/** The font for giving messages to the player */
	protected BitmapFont displayFont;
	protected BitmapFont levelselectfont;
	protected BitmapFont levelnumfont;

	private int floatframe = 40;

	/** Track asset loading from all instances and subclasses */
	private AssetState assetState = AssetState.EMPTY;
	private LevelParser levelparser = new LevelParser();
	private Texture pauseButton;

	private HashSet<Obstacle> boxes = new HashSet<>();

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

		initMusic();

		if (!theme.isPlaying()) {
			currentSong = theme;
			play(theme);
		}
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
		pauseButton = new Texture(assetDirectory.get("textures").get("pauseMenu").getString("file"));
		displayFont = JsonAssetManager.getInstance().getEntry("display", BitmapFont.class);
		levelselectfont = JsonAssetManager.getInstance().getEntry("levelselect", BitmapFont.class);
		levelnumfont = JsonAssetManager.getInstance().getEntry("levelnumber", BitmapFont.class);
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
	public static  final float DEFAULT_VOL = 0.5f;

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
	private boolean music = true;
	private boolean sound = true;
	/** Whether the level is paused or not */
	private boolean paused;
	/** Countdown active for winning or losing */
	private int countdown;
	/** Whether the player reached the objective or not */
	private boolean hasObjective;
	private GuardModel guardCollided = null;
	private GuardModel guardCaught = null;
	private SwitchModel switchCollided = null;
	private FileHandle levelSelectFile = Gdx.files.internal("jsons/levelselect.json");
	private FileHandle currentFile = Gdx.files.internal("jsons/levelselect.json");
	private String currentLevelString = "";
	private FileHandle nextFile;


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
		miniMap = new MiniMap(300, 225, level);
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
		miniMap = null;
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
		if(currentFile!=null)
			levelFormat = jsonReader.parse(currentFile);
		else
			levelFormat = jsonReader.parse(currentLevelString);
		level.populate(levelFormat);
		level.getWorld().setContactListener(this);
		miniMap = new MiniMap(300, 225, level);

		resetCamera();

		guardCollided = null;
		guardCaught = null;
	}

	public void loadXMLLevel(){
		JFileChooser chooser = new JFileChooser();
		FileNameExtensionFilter filter = new FileNameExtensionFilter(
				"TMX level files", "tmx");
		chooser.setFileFilter(filter);
		chooser.setCurrentDirectory(Gdx.files.internal("levels").file());
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
			levelFormat = jsonReader.parse(levelparser.readXml(Gdx.files.absolute(loadFile)));
			currentFile = null;
			currentLevelString = levelparser.readXml(Gdx.files.absolute(loadFile));
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
			guardCaught = null;
			lightController = new LightController(level);
			miniMap = new MiniMap(300, 225, level);

			resetCamera();
		}
		catch (Exception e){
			System.out.println("WRONG FILE");
		}
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
			guardCaught = null;
			lightController = new LightController(level);
			miniMap = new MiniMap(300, 225, level);

			resetCamera();
		}
		catch (Exception e){
			System.out.println("WRONG FILE");
		}
	}


	public void cameraPan(float dt){
		///*WORKING
		float playerX = level.getAvatar().getX();
		float playerY = level.getAvatar().getY();
		//pan the canvas camera
		OrthographicCamera cam = canvas.getCamera();
		float cx = level.bounds.width*level.scale.x/2;
		float cy = level.bounds.height*level.scale.y/2;
		float vw = cam.viewportWidth;
		float vh = cam.viewportHeight;
		float effectiveVW = vw * cam.zoom;
		float effectiveVH = vh * cam.zoom;
		float dx = Math.abs((level.bounds.width*level.scale.x - effectiveVW)/2);
		float dy = Math.abs((level.bounds.height*level.scale.y - effectiveVH)/2);
		float sx = 32;
		float sy = 32;
		if(!showExit || panToPlayer) {
			float deltaX = (MathUtils.clamp(playerX * sx, cx - dx, cx + dx) - cam.position.x) * dt * 2.8f;
			float deltaY = (MathUtils.clamp(playerY * sy, cy - dy, cy + dy) - cam.position.y) * dt * 2.8f;
			if(panToPlayer){
				if(Math.abs(deltaX)<=0.09 && Math.abs(deltaY)<=0.09){
					showExit = false;
					panToPlayer = false;
				}
				cam.position.x += deltaX;
				cam.position.y += deltaY;
			}
			else {
				cam.position.x += deltaX;
				cam.position.y += deltaY;
			}
		}
		else{
			cam.position.x += (MathUtils.clamp(level.getExit().getX() * sx, cx - dx, cx + dx) - cam.position.x) * dt * 3.2;
			cam.position.y += (MathUtils.clamp(level.getExit().getY() * sy, cy - dy, cy + dy) - cam.position.y) * dt * 3.2;
		}
		//pan the rayhandler camera
		OrthographicCamera rcam = level.raycamera;
		rcam.position.x = cam.position.x/32;
		rcam.position.y = cam.position.y/32;
		rcam.update();
		level.rayhandler.setCombinedMatrix(rcam);
		level.rayhandler.updateAndRender();
	}

	public void resetCamera(){
		canvas.getCamera().zoom=1;
		level.raycamera.zoom = 1;
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

//		GuardModel guard = level.getGuards().get(0);
//		float degree = guard.getLight().getConeDegree();
//		if(input.increaseView()){
//			guard.getLight().setConeDegree(degree+0.5f);
//		}
//		else if(input.decreaseView()){
//			guard.getLight().setConeDegree(degree-0.5f);
//		}

		// Handle resets
		if (input.didReset()&&!currentFile.equals(levelSelectFile)) {
			reset();
		}


		if (input.didLoad()){
			loadLevel();
		}
		if (input.didLoadX()){
			loadXMLLevel();
		}

		// Now it is time to maybe switch screens.
		if (input.didExit()) {
			listener.exitScreen(this, EXIT_QUIT);
			return false;
		} else if (countdown > 0) {
			countdown--;
		} else if (countdown == 0) {
//			reset();
			if(failed)
				reset();
			else
				nextFile = levelSelectFile;
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
		ArrayList<CameraModel> cameras = level.getCameras();
		InputController input = InputController.getInstance();

		if (input.didInvinc()) {
			avatar.changeInvinc(); //MAKES THE PLAYER ABLE TO WALK THROUGH WALLS, WE MUST REMOVE THIS LATER
		}

		if (input.didPause() && !paused) {
			pause();
		}

		if (sound) {
			for (DoorModel d : level.getDoors()) {
				d.unmute();
			}
			for (Laser l : level.getLasers()) {
				l.unmute();
			}
			for (MoveableBox b : level.getBoxes()) {
				b.unmute();
			}
			if (level.getObjective() != null) {
				level.getObjective().unmute();
			}
		} else {
			for (DoorModel d : level.getDoors()) {
				d.mute();
			}
			for (Laser l : level.getLasers()) {
				l.mute();
			}
			for (MoveableBox b : level.getBoxes()) {
				b.mute();
			}
			if (level.getObjective() != null) {
				level.getObjective().mute();
			}
		}

		if (music && level.alarmStoppedPlaying()) {
			currentSong.setVolume(DEFAULT_VOL);
		}

		if(input.didMap()){
			showMiniMap = !showMiniMap;
		}

		if(input.didAction() && avatar.getHasBox()){
			Obstacle box = avatar.getBoxHeld();
			level.placeBox(avatar);
		} else if(input.didAction() && !avatar.getHasBox() && avatarBoxCollision){
			avatar.setBoxHeld(avatar.getBoxInContact());
			avatar.getBoxHeld().playPickup();
			avatar.pickupBox();
			level.queueDisabled(avatar.getBoxInContact());
			boxes.clear();
		} else if(input.didAction() && switchCollided != null) {
			switchCollided.switchMode();
			for (AIController ai : level.getControl()) {
				if (ai.getGuard().getAlarmed()) {
					ai.setLastSwitch(switchCollided);
				}
			}
		}

		if (currentFile.equals(levelSelectFile)) {
			ArrayList<DoorModel> doors = level.getDoors();
			//TODO: CHANGE FOR RELEASE, TEMP
//				for (DoorModel d : doors) {
			for (int i = 0; i < 10; i++) {
				DoorModel d = doors.get(i);
				if (d.getPosition().dst(avatar.getPosition())<0.3f){
					nextFile = Gdx.files.internal("jsons/" + (d.getName()) + ".json");
				} else if (d.getPosition().dst(avatar.getPosition())<3f&&!d.getOpen()){
					d.switchState();
				}else if(d.getPosition().dst(avatar.getPosition())>=3f&&d.getOpen()){
					d.switchState();
				}
			}
		}
//		System.out.println(dt);

		if (!failed && !complete && !showExit &&!avatar.isElectrocuted()) {
			angleCache.set(input.getHorizontal(), input.getVertical());
			if (angleCache.len2() > 0.0f) {
				float angle = angleCache.angle();
				// Convert to radians with up as 0
				angle = (float) Math.PI * (angle - 90.0f) / 180.0f;
				avatar.setDirection(angle);
			}
			if (angleCache.x != 0f && angleCache.y != 0f)
				angleCache.set(angleCache.x * 0.7071f, angleCache.y * 0.7071f);
			angleCache.scl(avatar.getForce());
			avatar.setMovement(angleCache.x, angleCache.y);
			if (dt>0.016f) {
				avatar.applyForce();
			}
//			for (int i=0; i<(dt/0.015f)-1;i++){
//				System.out.println(i);
//				avatar.applyForce();
//			}
		}
		if (complete || failed || avatar.isElectrocuted()) {
			avatar.setMovement(0,0);
			avatar.applyForce();
		}
		if (complete || avatar.isElectrocuted()) {
			for (GuardModel g : guards) {
				g.setBodyType(BodyDef.BodyType.StaticBody);
			}
		}

		if (input.didHome()) {
			nextFile = Gdx.files.internal("jsons/levelselect.json");
			input.resetHome();
		}

		cameraPan(dt);

		if (guardCollided != null) {
			guardCollided.collidedAvatar(avatar);
		}

		// Turn the physics engine crank.
		if(!showExit) {
			level.update(dt);
			if (lightController.detectedByGuards(guards)!=null && !failed && !avatar.isElectrocuted()) {
				avatar.electrocute();
				guardCaught = lightController.detectedByGuards(guards);
				guardCaught.animateDirection((float)(Math.round(Math.atan2(
						avatar.getX()-guardCaught.getX(),
						avatar.getY()-guardCaught.getY()
				)/(Math.PI/2))*Math.PI/2));
				System.out.println((float)(Math.round(Math.atan2(
						avatar.getX()-guardCaught.getX(),
						avatar.getY()-guardCaught.getY()
				)/(Math.PI/2))*Math.PI/2));
				System.out.println("Raw:"+Math.atan2(
						avatar.getX()-guardCaught.getX(),
						avatar.getY()-guardCaught.getY()
				));
				guardCaught.getLight().setConeDegree(0f);
				guardCaught.setHasCaught(true);
			}
			else{
				CameraModel cam = lightController.detectedByCameras(cameras);
				if(cam!=null) {
					for (AIController ai : level.getControl()) {
						if (ai.getGuard().sector == cam.sector && ai.lastCamera != cam) {
							ai.setAlarmed();
							ai.setProtect(cam);
						}
					}
				}
			}
			if(!avatar.getIsAlive() && !failed){setFailure(true);}
		}
		if(showExit){
			level.getExit().animate(dt);
			if(!level.getExit().isAnimating() && !panToPlayer){
				panToPlayer = true;
			}
		}

		if(currentFile.equals(levelSelectFile)) {
			if (!theme.isPlaying()) {
				currentSong.stop();
				currentSong = theme;
				play(currentSong);
			}
		} else {
			if (!hasObjective) {
				if (currentSong != null) {
					if (!currentSong.equals(preHeist)) {
						currentSong.stop();
						currentSong = preHeist;
						play(currentSong);
					}
				} else {
					currentSong = preHeist;
					play(currentSong);
				}
			} else {
				if (currentSong != null) {
					if (!currentSong.equals(postHeist)) {
						currentSong.stop();
						currentSong = postHeist;
						play(currentSong);
					}
				} else {
					currentSong = postHeist;
					play(currentSong);
				}
			}
		}

		try {
			//load the next level if needed
			if (nextFile != null) {
				levelFormat = jsonReader.parse(nextFile);
				FileHandle lastFile = currentFile;
				currentFile = nextFile;
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
				guardCaught = null;
				lightController = new LightController(level);
				showExit = false;

//			System.out.println(lastFile.name());

				if (currentFile.equals(levelSelectFile)) {
					if (!theme.isPlaying()) {
						currentSong.stop();
						currentSong = theme;
						play(currentSong);
					}
					ArrayList<DoorModel> doors = level.getDoors();
					//TODO: CHANGE FOR RELEASE, TEMP
//				for (DoorModel d : doors) {
					for (int i = 0; i < 10; i++) {
						DoorModel d = doors.get(i);
						if ((d.getName() + ".json").equals(lastFile.name())) {
							System.out.println("level1 door");
							avatar = level.getAvatar();
							avatar.setPosition(d.getPosition().x, d.getPosition().y - 1);
							avatar.setDirection(3.14f);
						}
					}
				}
				miniMap = new MiniMap(300, 225, level);
				resetCamera();
				nextFile = null;
			}
		}catch (Exception e){

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
//      displayFont.setColor(Color.YELLOW);
//      canvas.begin(); // DO NOT SCALE
//      canvas.drawTextCentered(""+(int)(1f/delta), displayFont, 0.0f);
//      canvas.end();

		OrthographicCamera cam = canvas.getCamera();

		DudeModel avatar = level.getAvatar();
		if (avatar.getHasBox()) {
			TextureRegion gBox = JsonAssetManager.getInstance().getEntry("boxselect", TextureRegion.class);
			canvas.begin();
			if (level.canPlaceBoxAt()) {
				canvas.draw(gBox, Color.WHITE, gBox.getRegionWidth() / 2, 0,
						level.boxGhost().x * 32, level.boxGhost().y * 32 - 1f / 2 * 32,
						0, 1, 1, 0.9f);
			}else {
				canvas.draw(gBox, Color.RED, gBox.getRegionWidth() / 2, 0,
						level.boxGhost().x * 32, level.boxGhost().y * 32 - 1f / 2 * 32,
						0, 1, 1, 0.9f);
			}
			canvas.end();
		}


		if(failed || complete) {
			TextureRegion overlayTexture = JsonAssetManager.getInstance().getEntry("overlay", TextureRegion.class);
			canvas.begin();
			canvas.draw(overlayTexture, Color.WHITE, overlayTexture.getRegionWidth()/2,
					overlayTexture.getRegionHeight()/2, canvas.getWidth(), canvas.getHeight(), 0, 5, 5, 0.8f);
			canvas.end();
		}
		// Final message
		if (complete && !failed) {
			displayFont.setColor(Color.YELLOW);
			canvas.begin(); // DO NOT SCALE
			TextureRegion texture = JsonAssetManager.getInstance().getEntry("victory", TextureRegion.class);
			canvas.draw(texture,Color.WHITE,texture.getRegionWidth()/2f,texture.getRegionHeight()/2f,cam.position.x,cam.position.y,0,1.5f,1.5f);
//			canvas.drawText("VICTORY!", displayFont, cam.position.x, cam.position.y);
			canvas.end();
		} else if (failed) {
			displayFont.setColor(Color.RED);
			canvas.begin(); // DO NOT SCALE
			TextureRegion texture = JsonAssetManager.getInstance().getEntry("defeat", TextureRegion.class);
			canvas.draw(texture,Color.WHITE,texture.getRegionWidth()/2f,texture.getRegionHeight()/2f,cam.position.x,cam.position.y,0,1.5f,1.5f);
//			canvas.drawText("FAILURE!", displayFont, cam.position.x, cam.position.y);
			canvas.end();
		}

		if (guardCaught!=null && !failed){
			canvas.begin();
			TextureRegion texture = JsonAssetManager.getInstance().getEntry("taseLaser", TextureRegion.class);
			TextureRegion[][] taseTextures = texture.split(10,14);
			float angle = (float)(Math.atan2(
					avatar.getY() - guardCaught.getY(),
					avatar.getX() - guardCaught.getX()
			) + Math.PI/2);
			float length = avatar.getPosition().dst(guardCaught.getPosition())*32;
			canvas.draw(taseTextures[0][0],Color.WHITE,taseTextures[0][0].getRegionWidth()/2f,taseTextures[0][0].getRegionHeight(),
					guardCaught.getX()*32,guardCaught.getY()*32,angle,1f,(length-14)/14);
			canvas.draw(taseTextures[1][0],Color.WHITE,taseTextures[1][0].getRegionWidth()/2f,0,
					guardCaught.getX()*32+(avatar.getX() - guardCaught.getX())*32,guardCaught.getY()*32 + (avatar.getY() - guardCaught.getY())*32,angle,1f,1f);
			if(avatar.getY()-guardCaught.getY()<0){
				avatar.draw(canvas);
			}else{
				guardCaught.draw(canvas);
			}
			canvas.end();
		}

		if (hasObjective && !complete) {
			Alarm alarm = level.getAlarm();
			if(!alarm.isOn()&&!failed){
				alarm.turnOn();
				alarm.start();
			}
			if(!showExit) {
				canvas.begin();
				alarm.draw(canvas);
				canvas.end();
			}
			if(alarm.getTimeLeft()<=0 && !failed){
				setFailure(true);
			}
		}

		if (paused) {
			InputController input = InputController.getInstance();

			TextureRegion overlayTexture = JsonAssetManager.getInstance().getEntry("overlay", TextureRegion.class);
			TextureRegion continueButton = JsonAssetManager.getInstance().getEntry("continue", TextureRegion.class);
			TextureRegion abortButton = JsonAssetManager.getInstance().getEntry("abort", TextureRegion.class);
			TextureRegion musicButton;
			TextureRegion soundButton;


			if (input.didMusic()) {
				music = !music;
			}
			if (input.didSound()) {
				sound = !sound;
			}
			input.resetMusic();
			input.resetSound();

			if (music) {
				musicButton = JsonAssetManager.getInstance().getEntry("musicOn", TextureRegion.class);
				theme.setVolume(0.5f);
				preHeist.setVolume(0.5f);
				postHeist.setVolume(0.5f);
			} else {
				musicButton = JsonAssetManager.getInstance().getEntry("musicOff", TextureRegion.class);
				theme.setVolume(0f);
				preHeist.setVolume(0f);
				postHeist.setVolume(0f);
			}
			if (sound) {
				soundButton = JsonAssetManager.getInstance().getEntry("soundOn", TextureRegion.class);
				for (DoorModel d : level.getDoors()) {
					d.unmute();
				}
				for (Laser l : level.getLasers()) {
					l.unmute();
				}
				for (MoveableBox b : level.getBoxes()) {
					b.unmute();
				}
				if (level.getObjective() != null) {
                    level.getObjective().unmute();
                }
			} else {
				soundButton = JsonAssetManager.getInstance().getEntry("soundOff", TextureRegion.class);
				for (DoorModel d : level.getDoors()) {
					d.mute();
				}
				for (Laser l : level.getLasers()) {
					l.mute();
				}
				for (MoveableBox b : level.getBoxes()) {
					b.mute();
				}
				if (level.getObjective() != null) {
                    level.getObjective().mute();
                }
			}

			canvas.begin();

			Color conTint = input.didContinueHover() ? Color.GRAY : Color.WHITE;
			Color abortTint = input.didAbortHover() ? Color.GRAY : Color.WHITE;
			Color musicTint = input.didMusicHover() ? Color.GRAY : Color.WHITE;
			Color soundTint = input.didSoundHover() ? Color.GRAY : Color.WHITE;

			BitmapFont pauseTitle = JsonAssetManager.getInstance().getEntry("pauseTitle",BitmapFont.class);
			BitmapFont pauseDesc = JsonAssetManager.getInstance().getEntry("pauseDesc",BitmapFont.class);
			pauseTitle.setColor(Color.WHITE);
			pauseDesc.setColor(Color.WHITE);
			String pauseDescription[] = {"Arrow Keys:","Move","Space:","Interact","M:","Minimap","R:","Restart","P:","Pause","Esc:","Exit Game"};

			canvas.draw(overlayTexture, Color.WHITE, overlayTexture.getRegionWidth()/2,
					overlayTexture.getRegionHeight()/2, cam.position.x, cam.position.y, 0, 1, 1, 1f);
//			canvas.draw(pauseButton, Color.WHITE, pauseButton.getWidth()/2, pauseButton.getHeight()/2,
//					cam.position.x,cam.position.y, 0, 0.8f*cam.zoom, 0.8f*cam.zoom);
			canvas.draw(continueButton, conTint, 0, continueButton.getRegionHeight() / 2f,
					cam.position.x + 85*cam.zoom, cam.position.y - 35*cam.zoom, 0, 1f*cam.zoom, 1f*cam.zoom);
			canvas.draw(abortButton, abortTint, 0, abortButton.getRegionHeight() / 2f,
					cam.position.x + 85*cam.zoom, cam.position.y - 100*cam.zoom, 0, 1f*cam.zoom, 1f*cam.zoom);
			canvas.draw(musicButton, musicTint, 0, musicButton.getRegionHeight() / 2f,
					cam.position.x + 85*cam.zoom, cam.position.y + 30*cam.zoom, 0, 1f*cam.zoom, 1f*cam.zoom);
			canvas.draw(soundButton, soundTint, 0, soundButton.getRegionHeight() / 2f,
					cam.position.x + 153*cam.zoom, cam.position.y + 30*cam.zoom, 0, 1f*cam.zoom, 1f*cam.zoom);
			canvas.drawText("PAUSED",pauseTitle,cam.position.x-120,cam.position.y+115);
			canvas.drawText(pauseDescription[0],pauseDesc,cam.position.x-220,cam.position.y+46-0*26);
			canvas.drawText(pauseDescription[2],pauseDesc,cam.position.x-150,cam.position.y+46-1*26);
			canvas.drawText(pauseDescription[4],pauseDesc,cam.position.x-100,cam.position.y+46-2*26);
			canvas.drawText(pauseDescription[6],pauseDesc,cam.position.x-99,cam.position.y+46-3*26);
			canvas.drawText(pauseDescription[8],pauseDesc,cam.position.x-99,cam.position.y+46-4*26);
			canvas.drawText(pauseDescription[10],pauseDesc,cam.position.x-123,cam.position.y+46-5*26);
			for (int i=0; i<pauseDescription.length;i+=2){
				canvas.drawText(pauseDescription[i+1],pauseDesc,cam.position.x-50,cam.position.y+46-i*26/2);
			}
//			canvas.draw(continueButton, conTint, continueButton.getRegionWidth() / 2f, continueButton.getRegionHeight() / 2f,
//					cam.position.x - 10*cam.zoom, cam.position.y - 62*cam.zoom, 0, 1.6f*cam.zoom, 1.58f*cam.zoom);
//			canvas.draw(abortButton, abortTint, abortButton.getRegionWidth() / 2f, abortButton.getRegionHeight() / 2f,
//					cam.position.x - 10*cam.zoom, cam.position.y - 162*cam.zoom, 0, 1.6f*cam.zoom, 1.58f*cam.zoom);
//			canvas.draw(musicButton, musicTint, musicButton.getRegionWidth() / 2f, musicButton.getRegionHeight() / 2f,
//					cam.position.x - 64*cam.zoom, cam.position.y + 53*cam.zoom, 0, 1.6f*cam.zoom, 1.58f*cam.zoom);
//			canvas.draw(soundButton, soundTint, soundButton.getRegionWidth() / 2f, soundButton.getRegionHeight() / 2f,
//					cam.position.x + 46*cam.zoom, cam.position.y + 53*cam.zoom, 0, 1.6f*cam.zoom, 1.58f*cam.zoom);
			canvas.end();
		}


		if ((currentFile==null) || !currentFile.equals(levelSelectFile) && !paused) {
			canvas.begin();
			TextureRegion restartButton = JsonAssetManager.getInstance().getEntry("restart", TextureRegion.class);
			TextureRegion homeButton = JsonAssetManager.getInstance().getEntry("home", TextureRegion.class);
			InputController input = InputController.getInstance();
			Color tint = input.didResetHover() ? Color.GRAY : Color.WHITE;
			Color tint2 = input.didHomeHover() ? Color.GRAY : Color.WHITE;
//		Drawable drawable = new TextureRegionDrawable(texture);
			canvas.draw(restartButton, tint, restartButton.getRegionWidth() / 2f, restartButton.getRegionHeight() / 2f,
					cam.position.x + 350*cam.zoom, cam.position.y + 275*cam.zoom, 0, 1.5f*cam.zoom, 1.5f*cam.zoom);
			canvas.draw(homeButton, tint2, homeButton.getRegionWidth() / 2f, homeButton.getRegionHeight() / 2f,
					cam.position.x + 250*cam.zoom, cam.position.y + 275*cam.zoom, 0, 1.5f*cam.zoom, 1.5f*cam.zoom);
//		ImageButton button = new ImageButton(drawable);
//		button.setPosition(cam.position.x+350, cam.position.y+275);
//		button.setSize(texture.getRegionWidth(), texture.getRegionHeight());
//		InputListener listener = new InputListener() {
//
//		};
//		button.addListener(listener);
			canvas.end();
			if(showMiniMap) {
				miniMap.render(canvas, delta);
			}
		} else if (!paused) {
			canvas.begin();
      		levelselectfont.setColor(Color.GOLDENROD);
//			canvas.drawText("SELECT LEVEL", levelselectfont, cam.position.x-370, cam.position.y-250);
			canvas.drawText("SELECT LEVEL", levelselectfont, cam.position.x-190, cam.position.y-200);

			levelnumfont.setColor(Color.WHITE);
			ArrayList<DoorModel> doors = level.getDoors();
//			for(int i=0;i<doors.size();i++){
			for(int i=0;i<10;i++){
				int f = floatframe/4 + i;
				int t = (f%10>5)?(10-f%10):f%10;
				canvas.drawText(""+(i+1),levelnumfont,doors.get(i).getX()*32-10,doors.get(i).getY()*32+85+(t*3));
			}
			floatframe = (floatframe+1)%40;
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
			InputController input = InputController.getInstance();
			if (!paused) {
				input.resetContinue();
				if (preUpdate(delta)) {
					update(delta);
				}
				draw(delta);
			}
			else {
				input.readInput();
				if (input.didPause() || input.didContinue()) {
					resume();
				} else if (input.didAbort()) {
					input.resetAbort();
					nextFile = Gdx.files.internal("jsons/levelselect.json");
					input.resetHome();
					resume();
				}
				draw(delta);
			}
		}
	}

	public void initMusic() {
		theme = Gdx.audio.newMusic(Gdx.files.internal("music/theme.mp3"));
		theme.setLooping(true);
		theme.setVolume(0.5f);
		preHeist = Gdx.audio.newMusic(Gdx.files.internal("music/pre_heist.mp3"));
		preHeist.setLooping(true);
		preHeist.setVolume(0.5f);
		postHeist = Gdx.audio.newMusic(Gdx.files.internal("music/post_heist.mp3"));
		postHeist.setLooping(true);
		postHeist.setVolume(0.5f);
	}

	public void play(Music song) {
		if (currentSong != null) {
			if (currentSong.isPlaying()) {
				currentSong.stop();
			}
		}
		if (!song.isPlaying()) {
			song.play();
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
		if (!theme.isPlaying()) {
			currentSong.pause();
			play(theme);
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
		if (!currentFile.equals(levelSelectFile)) {
			theme.stop();
			currentSong.play();
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
				//setFailure(true);
				if(!avatar.isElectrocuted()){avatar.electrocute();}
			}

			if((switches.contains(bd1) && bd2==avatar ) || (bd1 == avatar && switches.contains(bd2))){
				if (switches.contains(bd1)) {
					SwitchModel switchi = switches.get(switches.indexOf(bd1));
					switchCollided = switchi;
				} else if (switches.contains(bd2)) {
					SwitchModel switchi = switches.get(switches.indexOf(bd2));
					switchCollided = switchi;
				}
//				TEST
//				nextFile = Gdx.files.internal("jsons/testlevel2.json");
			}

			if((bd1==avatar && bd2 instanceof MoveableBox ) || (bd1 instanceof MoveableBox && bd2==avatar)){
				avatarBoxCollision = true;
				if (bd1 instanceof  MoveableBox) {
					avatar.setBoxInContact((MoveableBox)bd1);
				} else if (bd2 instanceof  MoveableBox) {
					avatar.setBoxInContact((MoveableBox)bd2);
				}
			}

			// Check for objective
			if (((bd1 == avatar && bd2 == objective) || (bd1 == objective && bd2== avatar)) && !hasObjective){
				if(bd1 instanceof ObjectiveModel){
					((ObjectiveModel) bd1).stealCard();
					level.queueDestroyed(bd1);
				}if(bd2 instanceof ObjectiveModel){
					((ObjectiveModel) bd2).stealCard();
					level.queueDestroyed(bd2);
				}
				for (AIController ai : level.getControl()) {
					if (ai.getObPath().length != 0) {
						ai.setPatrol();
						ai.setPath(ai.getObPath());
					}
				}
				level.getObjective().playSteal();
				hasObjective = true;
				exit.open();
				showExit = true;
				for(GuardModel g: guards){
					g.setAlarmed2(true);
				}
			}

			// Check for win condition
			if ((bd1 == avatar && bd2 == exit && hasObjective ) ||
					(bd1 == exit   && bd2 == avatar && hasObjective)) {
				setComplete(true);
			}

//			if (currentFile.equals(levelSelectFile)) {
//				if ((bd1 == avatar && bd2 instanceof DoorModel) || (bd2 == avatar && bd1 instanceof DoorModel)) {
//					nextFile = Gdx.files.internal("jsons/" + (bd1 instanceof DoorModel ? bd1.getName() : bd2.getName()) + ".json");
//				}
//			}

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

		if(bd1 instanceof Laser || bd2 instanceof Laser){
			contact.setEnabled(false);
		}
		if (((bd1 == avatar && bd2 instanceof Laser) || (bd1 instanceof Laser && bd2==avatar)) ||
				((bd1 instanceof MoveableBox && bd2 instanceof Laser) ||
						(bd1 instanceof Laser && bd2 instanceof MoveableBox))){
			contact.setEnabled(false);
			if (bd1 instanceof Laser) {
				if (((Laser) bd1).isTurnedOn()) {
					avatarLaserCollision = true;
					if (boxes.contains(bd2) && bd2 instanceof MoveableBox) {
						return;
					}
					for (AIController ai : level.getControl()) {
						if (ai.getGuard().sector == ((Laser) bd1).sector) {
							ai.setAlarmed();
							ai.setProtect(bd1);
							if (music) {
								currentSong.setVolume(DEFAULT_VOL/3);
							}
							((Laser) bd1).playAlarm();
						}
					}
					if (!boxes.contains(bd2) && bd2 instanceof MoveableBox) {
						boxes.add(bd2);
					}
				}
				else {
					avatarLaserCollision = false;
				}
			}
			else {
				if (((Laser) bd2).isTurnedOn()) {
					avatarLaserCollision = true;
					if (boxes.contains(bd1) && bd1 instanceof MoveableBox) {
						return;
					}
					for (AIController ai : level.getControl()) {
						if (ai.getGuard().sector == ((Laser) bd2).sector) {
							ai.setAlarmed();
							ai.setProtect(bd2);
							if (music) {
								currentSong.setVolume(DEFAULT_VOL/3);
							}
							((Laser) bd2).playAlarm();
						}
					}
					if (!boxes.contains(bd1) && bd1 instanceof MoveableBox) {
						boxes.add(bd1);
					}
				}
				else {
					avatarLaserCollision = false;
				}
			}
		}
	}
}
