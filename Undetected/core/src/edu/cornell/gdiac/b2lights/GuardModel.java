/*
 * DudeModel.java
 *
 * This is a refactored version of DudeModel that allows us to read its properties
 * from a JSON file.  As a result, it has a lot more getter and setter "hooks" than
 * in lab.
 *
 * While the dude can support lights, these are completely decoupled from this object.
 * The dude is not aware of any of the lights. These are attached to the associated
 * body and move with the body.
 *
 * Author: Walker M. White
 * Based on original PhysicsDemo Lab by Don Holden, 2007
 * B2Lights version, 3/12/2016
 */
package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import java.lang.reflect.*;

import edu.cornell.gdiac.physics.lights.ConeSource;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.physics.obstacle.*;

import static edu.cornell.gdiac.b2lights.LevelModel.bitStringToComplement;

/**
 * Player avatar for the plaform game.
 *
 * Note that the constructor does very little.  The true initialization happens
 * by reading the JSON value.
 */
public class GuardModel extends CharacterModel {
    // Physics constants
    /** The factor to multiply by the input */
    private float force;
    /** The amount to slow the character down */
    private float damping;
    /** The maximum character speed */
    private float maxspeed;

    // Character states
    /** Whether Guard is Active */
    private boolean isActive;
    /** Whether Guard is Alarmed */
    private boolean isAlarmed;

    /** The current horizontal movement of the character */
    private Vector2 movement = new Vector2();
    /** Whether or not to animate the current frame */
    private boolean animate = false;

    /** How many frames until we can walk again */
    private int walkCool;
    /** The standard number of frames to wait until we can walk again */
    private int walkLimit;

    /** Texture of character */
    private TextureRegion defaultCharTexture;

    /** FilmStrip pointer to the texture region */
    private FilmStrip filmstrip;
    /** The current animation frame of the avatar */
    private int startFrame;

    /** Cache for internal force calculations */
    private Vector2 forceCache = new Vector2();

    /** Direction of character */
    private Vector2 direction;

    /** Line of sight of guard */
    private ConeSource light;

    /**
     * Returns the directional movement of this character.
     *
     * This is the result of input times dude force.
     *
     * @return the directional movement of this character.
     */
    public Vector2 getMovement() {
        return movement;
    }

    /**
     * Sets the directional movement of this character.
     *
     * This is the result of input times dude force.
     *
     * @param value the directional movement of this character.
     */
    public void setMovement(Vector2 value) {
        setMovement(value.x,value.y);
    }

    /**
     * Modifies the Guard's X movement
     *
     */
    public void changeX(float speed) {
        setMovement(movement.x + speed, movement.y);
    }

    /**
     * Modifies the Guard's Y movement
     *
     */
    public void changeY(float speed) {
        setMovement(movement.x, movement.y + speed);
    }

    /**
     * Returns whether or not the Guard is Active
     *
     */
    public boolean getActive() {
        return isActive;
    }

    /**
     * Sets whether the Guard is Active or not
     *
     */
    public void setActive(boolean value) {
        isActive = value;
    }

    /**
     * Returns whether or not the Guard is Alarmed
     *
     */
    public boolean getAlarmed() {
        return isAlarmed;
    }

    /**
     * Sets whether the guard is Alarmed or not
     *
     */
    public void setAlarmed(boolean value) {
        isAlarmed = value;
    }

    /**
     * Sets the directional movement of this character.
     *
     * This is the result of input times dude force.
     *
     * @param dx the horizontal movement of this character.
     * @param dy the horizontal movement of this character.
     */
    public void setMovement(float dx, float dy) {
        movement.set(dx,dy);
    }

    public Vector2 getDirection(){ return direction; }

    public void setDirection(float angle){
        float adjustedAng = angle+(float)Math.PI/2.0f;
        this.light.setDirection(adjustedAng*MathUtils.radiansToDegrees);
        direction = new Vector2((float)Math.cos(adjustedAng), (float)Math.sin(adjustedAng));
    }
    public void setDirection(Vector2 dir){ direction = dir; }
    public void setDirection(float x, float y) { direction = new Vector2(x, y);}

    /**
     * Returns the guard's light
     */
    public ConeSource getLight(){
        return light;
    }

    /**
     * Returns how much force to apply to get the dude moving
     *
     * Multiply this by the input to get the movement value.
     *
     * @return how much force to apply to get the dude moving
     */
    public float getForce() {
        return force;
    }

    /**
     * Sets how much force to apply to get the dude moving
     *
     * Multiply this by the input to get the movement value.
     *
     * @param value	how much force to apply to get the dude moving
     */
    public void setForce(float value) {
        force = value;
    }

    /**
     * Returns how hard the brakes are applied to get a dude to stop moving
     *
     * @return how hard the brakes are applied to get a dude to stop moving
     */
    public float getDamping() {
        return damping;
    }

    /**
     * Sets how hard the brakes are applied to get a dude to stop moving
     *
     * @param value	how hard the brakes are applied to get a dude to stop moving
     */
    public void setDamping(float value) {
        damping = value;
    }

    /**
     * Returns the upper limit on dude left-right movement.
     *
     * This does NOT apply to vertical movement.
     *
     * @return the upper limit on dude left-right movement.
     */
    public float getMaxSpeed() {
        return maxspeed;
    }

    /**
     * Sets the upper limit on dude left-right movement.
     *
     * This does NOT apply to vertical movement.
     *
     * @param value	the upper limit on dude left-right movement.
     */
    public void setMaxSpeed(float value) {
        maxspeed = value;
    }

    /**
     * Returns the current animation frame of this dude.
     *
     * @return the current animation frame of this dude.
     */
    public float getStartFrame() {
        return startFrame;
    }

    /**
     * Sets the animation frame of this dude.
     *
     * @param value	animation frame of this dude.
     */
    public void setStartFrame(int value) {
        startFrame = value;
    }

    /**
     * Returns the cooldown limit between walk animations
     *
     * @return the cooldown limit between walk animations
     */
    public int getWalkLimit() {
        return walkLimit;
    }

    /**
     * Sets the cooldown limit between walk animations
     *
     * @param value	the cooldown limit between walk animations
     */
    public void setWalkLimit(int value) {
        walkLimit = value;
    }

    /**
     * Creates a new dude with degenerate settings
     *
     * The main purpose of this constructor is to set the initial capsule orientation.
     */
    public GuardModel() {
        super(1,1,1,1);
        direction = new Vector2(0, 1);
        setFixedRotation(false);
    }

    /**
     * Creates a new dude with degenerate settings
     *
     * The main purpose of this constructor is to set the initial capsule orientation.
     */
    public GuardModel(float x, float y,float width, float height) {
        super(x,y,width, height);
        setFixedRotation(false);
    }

    /**
     * Initializes the dude via the given JSON value
     *
     * The JSON value has been parsed and is part of a bigger level file.  However,
     * this JSON value is limited to the dude subtree
     *
     * @param json	the JSON subtree defining the dude
     */
    public void initialize(JsonValue json) {
        setName(json.name());
        float width = json.get("width").asFloat();
        float height = json.get("height").asFloat();
        float[] pos = json.get("pos").asFloatArray();
        setPosition(pos[0]+0.5f,pos[1]+0.5f);
        setWidth(width);
        setHeight(height/2);
        setFixedRotation(true);
        setActive(false);
        setAlarmed(false);

        // Technically, we should do error checking here.
        // A JSON field might accidentally be missing
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setDensity(json.get("density").asFloat());
        setFriction(json.get("friction").asFloat());
        setRestitution(json.get("restitution").asFloat());
        setForce(json.get("force").asFloat());
        setDamping(json.get("damping").asFloat());
        setMaxSpeed(json.get("maxspeed").asFloat());
        setStartFrame(json.get("startframe").asInt());
        setWalkLimit(json.get("walklimit").asInt());

        // Create the collision filter (used for light penetration)
        short collideBits = LevelModel.bitStringToShort(json.get("collideBits").asString());
        short excludeBits = bitStringToComplement(json.get("excludeBits").asString());
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        setFilterData(filter);

        // Reflection is best way to convert name to color
        Color debugColor;
        try {
            String cname = json.get("debugcolor").asString().toUpperCase();
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color)field.get(null));
        } catch (Exception e) {
            debugColor = null; // Not defined
        }
        int opacity = json.get("debugopacity").asInt();
        debugColor.mul(opacity/255.0f);
        setDebugColor(debugColor);


        // Now get the texture from the AssetManager singleton
        String key = json.get("texture").asString();
        TextureRegion texture = JsonAssetManager.getInstance().getEntry(key, TextureRegion.class);
        defaultCharTexture = texture;
        setTexture(texture);
        setOrigin(origin.x,0);
    }


    /**
     * Applies the force to the body of this dude
     *
     * This method should be called after the force attribute is set.
     */
    public void applyForce() {
        if (!isActive()) {
            return;
        }

        // Only walk or spin if we allow it
        setLinearVelocity(Vector2.Zero);
        setAngularVelocity(0.0f);

        // Apply force for movement
        if (getMovement().len2() > 0f) {
            forceCache.set(getMovement());
            body.applyForce(forceCache,getPosition(),true);
//            body.setLinearVelocity(forceCache.x/16.8f, forceCache.y/16.8f);
            animate = true;
        } else {
            animate = false;
        }
    }

    /**
     * adds a light to this guard
     */
    public void addLight(ConeSource light){
        this.light = light;
    }

    /**
     * Updates the object's physics state (NOT GAME LOGIC).
     *
     * We use this method to reset cooldowns.
     *
     * //@param delta Number of seconds since last animation frame
     */
    public void update(float dt) {
        // Animate if necessary
        if (animate && walkCool == 0) {
            if (filmstrip != null) {
                int next = (filmstrip.getFrame()+1) % filmstrip.getSize();
                filmstrip.setFrame(next);
            }
            walkCool = walkLimit;
        } else if (walkCool > 0) {
            walkCool--;
        } else if (!animate) {
            if (filmstrip != null) {
                filmstrip.setFrame(startFrame);
            }
            walkCool = 0;
        }

        super.update(dt);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(ObstacleCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/2f*drawScale.y,0f,0.15f,0.15f);
        }
    }
}