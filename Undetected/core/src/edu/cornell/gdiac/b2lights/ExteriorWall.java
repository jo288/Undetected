package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;
import java.lang.reflect.*;
import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.physics.obstacle.*;

public class ExteriorWall extends Obstacle{
    /** Collide Bit */
    public static final String COLLIDE_BIT = "1000";
    /** Default Width of Player */
    public static final String EXCLUDE_BIT = "1000";

    /** A complex physics object has multiple bodies */
    protected Array<Obstacle> bodies;
    protected Array<TextureRegion> textures;

    private Array<Integer> positions;

    public ExteriorWall(){
        bodies = new Array<Obstacle>();
        textures = new Array<TextureRegion>();
        positions = new Array<Integer>();
    }

    public class WallBlock extends BoxObstacle{
        /** 0:horiontal, 1:vertical, 2:left-bottom corner, 3: right-bottom corner, 4: left-top corner, 5: right-top corner */
        public int walltype = 0;

        public WallBlock(){
            super(1,1);
        }

        public void initialize(JsonValue json, int posx, int posy){
            setPosition(posx+0.5f,posy+1f);
            setDimension(1,2);

            setBodyType(BodyDef.BodyType.StaticBody);
            setDensity(0);
            setFriction(0.2f);
            setRestitution(0.1f);

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
            this.positions.add(positions[i]);
            this.positions.add(positions[i+1]);
        }
    }

    public Array<Integer> getPositions(){
        return positions;
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
