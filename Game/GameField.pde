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
    void draw() {

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