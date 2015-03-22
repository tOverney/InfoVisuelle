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

    boolean isColliding(PVector other, float radius) {
        float deltaX = mLocation.x - other.x;
        float deltaZ = mLocation.z - other.z;
        float lhs = sqrt(pow(deltaX, 2) + pow(deltaZ, 2));
        float rhs = mRadius + radius;
        return lhs <= rhs;
    }

    void update() { 
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

    void display() {

        pushMatrix();
            noStroke();
            translate(mLocation.x, mLocation.y - mRadius, mLocation.z);

            sphere(mRadius);
        popMatrix();
    }

    void checkEdges() {
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

    void checkCylinderCollision() {
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