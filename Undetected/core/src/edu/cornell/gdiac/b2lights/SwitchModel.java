package edu.cornell.gdiac.b2lights;

import java.util.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.lang.reflect.Field;

public class SwitchModel extends BoxObstacle{
    private static final float SWITCH_SIZE = 1f;
    private boolean switched = false;
    private boolean animateOn = false;
    private boolean animateOff = false;
    private int animateCool = 3;
    private TextureRegion switchOffTexture;
    private TextureRegion switchOnTexture;
    private FilmStrip filmstrip;
    private boolean flaggedForDelete;
    private ArrayList<Laser> lasers = new ArrayList<>();
    private ArrayList<DoorModel> doors = new ArrayList<>();
    private ArrayList<CameraModel> cameras = new ArrayList<CameraModel>();

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
    }

    public void switchMode() {
        switched = !switched;
        if (doors != null) {
            for (DoorModel door : doors) {
                door.switchState();
            }
            setOrigin(origin.x, 0);
        }
        if (lasers != null) {
            for (Laser las : lasers) {
                if (switched) {
                    if (!las.isTurnedOn()) {
                        las.setOn(true);
                    }
                } else {
                    las.setOn(false);
                }
            }
            setOrigin(origin.x, 0);
        }
        if (cameras != null) {
            for (CameraModel cam : cameras) {
                cam.toggle();
            setOrigin(origin.x, 0);
            }
        }
        if (switched) {
//                setTexture(switchOnTexture);
//                filmstrip.setFrame(0);
            animateOn = true;
            animateOff = false;
        } else {
//                setTexture(switchOffTexture);
//                filmstrip.setFrame(9);
            animateOff = true;
            animateOn = false;
        }
//        System.out.println("animate on: " + animateOn + "\nanimate off: " + animateOff);
    }

    public void addLaser(Laser laser) {
        lasers.add(laser);
    }

    public void addDoor(DoorModel door) {
        doors.add(door);
    }
    public void addCamera(CameraModel camera){
        cameras.add(camera);
    }

    public ArrayList<Laser> getLasers() {
        return lasers;
    }

    public ArrayList<DoorModel> getDoors() {
        return doors;
    }
    public ArrayList<CameraModel> getCameras() {
        return cameras;
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
        short collideBits = (short)0x0040;
        short excludeBits = LevelModel.bitStringToComplement("0000000000000000");
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
        texture = JsonAssetManager.getInstance().getEntry("switchAnimation", TextureRegion.class);
        try {
            filmstrip = new FilmStrip(texture.getTexture(), 1, 4);
        } catch (Exception e) {
            filmstrip = null;
        }
        setTexture(filmstrip);

        if (!switched) {
//            setTexture(offTexture);
            filmstrip.setFrame(3);
        } else {
//            setTexture(onTexture);
            filmstrip.setFrame(0);
        }
        setOrigin(origin.x, 0);

    }

    public void update(float dt){
        if(animateCool!=0){
            animateCool--;
            return;
        }
        if (animateOff){
            if(filmstrip.getFrame()>=3) {
                animateOff = false;
            }else{
                filmstrip.setFrame(filmstrip.getFrame()+1);
            }
        }
        if (animateOn){
            if(filmstrip.getFrame()<=0) {
                animateOn = false;
            }else{
                filmstrip.setFrame(filmstrip.getFrame()-1);
            }
        }
        animateCool = 3;
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(ObstacleCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y-getHeight()/4*drawScale.y,getAngle(),1.0f,1.0f);
        }
    }

}
