package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.lights.ConeSource;
import edu.cornell.gdiac.physics.lights.LightSource;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.lang.reflect.Field;

public class CameraModel extends BoxObstacle{
    private static final float CAMERA_WIDTH = 1f;
    private static final float CAMERA_HEIGHT = 1f;
    private boolean isOn = true;
    private ConeSource light;
    private float onRadius;
    private float offRadius = 0;
    private Vector2 direction;
    private float rotationSpeed;
    private TextureRegion cameraTexture;

    public CameraModel(float x, float y) {
        super(x, y, CAMERA_WIDTH, CAMERA_HEIGHT);
    }

    public CameraModel() {
        super(CAMERA_WIDTH, CAMERA_HEIGHT); isOn=true;
    }

    public boolean isOn() {return isOn;}

    public ConeSource getLight(){
        return light;
    }

    public void addLight(ConeSource light){
        this.light = light;
        this.onRadius = light.getDistance();
    }

    public void turnOn(boolean val) {
        isOn = val;
    }
    public void toggle(){isOn=!isOn;}

    public void switchState() {
        isOn = !isOn;
        if(!isOn){
            this.light = null;
        }
        else{
            update();
        }
        System.out.println("CAM STATE "+isOn);
    }

    public void setRotationSpeed(float speed){
        this.rotationSpeed = speed;
    }
    public Vector2 getDirection(){ return direction; }
    public void setDirection(float angle){
        float adjustedAng = angle+(float)Math.PI/2.0f;
        this.light.setDirection(adjustedAng* MathUtils.radiansToDegrees);
        direction = new Vector2((float)Math.cos(adjustedAng), (float)Math.sin(adjustedAng));
    }
    public void setDirection(Vector2 dir){ direction = dir; }

    //pan the camera
    public void update() {
        if (this.isOn && this.light!=null) {
            System.out.println("camera onn "+onRadius);
            this.light.setDistance(onRadius);
            Vector2 newDir = this.direction.rotate(rotationSpeed);
            float angle = newDir.angle();
            System.out.println("CAM ANGLE " + angle);
            if (angle >= 135) {
                angle = 135;
                rotationSpeed *= -1;
            } else if (angle <= 45) {
                angle = 45;
                rotationSpeed *= -1;
            }
            this.light.setDirection(angle);
            this.setDirection(new Vector2((float) Math.cos(angle * MathUtils.degreesToRadians), (float) Math.sin(angle * MathUtils.degreesToRadians)));
        }
        else{
            System.out.println("camera off");
            if(this.light!=null&&this.light.getDistance()!=0){
                this.light.setDistance(0);
            }
        }
    }

    public void initialize(){
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("camera", TextureRegion.class);
        cameraTexture = texture;
        setBodyType(BodyDef.BodyType.StaticBody);
        setPosition(3+0.5f,3+0.5f);
        turnOn(true);
        setRotationSpeed(1);
        setDirection(new Vector2(1, 0));
        setTexture(texture);
        setOrigin(origin.x, 0);
    }

    public void initialize(JsonValue json){
        setName(json.name());
        float[] size = {1, 1};
        setWidth(size[0]);
        setHeight(size[1]);
        setFixedRotation(true);
        float[] pos = json.get("pos").asFloatArray();
        setPosition(pos[0]+0.5f,pos[1]+0.5f);
        setRotationSpeed(json.get("rotationSpeed").asFloat());
        turnOn(json.get("on").asBoolean());
        int[] dir = json.get("direction").asIntArray();
        setDirection(new Vector2(dir[0], dir[1]));

        // Technically, we should do error checking here.
        // A JSON field might accidentally be missing
        setBodyType(BodyDef.BodyType.StaticBody);

        // Create the collision filter (used for light penetration)
        short collideBits = LevelModel.bitStringToShort("10001");
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
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("camera", TextureRegion.class);
        cameraTexture = texture;
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
