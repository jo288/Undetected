package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import java.lang.reflect.*;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.physics.obstacle.*;

import javax.xml.soap.Text;

public class MoveableBox extends BoxObstacle{
    /** Collide Bit */
    public static final String COLLIDE_BIT = "0010";
    /** Default Width of Player */
    public static final String EXCLUDE_BIT = "0000";

    private static final float BOX_SIZE = 0.5f;
    private boolean held = false;
    private TextureRegion boxTexture;
    private boolean flaggedForDelete;

    public MoveableBox(float x, float y) {
        super(x, y, BOX_SIZE, BOX_SIZE);
    }

    public MoveableBox() {
        super(BOX_SIZE, BOX_SIZE);
    }

    public boolean getHeld() {
        return held;
    }

    public void setHeld(boolean value) {
        held = value;
    }

    public void setFlaggedForDelete () {
        flaggedForDelete = true;
    }

    public boolean isFlaggedForDelete () {
        return flaggedForDelete;
    }

    public void initialize(){
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("box", TextureRegion.class);
        boxTexture = texture;
        setTexture(texture);
        setBodyType(BodyDef.BodyType.StaticBody);
    }

    public void initialize(JsonValue json){
        setName(json.name());
        int[] pos  = json.get("pos").asIntArray();
//        float[] size = json.get("size").asFloatArray();
        setPosition(pos[0]+0.5f,pos[1]+0.5f);
        setWidth(1);
        setHeight(1);
        setFixedRotation(true);

        // Technically, we should do error checking here.
        // A JSON field might accidentally be missing
        setBodyType(BodyDef.BodyType.DynamicBody);
        setDensity(999999999.0f);
        setFriction(0);
        setRestitution(0);

        // Create the collision filter (used for light penetration)
        short collideBits = LevelModel.bitStringToShort(COLLIDE_BIT);
        short excludeBits = LevelModel.bitStringToComplement(EXCLUDE_BIT);
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        setFilterData(filter);

        // Reflection is best way to convert name to color
        Color debugColor;
        try {
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField("YELLOW");
            debugColor = new Color((Color)field.get(null));
        } catch (Exception e) {
            debugColor = null; // Not defined
        }
        int opacity = 200;
        debugColor.mul(opacity/255.0f);
        setDebugColor(debugColor);

        // Now get the texture from the AssetManager singleton
        String key = json.get("texture").asString();
        TextureRegion texture = JsonAssetManager.getInstance().getEntry(key, TextureRegion.class);
        setTexture(texture);
        setOrigin(origin.x, 0);
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(ObstacleCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),1.0f,1.0f);
        }
    }

}
