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
import edu.cornell.gdiac.util.FilmStrip;
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
    private float minAngle;
    private float maxAngle;
    private float[] angles;
    private TextureRegion cameraTexture;
    private FilmStrip cameraAnimation;
    private int animateCool;
    private int animateMax;
    public int sector;

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

    public void animateCamera(){
        animateCool++;
        if(animateCool==animateMax){
            animateCool=0;
            int next = (cameraAnimation.getFrame() + 1) % cameraAnimation.getSize();
            cameraAnimation.setFrame(next);
            this.rotationSpeed = (angles[(next+1)%cameraAnimation.getSize()]-angles[next])/animateMax;
        }
    }

    //pan the camera
    public void update() {
        if (this.isOn && this.light!=null) {
            animateCamera();
            this.light.setDistance(onRadius);
            /*Vector2 newDir = this.direction.rotate(rotationSpeed);
            float angle = newDir.angle();*/
            float angle = angles[cameraAnimation.getFrame()];
            this.light.setDirection(angle);
            this.setDirection(new Vector2((float) Math.cos(angle * MathUtils.degreesToRadians), (float) Math.sin(angle * MathUtils.degreesToRadians)));

        }
        else{
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
//        short collideBits = (short)0x0040;
        short collideBits = LevelModel.bitStringToShort("10000");
//        short excludeBits = LevelModel.bitStringToComplement("0000");
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
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("camera", TextureRegion.class);
        cameraTexture = texture;
        setTexture(texture);
        setOrigin(origin.x, 0);

        TextureRegion tex = JsonAssetManager.getInstance().getEntry("camerafront", TextureRegion.class);
        //camera facing front
        //(0,-1)
        if(direction.x==0 && (direction.y==0 || direction.y<0)) {
            cameraAnimation = new FilmStrip(tex.getTexture(), 1, 17);
            this.animateMax = 10;
            this.angles = new float[]{270,250,230,220,200,210,230,250,270,290,300,315,330,310,300,290,270};
            this.setDirection(new Vector2((float) Math.cos(270 * MathUtils.degreesToRadians), (float) Math.sin(270 * MathUtils.degreesToRadians)));
        }
        //(1,0) camera on left wall (facing right)
        if(direction.x>0 && direction.y==0){
            tex = JsonAssetManager.getInstance().getEntry("cameraleft", TextureRegion.class);
            cameraAnimation = new FilmStrip(tex.getTexture(), 1, 10);
            this.animateMax=12;
            this.angles = new float[]{-60, -45, -20, 10, 30, 55, 30, 15, -10, -30};
            this.setDirection(new Vector2((float) Math.cos(minAngle * MathUtils.degreesToRadians), (float) Math.sin(minAngle * MathUtils.degreesToRadians)));
        }
        //(-1, 0) camera on right wall (facing left)
        else if(direction.x<0 && direction.y==0){
            tex = JsonAssetManager.getInstance().getEntry("cameraright", TextureRegion.class);
            cameraAnimation = new FilmStrip(tex.getTexture(), 1, 10);
            this.animateMax=12;
            this.angles = new float[]{220, 215, 205, 160, 140, 120, 130, 170, 190, 210};
            this.setDirection(new Vector2((float) Math.cos(230 * MathUtils.degreesToRadians), (float) Math.sin(230 * MathUtils.degreesToRadians)));
        }
    }

    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(ObstacleCanvas canvas) {
        if (cameraAnimation != null) {
            if(cameraAnimation.getTexture().equals(JsonAssetManager.getInstance().getEntry("cameraright", TextureRegion.class).getTexture())){
                canvas.draw(cameraAnimation,Color.WHITE,origin.x,origin.y,(getX()+0.1f)*drawScale.x,getY()*drawScale.y-getHeight()/2*drawScale.y,getAngle(),0.7f,0.7f);
            }else{
                canvas.draw(cameraAnimation, Color.WHITE, origin.x, origin.y, (getX() - 0.5f) * drawScale.x, getY() * drawScale.y - getHeight() / 2 * drawScale.y, getAngle(), 0.7f, 0.7f);
            }
        }
    }

}
