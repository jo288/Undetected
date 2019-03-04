package edu.cornell.gdiac.physics.obstacle;

import com.badlogic.gdx.math.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.physics.box2d.*;

import java.lang.reflect.*;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.physics.obstacle.*;

import javax.xml.soap.Text;

public class MoveableBox extends BoxObstacle{
    private static final float BOX_SIZE = 1;
    private boolean held = false;
    private TextureRegion boxTexture;
    private boolean flaggedForDelete;

    public MoveableBox(float x, float y) {
        super(x, y, BOX_SIZE, BOX_SIZE);
    }

    public MoveableBox() {
        super(BOX_SIZE, BOX_SIZE);
    }

    public boolean getHeld() {
        return held;
    }

    public void setHeld(boolean value) {
        held = value;
    }

    public void setFlaggedForDelete () {
        flaggedForDelete = true;
    }

    public boolean isFlaggedForDelete () {
        return flaggedForDelete;
    }

    public void initialize(){
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("box", TextureRegion.class);
        boxTexture = texture;
        setTexture(texture);
        setBodyType(BodyDef.BodyType.StaticBody);
    }

}
