import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Game extends PApplet {

    GameField field;
    Sphere sphere;
    ArrayList<Cylinder> cylinders = new ArrayList<Cylinder>();
    PVector rotation = new PVector(0.0f, 0.0f, 0.0f);
    PVector bufferedRotation = new PVector(0.0f, 0.0f, 0.0f);
    float angularSpeed = radians(1);
    boolean running = true;

    public static void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Game" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
  } else {
      PApplet.main(appletArgs);
  }
}

    public void setup() {
        size(1000, 1000, P3D);
        noStroke();
        field = new GameField(FIELD_DIMENSION, FIELD_THICKNESS);
        sphere = new Sphere(SPHERE_RADIUS);
        loadCylinderModel();
    }

    public void draw() {
        directionalLight(50, 100, 125, 0, 1, 0);
        ambientLight(102, 102, 102);
        background(200);

        if (running) {
            camera(0, -height/5, EYE_AWAY, 0, 0, 0, 0, 1, 0);
        } else {
            camera(0, -SIDE_VIEW, 0, 0, 0, 0, 0, 0, 1);
        }

        rotate();

        if(running) {
            sphere.update();
            sphere.checkEdges();
            sphere.checkCylinderCollision();
        }
        sphere.display();

        field.draw();

        for(Cylinder cylinder : cylinders){
            cylinder.draw();
        }
    }

    public void rotate() {
        rotateX(rotation.x);
        rotateZ(rotation.z);}


/**
 *  Listens to the mouse drag made by the user.
 *
 */
    public void mouseDragged() {
        if(running) {
            float delta = pmouseX - mouseX;
            rotation.z = updateAngle(delta, rotation.z);
            delta = pmouseY - mouseY;
            rotation.x = updateAngle(delta, rotation.x);
        }
    }

    public void mouseClicked(MouseEvent event) {
        if (!running) {
            switch(event.getButton()) {
                case 37 : /* left click */
                float x = (mouseX-width/2.0f)* 280 / SIDE_VIEW;
                float z = (mouseY-height/2.0f)* 280 / SIDE_VIEW;
                if( x < MAX_CYL_POSITION && x > MIN_CYL_POSITION &&
                    z > MIN_CYL_POSITION && z < MAX_CYL_POSITION &&
                    !sphere.isColliding(new PVector(x, 0, z), CYLINDER_RADIUS)){
                    cylinders.add(new Cylinder(new PVector(x, 0,z)));
                }
            break;
            }
        }
    }

    public void mouseWheel(MouseEvent event) {
        float change = event.getCount();
        angularSpeed += (change < 0) ? ANGULAR_CHANGE : -ANGULAR_CHANGE;
        angularSpeed = min(radians(5), max(radians(0), angularSpeed));
    }

    public void keyPressed() {
        switch (keyCode) {
            case SHIFT :
            running = false;
            bufferedRotation = rotation;
            rotation = new PVector(0, 0, 0);
            break;
        }
    }

    public void keyReleased() {
        switch (keyCode) {
            case SHIFT :
            running = true;
            rotation = bufferedRotation;
            break;
        }
    }

/**
 * Computes a new angle given the base angle based on the mouse position.
 * 
 * @param delta mouse position change
 * @param angle angle for which an updated value will be computed.
 * 
 * @return updated angle
 *
 */
    public float updateAngle(float delta, float angle) {

        angle += (delta == 0) ? 0 : (
            (delta > 0) ? angularSpeed : - angularSpeed);

        return max(-MAX_TILT, min(MAX_TILT, angle));
    }
/*
 *  This file regroup all the constants of this project
 *  to avoid useless cluttering
 */
    final static float EYE_AWAY = 800;

    final static int FIELD_DIMENSION = 450;
    final static int FIELD_THICKNESS = 20;
    final static int SPHERE_RADIUS = 20;


    final static float CYLINDER_RADIUS = 20;
    final static float CYLINDER_HEIGHT = 40;
    final static int CYLINDER_RES = 60;

    final static float MIN_CYL_POSITION = - FIELD_DIMENSION / 2 + CYLINDER_RADIUS;
    final static float MAX_CYL_POSITION = FIELD_DIMENSION / 2 - CYLINDER_RADIUS;

    final static int MIN_SPHERE_POSITION = - FIELD_DIMENSION / 2 + SPHERE_RADIUS;
    final static int MAX_SPHERE_POSITION = FIELD_DIMENSION / 2 - SPHERE_RADIUS;

    final static int SIDE_VIEW = FIELD_DIMENSION + 50; 

    final static float GRAVITY_COEF = 0.15f;
    final static float FRICTION_MAGNITUDE = 1 * 0.03f;

    final static float ANGULAR_CHANGE = radians(0.5f);
    final static float MAX_TILT = radians(60);

    final static boolean DEBUG = false;
    private PShape msTreeModel;

    public void loadCylinderModel() {
        msTreeModel = loadShape("tree.obj");
        msTreeModel.scale(10);
        msTreeModel.rotateZ(PI);
    }

class Cylinder {

    private PVector mPosition;

    Cylinder(PVector position) {
        if (msTreeModel == null) {
            loadCylinderModel();

        }

        mPosition = new PVector(position.x, -FIELD_THICKNESS/2, position.z);
    }

    public PShape createDisk(float[] xPos, float yPos, float[] zPos) {
        PShape disk = createShape();
        disk = createShape();
        disk.beginShape(TRIANGLE_FAN);
        disk.vertex(0, yPos, 0);
        for(int i = 0; i < xPos.length; i++) {
            disk.vertex(xPos[i], yPos, zPos[i]);
        }
        disk.endShape();

        return disk;
    }

    public PVector getPosition() {
        return new PVector(mPosition.x, mPosition.y, mPosition.z);
    }

    public void draw() {
        pushMatrix();
        translate(mPosition.x, mPosition.y, mPosition.z);
        shape(msTreeModel);
        popMatrix();
    }
}
/**
 *  This class represent the gamefield (plate). 
 *  @author: Tristan Overney
 *  @date: March 2015
 */

class GameField{
    private int mDimension;
    private int mThickness;

    /**
     * Constructor which takes the two size values
     *
     * @param dimension both y and z dimension for the square field
     * @param thickness y dimension for the field
     *  
     */
    GameField(int dimension, int thickness) {
        mDimension = dimension;
        mThickness = thickness;
    }

    /**
     *  Draws the Field object on the window
     *
     */
    public void draw() {

        pushMatrix();

        stroke(126);
        if (DEBUG) {
            line(-1000, 0, 0, 1000, 0, 0);
            line(0,0,-1000, 0, 0, 1000);
        }
        box(mDimension, mThickness, mDimension);
        popMatrix();

    }
}
class Sphere {
    private PVector mLocation;
    private PVector mVelocity;
    private PVector mAcceleration;
    private int mRadius; 

    Sphere(int radius) {
        mRadius = radius;
        mLocation = 
        new PVector(0, - FIELD_THICKNESS/2, 0); 
        mVelocity = new PVector(0, 0, 0);
        mAcceleration = new PVector(0, 0, 0);
    }

    public boolean isColliding(PVector other, float radius) {
        float deltaX = mLocation.x - other.x;
        float deltaZ = mLocation.z - other.z;
        float lhs = sqrt(pow(deltaX, 2) + pow(deltaZ, 2));
        float rhs = mRadius + radius;
        return lhs <= rhs;
    }

    public void update() { 
        // Gravity computation
        mAcceleration.x = sin(rotation.z) * GRAVITY_COEF;
        mAcceleration.z = -sin(rotation.x) * GRAVITY_COEF;

        // Friction computation
        PVector friction = mVelocity.get();
        friction.mult(-1);
        friction.normalize();
        friction.mult(FRICTION_MAGNITUDE);

        // updating properties
        mAcceleration.add(friction);
        mVelocity.add(mAcceleration);
        mLocation.add(mVelocity);
    }

    public void display() {

        pushMatrix();
        noStroke();
        translate(mLocation.x, mLocation.y - mRadius, mLocation.z);

        sphere(mRadius);
        popMatrix();
    }

    public void checkEdges() {
        if (mLocation.x > MAX_SPHERE_POSITION) {
            mLocation.x = MAX_SPHERE_POSITION;
            mVelocity.x = -mVelocity.x;
        }
        else if(mLocation.x < MIN_SPHERE_POSITION) {
            mLocation.x = MIN_SPHERE_POSITION;
            mVelocity.x = -mVelocity.x;
        }

        if (mLocation.z > MAX_SPHERE_POSITION) {
            mLocation.z = MAX_SPHERE_POSITION;
            mVelocity.z = -mVelocity.z;
        }
        else if(mLocation.z < MIN_SPHERE_POSITION) {
            mLocation.z = MIN_SPHERE_POSITION;
            mVelocity.z = -mVelocity.z;
        }
    }

    public void checkCylinderCollision() {
        for(Cylinder cyl : cylinders) {
            PVector normalCyl = new PVector(), result = new PVector(),
            positionCyl = cyl.getPosition();
            if(isColliding(positionCyl, CYLINDER_RADIUS)){
                PVector.sub(mLocation, positionCyl, normalCyl);
                normalCyl.normalize();
                float bounceCoeff = mVelocity.dot(normalCyl)*2;
                PVector.mult(normalCyl, bounceCoeff, result);
                PVector.sub(mVelocity, result, mVelocity);
                mLocation.add(mVelocity);
            }
        }
    }
}
}
