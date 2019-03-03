package edu.cornell.gdiac.b2lights;

import edu.cornell.gdiac.physics.obstacle.*;

public class CharacterModel extends WheelObstacle {
    public CharacterModel(){
        super(1f,1f,1f);
    }
    public CharacterModel(float x, float y, float radius) {
        super(x,y,radius);
    }
}
