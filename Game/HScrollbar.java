import processing.core.PApplet;

class HScrollbar {

    PApplet cont;

    float barWidth; // Bar's width in pixels
    float barHeight; // Bar's height in pixels
    float xPosition; // Bar's x position in pixels
    float yPosition; // Bar's y position in pixels
    float sliderPosition, newSliderPosition;
    // Position of slider
    float sliderPositionMin, sliderPositionMax; // Max and min values of slider
    boolean mouseOver;
    boolean locked;

    // Is the mouse over the slider?
    // Is the mouse clicking and dragging the slider now?
    /**
     * @brief Creates a new horizontal scrollbar
     *
     * @param x
     *            The x position of the top left corner of the bar in pixels
     * @param y
     *            The y position of the top left corner of the bar in pixels
     * @param w
     *            The width of the bar in pixels
     * @param h
     *            The height of the bar in pixels
     */
    HScrollbar(PApplet container, float x, float y, float w, float h) {
        container = cont;
        barWidth = w;
        barHeight = h;
        xPosition = x;
        yPosition = y;
        sliderPosition = xPosition + barWidth / 2 - barHeight / 2;
        newSliderPosition = sliderPosition;
        sliderPositionMin = xPosition;
        sliderPositionMax = xPosition + barWidth - barHeight;
    }

    /**
     * @brief Updates the state of the scrollbar according to the mouse movement
     */
    public void update() {
        mouseOver = isMouseOver();
        if (cont.mousePressed) {
            locked = mouseOver;
        }
        else {
            locked = false;
        }
        if (locked) {
            newSliderPosition = constrain(cont.mouseX - barHeight / 2,
                                          sliderPositionMin, sliderPositionMax);
        }
        if (cont.abs(newSliderPosition - sliderPosition) > 1) {
            sliderPosition = sliderPosition
                + (newSliderPosition - sliderPosition);
        }
    }

    /**
     * @brief Clamps the value into the interval
     *
     * @param val
     *            The value to be clamped
     * @param minVal
     *            Smallest value possible
     * @param maxVal
     *            Largest value possible
     *
     * @return val clamped into the interval [minVal, maxVal]
     */
    public float constrain(float val, float minVal, float maxVal) {
        return cont.min(cont.max(val, minVal), maxVal);
    }

    /**
     * @brief Gets whether the mouse is hovering the scrollbar
     *
     * @return Whether the mouse is hovering the scrollbar
     */
    public boolean isMouseOver() {
        return cont.mouseX > xPosition && cont.mouseX < xPosition + barWidth
            && cont.mouseY > yPosition && cont.mouseY < yPosition + barHeight;
    }

    /**
     * @brief Draws the scrollbar in its current state
     */
    public void display() {
        cont.noStroke();
        cont.fill(204);
        cont.rect(xPosition, yPosition, barWidth, barHeight);
        if (mouseOver || locked) {
            cont.fill(0, 0, 0);
        } else {
            cont.fill(102, 102, 102);
        }
        cont.rect(sliderPosition, yPosition, barHeight, barHeight);
    }

    /**
     * @brief Gets the slider position
     *
     * @return The slider position in the interval [0,1] corresponding to
     *         [leftmost position, rightmost position]
     */
    public float getPos() {
        return (sliderPosition - xPosition) / (barWidth - barHeight);
    }
}