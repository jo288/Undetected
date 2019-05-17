package edu.cornell.gdiac.b2lights;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics.obstacle.ObstacleCanvas;
import edu.cornell.gdiac.physics.obstacle.WheelObstacle;
import edu.cornell.gdiac.util.FilmStrip;
import edu.cornell.gdiac.util.JsonAssetManager;

public class SignModel extends WheelObstacle {
    String displayText;
    boolean display = false;
    BitmapFont signFont;
    FilmStrip filmstrip;
    private int animationFrame = 0;
    private int animationRate = 4;
    private int animationCount = 0;

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
        Filter filter = new Filter();
        filter.categoryBits = (short)0x0040;
        setFilterData(filter);

        try {
            filmstrip = new FilmStrip(texture.getTexture(), 1, 7);
        } catch (Exception e) {
            filmstrip = null;
        }
        animationFrame = 7;
        setTexture(filmstrip);
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        if(filmstrip!=null) {
            int anim = 0;
            if(animationCount>=2*(animationFrame*animationRate)-1){
                animationCount = 0;
            }
            else {
                animationCount = (animationCount + 1) % (animationFrame * animationRate);
                anim = animationCount/animationRate;
            }
            filmstrip.setFrame(anim);
        }
    }


    /**
     * Draws the physics object.
     *
     * @param canvas Drawing context
     */
    public void draw(ObstacleCanvas canvas) {
        TextureRegion overlaytexture = JsonAssetManager.getInstance().getEntry("darkoverlay", TextureRegion.class);
        if (texture != null) {
            canvas.draw(texture,Color.WHITE,origin.x,0,getX()*drawScale.x,getY()*drawScale.y-0.35f*drawScale.y,getAngle(),1.0f,1.0f);
        }
        if(display){
            canvas.draw(overlaytexture,Color.WHITE, overlaytexture.getRegionWidth()/2,overlaytexture.getRegionHeight()/2,
                    getX()*drawScale.x,getY()*drawScale.y+70,getAngle(),0.05f*(displayText.length()),0.1f);
            canvas.drawText(displayText,signFont,getX()*drawScale.x-100, getY()*drawScale.y+70);
        }
    }
}
