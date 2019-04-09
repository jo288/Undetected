package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.lang.reflect.Field;

public class DoorModel extends BoxObstacle{
    private static final float DOOR_WIDTH = 1f;
    private static final float DOOR_HEIGHT = 1f;
    private boolean open = false;
    private TextureRegion doorTexture;
    private boolean flaggedForDelete;

    public DoorModel(float x, float y) {
        super(x, y, DOOR_WIDTH, DOOR_HEIGHT);
    }

    public DoorModel() {
        super(DOOR_WIDTH, DOOR_HEIGHT);
    }

    public boolean getOpen() {
        return open;
    }

    public void setOpen(boolean val) {
        open = val;
        if (open) {
            setSensor(true);
        } else {
            setSensor(false);
        }
    }

    public void switchState() {
        open = !open;
        setSensor(open);
    }

    public void setFlaggedForDelete () {
        flaggedForDelete = true;
    }

    public boolean isFlaggedForDelete () {
        return flaggedForDelete;
    }

    public void initialize(){
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("door", TextureRegion.class);
        doorTexture = texture;
        setTexture(texture);
        setBodyType(BodyDef.BodyType.StaticBody);
    }

    public void initialize(JsonValue json){
        setName(json.name());
//        int[] pos  = json.get("pos").asIntArray();
        float[] size = json.get("size").asFloatArray();
//        setPosition(pos[0]+0.5f,pos[1]+0.5f);
        setWidth(size[0]);
        setHeight(size[1]);
        setFixedRotation(true);

        // Technically, we should do error checking here.
        // A JSON field might accidentally be missing
        setBodyType(json.get("bodytype").asString().equals("static") ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody);
        setDensity(json.get("density").asFloat());
        setFriction(json.get("friction").asFloat());
        setRestitution(json.get("restitution").asFloat());

        // Create the collision filter (used for light penetration)
        short collideBits = LevelModel.bitStringToShort(json.get("collideBits").asString());
        short excludeBits = LevelModel.bitStringToComplement(json.get("excludeBits").asString());
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
