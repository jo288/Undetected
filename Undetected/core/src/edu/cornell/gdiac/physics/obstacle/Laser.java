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

public class Laser extends BoxObstacle {
    private static final float LAZER_HEIGHT = 5f;
    private static final float LAZER_WIDTH = 0.1f;


    public Laser(float x, float y) {
        super(x, y, LAZER_WIDTH, LAZER_HEIGHT);
        setSensor(true);
    }

    public Laser() {
        super(LAZER_WIDTH, LAZER_HEIGHT);
        setSensor(true);
    }

    public void initialize() {
        TextureRegion texture = JsonAssetManager.getInstance().getEntry("laser", TextureRegion.class);
        setTexture(texture);
        this.setBodyType(BodyDef.BodyType.StaticBody);
    }
}
