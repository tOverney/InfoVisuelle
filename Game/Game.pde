GameField field;
Sphere sphere;
ArrayList<Cylinder> cylinders = new ArrayList<Cylinder>();
PVector rotation = new PVector(0.0, 0.0, 0.0);
PVector bufferedRotation = new PVector(0.0, 0.0, 0.0);
float angularSpeed = radians(1);
boolean running = true;

void setup() {
    size(1000, 1000, P3D);
    noStroke();
    field = new GameField(FIELD_DIMENSION, FIELD_THICKNESS);
    sphere = new Sphere(SPHERE_RADIUS);
    loadCylinderModel();
}

void draw() {
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

void rotate() {
    rotateX(rotation.x);
    rotateZ(rotation.z);}


/**
 *  Listens to the mouse drag made by the user.
 *
 */
void mouseDragged() {
    if(running) {
        float delta = pmouseX - mouseX;
        rotation.z = updateAngle(delta, rotation.z);
        delta = pmouseY - mouseY;
        rotation.x = updateAngle(delta, rotation.x);
    }
}

void mouseClicked(MouseEvent event) {
    if (!running) {
        switch(event.getButton()) {
            case 37 : /* left click */
                float x = (mouseX-width/2.0)* 280 / SIDE_VIEW;
                float z = (mouseY-height/2.0)* 280 / SIDE_VIEW;
                if( x < MAX_CYL_POSITION && x > MIN_CYL_POSITION &&
                    z > MIN_CYL_POSITION && z < MAX_CYL_POSITION &&
                    !sphere.isColliding(new PVector(x, 0, z), CYLINDER_RADIUS)){
                cylinders.add(new Cylinder(new PVector(x, 0,z)));
                }
                break;
        }
    }
}

void mouseWheel(MouseEvent event) {
    float change = event.getCount();
    angularSpeed += (change < 0) ? ANGULAR_CHANGE : -ANGULAR_CHANGE;
    angularSpeed = min(radians(5), max(radians(0), angularSpeed));
}

void keyPressed() {
    switch (keyCode) {
        case SHIFT :
            running = false;
            bufferedRotation = rotation;
            rotation = new PVector(0, 0, 0);
            break;
    }
}

void keyReleased() {
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
float updateAngle(float delta, float angle) {
    
    angle += (delta == 0) ? 0 : (
        (delta > 0) ? angularSpeed : - angularSpeed);

    return max(-MAX_TILT, min(MAX_TILT, angle));
}