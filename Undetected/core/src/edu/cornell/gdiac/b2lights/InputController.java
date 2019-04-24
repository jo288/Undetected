/*
 * InputController.java
 *
 * This class buffers in input from the devices and converts it into its
 * semantic meaning. If your game had an option that allows the player to
 * remap the control keys, you would store this information in this class.
 * That way, the main GameEngine does not have to keep track of the current
 * key mapping.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * B2Lights version, 3/12/2016
 */
package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.*;

import edu.cornell.gdiac.util.XBox360Controller;

import java.awt.event.InputEvent;

/**
 * Class for reading player input. 
 *
 * This supports both a keyboard and X-Box controller. In previous solutions, we only 
 * detected the X-Box controller on start-up.  This class allows us to hot-swap in
 * a controller via the new XBox360Controller class.
 */
public class InputController {
	/** The singleton instance of the input controller */
	private static InputController theController = null;
	
	/** 
	 * Return the singleton instance of the input controller
	 *
	 * @return the singleton instance of the input controller
	 */
	public static InputController getInstance() {
		if (theController == null) {
			theController = new InputController();
		}
		return theController;
	}
	
	// Fields to manage buttons
	/** Whether the reset button was pressed. */
	private boolean resetPressed;
	private boolean resetPrevious;
	/** Whether the button to advanced worlds was pressed. */
	private boolean nextPressed;
	private boolean nextPrevious;
	/** Whether the button to step back worlds was pressed. */
	private boolean pausePressed;
	private boolean pausePrevious;
	/** Whether the load button was pressed. */
	private boolean loadPressed;
	private boolean loadPrevious;
	/** Whether the debug toggle was pressed. */
	private boolean debugPressed;
	private boolean debugPrevious;
	/** Whether the exit button was pressed. */
	private boolean exitPressed;
	private boolean exitPrevious;
	/** Whether the action button was pressed. */
	private boolean actionPressed;
	private boolean actionPrevious;
	/** Whether hot key for changing guard's line of sight was pressed */
	private boolean incViewPressed;
	private boolean decViewPressed;
	/**Whether hot keys for translating camera was pressed */
	private boolean cameraUp;
	private boolean cameraLeft;
	private boolean cameraDown;
	private boolean cameraRight;
	private boolean zoomIn;
	private boolean zoomOut;

	
	/** How much did we move horizontally? */
	private float horizontal;
	/** How much did we move vertically? */
	private float vertical;

	/**manual controls for guard*/
	private float horizontalG;
	private float verticalG;

	private float holdSpace;
	
	/** An X-Box controller (if it is connected) */
	XBox360Controller xbox;
	
	/**
	 * Returns the amount of sideways movement. 
	 *
	 * -1 = left, 1 = right, 0 = still
	 *
	 * @return the amount of sideways movement. 
	 */
	public float getHorizontal() {
		return horizontal;
	}
	
	/**
	 * Returns the amount of vertical movement. 
	 *
	 * -1 = down, 1 = up, 0 = still
	 *
	 * @return the amount of vertical movement. 
	 */
	public float getVertical() {
		return vertical;
	}

	/**
	 * Returns amount of horizontal movement for guard
	 */
	public float getHorizontalG() { return horizontalG; }

	/**
	 * Returns amount of vertical movement for guard
	 */
	public float getVerticalG() { return verticalG; }
	/**
	 * Returns true if the reset button was pressed.
	 *
	 * @return true if the reset button was pressed.
	 */
	public boolean didReset() {
		return resetPressed && !resetPrevious;
	}

	/**
	 * Returns true if the player wants to go to the next level.
	 *
	 * @return true if the player wants to go to the next level.
	 */
	public boolean didForward() {
		return nextPressed && !nextPrevious;
	}
	
	/**
	 * Returns true if the player wants to go to the previous level.
	 *
	 * @return true if the player wants to go to the previous level.
	 */
	public boolean didPause() {
		return pausePressed && !pausePrevious;
	}
	/**
	 * Returns true if the player wants to go to the previous level.
	 *
	 * @return true if the player wants to go to the previous level.
	 */
	public boolean didLoad() {
		return loadPressed && !loadPrevious;
	}
	
	/**
	 * Returns true if the player wants to go toggle the debug mode.
	 *
	 * @return true if the player wants to go toggle the debug mode.
	 */
	public boolean didDebug() {
		return debugPressed && !debugPrevious;
	}
	
	/**
	 * Returns true if the exit button was pressed.
	 *
	 * @return true if the exit button was pressed.
	 */
	public boolean didExit() {
		return exitPressed && !exitPrevious;
	}

	/**
	 * Returns true if the reset button is being hovered.
	 *
	 * @return true if the reset button is being hovered.
	 */
	public boolean didResetHover() {
		return Gdx.input.getX() >= 710 && Gdx.input.getX() <= 800 && Gdx.input.getY() >= 5 && Gdx.input.getY() <= 45;
	}

	/**
	 * Returns whether the space key is being held down or not.
	 *
	 * 1 = yes, 0 = no
	 *
	 * @return state of space key.
	 */
//	public float getHoldSpace() {
//		return holdSpace;
//	}

	/**
	 * Returns true if the action button was pressed.
	 *
	 * @return true if the action button was pressed.
	 */
	public boolean didAction() {
		return actionPressed && !actionPrevious;
	}

	/**
	 * Returns true if Z is pressed (for increasing guard's field of view
	 */
	public boolean increaseView(){ return incViewPressed;}
	public boolean decreaseView(){ return decViewPressed;}

	public boolean moveCamUp(){ return cameraUp;}
	public boolean moveCamRight(){ return cameraRight;}
	public boolean moveCamDown(){ return cameraDown;}
	public boolean moveCamKLeft(){ return cameraLeft; }
	public boolean zoomIn(){ return zoomIn;}
	public boolean zoomOut(){return zoomOut;}
	
	/**
	 * Creates a new input controller
	 * 
	 * The input controller attempts to connect to the X-Box controller at device 0,
	 * if it exists.  Otherwise, it falls back to the keyboard control.
	 */
	public InputController() { 
		// If we have a game-pad for id, then use it.
		xbox = new XBox360Controller(0);
	}

	/**
	 * Reads the input for the player and converts the result into game logic.
	 */
	public void readInput() {
		// Copy state from last animation frame
		// Helps us ignore buttons that are held down
		resetPrevious  = resetPressed;
		debugPrevious  = debugPressed;
		exitPrevious = exitPressed;
		nextPrevious = nextPressed;
		pausePrevious = pausePressed;
		loadPrevious = loadPressed;
		actionPrevious = actionPressed;

		// Check to see if a GamePad is connected
		if (xbox.isConnected()) {
			readGamepad();
			readKeyboard(true); // Read as a back-up
		} else {
			readKeyboard(false);
		}
}

	/**
	 * Reads input from an X-Box controller connected to this computer.
	 *
	 * The method provides both the input bounds and the drawing scale.  It needs
	 * the drawing scale to convert screen coordinates to world coordinates.  The
	 * bounds are for the crosshair.  They cannot go outside of this zone.
	 *
	 * @param bounds The input bounds for the crosshair.  
	 * @param scale  The drawing scale
	 */
	private void readGamepad() {
		resetPressed = xbox.getStart();
		exitPressed  = xbox.getBack();
		nextPressed  = xbox.getRB();
		pausePressed  = xbox.getLB();
		debugPressed  = xbox.getY();
		actionPressed = xbox.getA();

		// Increase animation frame, but only if trying to move
		horizontal = xbox.getLeftX();
		vertical   = xbox.getLeftY();
	}

	/**
	 * Reads input from the keyboard.
	 *
	 * This controller reads from the keyboard regardless of whether or not an X-Box
	 * controller is connected.  However, if a controller is connected, this method
	 * gives priority to the X-Box controller.
	 *
	 * @param secondary true if the keyboard should give priority to a gamepad
	 */
	private void readKeyboard(boolean secondary) {
		// Give priority to gamepad results
		resetPressed = (secondary && resetPressed) || (Gdx.input.isKeyPressed(Input.Keys.R));
		debugPressed = (secondary && debugPressed) || (Gdx.input.isKeyPressed(Input.Keys.D));
		pausePressed = (secondary && pausePressed) || (Gdx.input.isKeyPressed(Input.Keys.P));
		loadPressed = (secondary && loadPressed) || (Gdx.input.isKeyPressed(Input.Keys.L));
		nextPressed = (secondary && nextPressed) || (Gdx.input.isKeyPressed(Input.Keys.N));
		exitPressed  = (secondary && exitPressed) || (Gdx.input.isKeyPressed(Input.Keys.ESCAPE));
		actionPressed = (secondary && actionPressed) || (Gdx.input.isKeyPressed(Input.Keys.SPACE));
		incViewPressed = (secondary && incViewPressed) || (Gdx.input.isKeyPressed(Input.Keys.Z));
		decViewPressed = (secondary && decViewPressed) || (Gdx.input.isKeyPressed(Input.Keys.X));
		cameraUp = (secondary && cameraUp) || (Gdx.input.isKeyPressed(Input.Keys.I));
		cameraLeft = (secondary && cameraLeft) || (Gdx.input.isKeyPressed(Input.Keys.J));
		cameraDown = (secondary && cameraDown) || (Gdx.input.isKeyPressed(Input.Keys.K));
		cameraRight = (secondary && actionPressed) || (Gdx.input.isKeyPressed(Input.Keys.L));
		zoomIn = (secondary && zoomIn) || (Gdx.input.isKeyPressed(Input.Keys.NUM_1));
		zoomOut = (secondary && zoomOut) || (Gdx.input.isKeyPressed(Input.Keys.NUM_2));

		// Directional controls
		horizontal = (secondary ? horizontal : 0.0f);
		horizontalG = (secondary ? horizontalG: 0.0f);
		if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			horizontal += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.D)) {
			horizontalG += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			horizontal -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			horizontalG -= 1.0f;
		}

//		if (Gdx.input.isKeyPressed(Input.Keys.SPACE)) {
//			holdSpace += 1.0f;
//		}
		
		vertical = (secondary ? vertical : 0.0f);
		verticalG = (secondary ? verticalG : 0.0f);
		if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
			vertical += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			verticalG += 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			vertical -= 1.0f;
		}
		if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			verticalG -= 1.0f;
		}

		if (didResetHover() && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
			resetPressed = true;
			resetPrevious = false;
		}
	}
}