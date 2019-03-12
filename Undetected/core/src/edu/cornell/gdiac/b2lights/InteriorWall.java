package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.BoxObstacle;
import edu.cornell.gdiac.physics.obstacle.Obstacle;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.util.JsonAssetManager;

import java.lang.reflect.Field;

public class InteriorWall extends Obstacle{

    /** A complex physics object has multiple bodies */
    protected Array<Obstacle> bodies;
    protected Array<TextureRegion> textures;

    public InteriorWall(){
        bodies = new Array<Obstacle>();
        textures = new Array<TextureRegion>();
    }

    private class WallBlock extends BoxObstacle{
        /** 0:horiontal, 1:vertical, 2:left-bottom corner, 3: right-bottom corner, 4: left-top corner, 5: right-top corner */
        public int walltype = 0;

        public WallBlock(){
            super(1,1);
        }

        public void initialize(JsonValue json, int posx, int posy){
            setPosition(posx+0.5f,posy+0.5f);
            setDimension(1,1);

            setBodyType(BodyDef.BodyType.StaticBody);
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

        }

        public void setWallTexture(TextureRegion texture){
            setTexture(texture);
            setOrigin(origin.x,0);
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

    public void initialize(JsonValue json){
        int[] positions = json.get("pos").asIntArray();
        int[] types = json.get("type").asIntArray();
        textures.add(JsonAssetManager.getInstance().getEntry("wallhorizontal", TextureRegion.class));
        textures.add(JsonAssetManager.getInstance().getEntry("wallvertical", TextureRegion.class));

        for (int i = 0;i<positions.length;i+=2){
            WallBlock wb = new WallBlock();
            wb.initialize(json,positions[i],positions[i+1]);
            if (i/2<types.length)
                wb.walltype = types[i/2];
            wb.setWallTexture(textures.get(wb.walltype));
            bodies.add(wb);
        }
    }

    @Override
    public void setDrawScale(Vector2 value) {
        for (Obstacle o: bodies){
            o.setDrawScale(value);
        }
    }

    @Override
    public boolean activatePhysics(World world) {
        boolean ret = true;
        for (Obstacle o: bodies){
            ret &= o.activatePhysics(world);
        }
        return ret;
    }

    @Override
    public void deactivatePhysics(World world) {
        for (Obstacle o: bodies){
            o.deactivatePhysics(world);
        }

    }

    @Override
    public void drawDebug(ObstacleCanvas canvas) {
        for (Obstacle o: bodies){
            o.drawDebug(canvas);
        }
    }

    @Override
    public void draw(ObstacleCanvas canvas) {
        for (Obstacle o: bodies){
            o.draw(canvas);
        }
    }
}
