package edu.cornell.gdiac.b2lights;

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

public class DoorModel extends BoxObstacle{
    private static final float DOOR_WIDTH = 1f;
    private static final float DOOR_HEIGHT = 1f;
    private boolean open = false;
    private boolean animateOn = false;
    private boolean animateOff = false;
    private TextureRegion closedDoorTexture;
    private TextureRegion openDoorTexture;
    private short openCategoryBits = (short)0x0010;
    private short openMaskBits = (short)0x0000;
    private short closedMaskBits = (short)0xffff;
    private short closedCategoryBits = (short)0x0020;
    private boolean flaggedForDelete;
    private FilmStrip filmstrip;

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
        TextureRegion closedTexture = JsonAssetManager.getInstance().getEntry("doorClosed", TextureRegion.class);
        closedDoorTexture = closedTexture;
        TextureRegion openTexture = JsonAssetManager.getInstance().getEntry("doorOpen", TextureRegion.class);
        openDoorTexture = openTexture;
        Filter f = this.getFilterData();
        if (open) {
            setTexture(openDoorTexture);
            f.categoryBits = openCategoryBits;
            f.maskBits = openMaskBits;
        } else {
            setTexture(closedDoorTexture);
            f.categoryBits = closedCategoryBits;
            f.maskBits = closedMaskBits;
        }
        setFilterData(f);
        System.out.println(open+" category bits "+f.categoryBits+" mask "+f.maskBits);
        setOrigin(origin.x, 0);
    }

    public void switchState() {
        open = !open;
        Filter f = getFilterData();
        if (open) {
//            setTexture(openDoorTexture);
            animateOn = true;
            animateOff = false;
            f.categoryBits = openCategoryBits;
            f.maskBits = openMaskBits;
        } else {
//            setTexture(closedDoorTexture);
            animateOff = true;
            animateOn = false;
            f.categoryBits = closedCategoryBits;
            f.maskBits = closedMaskBits;
        }
        setFilterData(f);
        System.out.println(open+" category bits "+f.categoryBits+" mask "+f.maskBits);
        setOrigin(origin.x, 0);
    }

    public void setFlaggedForDelete () {
        flaggedForDelete = true;
    }

    public boolean isFlaggedForDelete () {
        return flaggedForDelete;
    }

    public void initialize(){
        TextureRegion closedTexture = JsonAssetManager.getInstance().getEntry("doorClosed", TextureRegion.class);
        closedDoorTexture = closedTexture;
        TextureRegion openTexture = JsonAssetManager.getInstance().getEntry("doorOpen", TextureRegion.class);
        openDoorTexture = openTexture;
        if (!open) {
            setTexture(closedTexture);
        } else {
            setTexture(openTexture);
        }
        setOrigin(origin.x, 0);
        setBodyType(BodyDef.BodyType.StaticBody);
    }

    public void initialize(JsonValue json){
//        setName(json.name());
        setName(json.get("name").asString());
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
       /* short collideBits = LevelModel.bitStringToShort("0010");
        short excludeBits = LevelModel.bitStringToComplement("0000");*/

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
        TextureRegion closedTexture = JsonAssetManager.getInstance().getEntry("doorClosed", TextureRegion.class);
        closedDoorTexture = closedTexture;
        TextureRegion openTexture = JsonAssetManager.getInstance().getEntry("doorOpen", TextureRegion.class);
        openDoorTexture = openTexture;
        texture = JsonAssetManager.getInstance().getEntry("greendoor", TextureRegion.class);
        try {
            filmstrip = (FilmStrip)texture;
        } catch (Exception e) {
            filmstrip = null;
        }

        Filter filter = new Filter();
        if (!open) {
//            setTexture(closedTexture);
            filmstrip.setFrame(0);

            setTexture(filmstrip);
            filter.categoryBits = closedCategoryBits;
            filter.maskBits = closedMaskBits;
        } else {
//            setTexture(openTexture);
            filmstrip.setFrame(11);

            setTexture(filmstrip);
            filter.categoryBits = openCategoryBits;
            filter.maskBits = openMaskBits;
        }
        setFilterData(filter);
        setOrigin(origin.x, 0);


//        setTexture(filmstrip);

//        if (open) {
////            setTexture(offTexture);
//            filmstrip.setFrame(11);
//        } else {
////            setTexture(onTexture);
//            filmstrip.setFrame(0);
//        }
//        setOrigin(origin.x, 0);
    }

    public void update(float dt){
        if (animateOff){
            if(filmstrip.getFrame()<=0) {
                animateOff = false;
            }else{
                System.out.println("off door frame "+filmstrip.getFrame());
                filmstrip.setFrame(filmstrip.getFrame()-1);
            }
        }
        if (animateOn){
            if(filmstrip.getFrame()>=11) {
                animateOn = false;
            }else{
                System.out.println("on door frame "+filmstrip.getFrame());
                filmstrip.setFrame(filmstrip.getFrame()+1);
            }
        }
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
