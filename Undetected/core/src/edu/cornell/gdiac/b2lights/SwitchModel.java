package edu.cornell.gdiac.b2lights;

import java.util.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.lang.reflect.Field;

public class SwitchModel extends BoxObstacle{
    private static final float SWITCH_SIZE = 1f;
    private boolean switched = false;
    private TextureRegion switchOffTexture;
    private TextureRegion switchOnTexture;
    private boolean flaggedForDelete;
    private ArrayList<DoorModel> doors = new ArrayList<DoorModel>();

    public SwitchModel(float x, float y) {
        super(x, y, SWITCH_SIZE, SWITCH_SIZE);
        setSensor(true);
    }

    public SwitchModel() {
        super(SWITCH_SIZE, SWITCH_SIZE);
        setSensor(true);
    }

    public boolean getSwitched() {
        return switched;
    }

    public void setSwitch(boolean value) {
        switched = value;
        TextureRegion offTexture = JsonAssetManager.getInstance().getEntry("switchOff", TextureRegion.class);
        switchOffTexture = offTexture;
        TextureRegion onTexture = JsonAssetManager.getInstance().getEntry("switchOn", TextureRegion.class);
        switchOnTexture = onTexture;
        if (switched) {
            setTexture(switchOnTexture);
        } else {
            setTexture(switchOffTexture);
        }
        setOrigin(origin.x, 0);
    }

    public void switchMode() {
        switched = !switched;
        if (doors != null) {
            for (DoorModel door : doors) {
                door.switchState();
            }
            if (switched) {
                setTexture(switchOnTexture);
            } else {
                setTexture(switchOffTexture);
            }
            setOrigin(origin.x, 0);
        }
    }

    public void addDoor(DoorModel door) {
        doors.add(door);
    }

    public ArrayList<DoorModel> getDoors() {
        return doors;
    }

    public void setFlaggedForDelete () {
        flaggedForDelete = true;
    }

    public boolean isFlaggedForDelete () {
        return flaggedForDelete;
    }

    public void initialize(){
        TextureRegion offTexture = JsonAssetManager.getInstance().getEntry("switchOff", TextureRegion.class);
        switchOffTexture = offTexture;
        TextureRegion onTexture = JsonAssetManager.getInstance().getEntry("switchOn", TextureRegion.class);
        switchOnTexture = onTexture;
        if (!switched) {
            setTexture(offTexture);
        } else {
            setTexture(onTexture);
        }
        setOrigin(origin.x, 0);
        setBodyType(BodyDef.BodyType.StaticBody);
    }

    public void initialize(JsonValue json){
        setName(json.name());
//        int[] pos  = json.get("pos").asIntArray();
        float[] size = {1, 1};
//        setPosition(pos[0]+0.5f,pos[1]+0.5f);
        setWidth(size[0]);
        setHeight(size[1]);
        setFixedRotation(true);

        // Technically, we should do error checking here.
        // A JSON field might accidentally be missing
        setBodyType(BodyDef.BodyType.StaticBody);



        // Create the collision filter (used for light penetration)
        short collideBits = LevelModel.bitStringToShort("1000");
        short excludeBits = LevelModel.bitStringToComplement("0000");
        Filter filter = new Filter();
        filter.categoryBits = collideBits;
        filter.maskBits = excludeBits;
        setFilterData(filter);

        // Reflection is best way to convert name to color
        Color debugColor;
        try {
            String cname = "YELLOW";
            Field field = Class.forName("com.badlogic.gdx.graphics.Color").getField(cname);
            debugColor = new Color((Color)field.get(null));
        } catch (Exception e) {
            debugColor = null; // Not defined
        }
        int opacity = 200;
        debugColor.mul(opacity/255.0f);
        setDebugColor(debugColor);

        // Now get the texture from the AssetManager singleton
        TextureRegion offTexture = JsonAssetManager.getInstance().getEntry("switchOff", TextureRegion.class);
        switchOffTexture = offTexture;
        TextureRegion onTexture = JsonAssetManager.getInstance().getEntry("switchOn", TextureRegion.class);
        switchOnTexture = onTexture;
        if (!switched) {
            setTexture(offTexture);
        } else {
            setTexture(onTexture);
        }
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
