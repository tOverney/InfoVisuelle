private PShape msTreeModel;

void loadCylinderModel() {
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
            shape(msTreeModel);
        popMatrix();
    }
}