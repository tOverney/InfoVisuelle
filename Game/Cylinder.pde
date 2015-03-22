static PShape msCylinder;

class Cylinder {
    private PVector mPosition;

    Cylinder(PVector position) {
        if (msCylinder == null) {
            float angle;
            float[] x = new float[CYLINDER_RES + 1];
            float[] z = new float[CYLINDER_RES + 1];
            
            //get the x and y position on a circle for all the sides
            for(int i = 0; i < x.length; i++) {
                angle = (TWO_PI / CYLINDER_RES) * i;
                x[i] = sin(angle) * CYLINDER_RADIUS;
                z[i] = cos(angle) * CYLINDER_RADIUS;
            }

            PShape middleCylinder = createShape();
            middleCylinder.beginShape(QUAD_STRIP);
                //draw the border of the msCylinder
                for(int i = 0; i < x.length; i++) {
                    middleCylinder.vertex(x[i], 0, z[i]);
                    middleCylinder.vertex(x[i], -CYLINDER_HEIGHT, z[i]);
                }
            middleCylinder.endShape();

            PShape topCylinder = createDisk(x, -CYLINDER_HEIGHT, z);
            PShape bottomCylinder = createDisk(x, 0, z);

            msCylinder = createShape(GROUP);
            msCylinder.addChild(bottomCylinder);
            msCylinder.addChild(middleCylinder);
            msCylinder.addChild(topCylinder);
        }

        mPosition = new PVector(position.x, -FIELD_THICKNESS/2, position.z);
    }

    PShape createDisk(float[] xPos, float yPos, float[] zPos) {
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

    PVector getPosition() {
        return new PVector(mPosition.x, mPosition.y, mPosition.z);
    }

    void draw() {
        pushMatrix();
            translate(mPosition.x, mPosition.y, mPosition.z);
            shape(msCylinder);
        popMatrix();
    }
}