package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.physics.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.JsonAssetManager;

public class SignModel extends WheelObstacle {
    String displayText;
    boolean display = false;
    BitmapFont signFont;

    SignModel(){
        super(1,1,1);
    }
    SignModel(float x, float y, float radius){
        super(x,y,radius);
    }
    public void setDisplay(boolean display){
        this.display = display;
    }

    public void initialize(JsonValue json){
        displayText = json.get("text").asString();
        int[] pos  = json.get("pos").asIntArray();
        setPosition(pos[0]+0.5f,pos[1]+0.5f);
        setRadius(2f);

        setBodyType(BodyDef.BodyType.StaticBody);

        TextureRegion texture = JsonAssetManager.getInstance().getEntry("tutorial", TextureRegion.class);
        setTexture(texture);
        signFont = JsonAssetManager.getInstance().getEntry("levelnumber", BitmapFont.class);
        setOrigin(origin.x, origin.y);
        setSensor(true);
    }


    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(ObstacleCanvas canvas) {
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,origin.y,getX()*drawScale.x,getY()*drawScale.y,getAngle(),1.0f,1.0f);
        }
        if(display){
            canvas.drawText(displayText,signFont,getX()*drawScale.x-100, getY()*drawScale.y+70);
        }
    }
}
