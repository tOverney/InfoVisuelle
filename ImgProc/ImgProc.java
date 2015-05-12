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

public class ImgProc extends PApplet {

PImage img;

float thresholdDown = 110.0f;
float thresholdUp = 130.0f;

int width = 800;
int height = 600;

float discretizationStepsPhi = 0.06f;
float discretizationStepsR = 2.5f;


// HScrollbar lower = new HScrollbar(0, 580 + 100, width, 20);
// HScrollbar upper = new HScrollbar(0, 530 + 100, width, 20);

public void setup() {
    size(width, height);
    img = loadImage("board1.jpg");
}

public void draw() {
    PImage result = createImage(width, height, RGB);
    PImage sobel;

    result = hueThreshold(img);
    result = gaussianBlur(result);
    sobel = sobel(result);

    image(sobel, 0, 0);

    result = hough(sobel);
}

public PImage sobel(PImage img) {
    float[][] hKernel = {{0, 1, 0,},
                         {0, 0, 0},
                         {0, -1, 0}};
    float[][] vKernel = {{0, 0, 0},
                         {1, 0, -1},
                         {0, 0, 0}};

    float[][][] allKernels = { hKernel, vKernel };

    PImage result;

    float[] buffer = new float[img.width * img.height];
    float max = 80.0f;

    result = convoluteSobel(img, allKernels, buffer);

    for (int y = 2; y < img.height - 2; y++) {
        for (int x = 2; x < img.width - 2; x++) {
            if (buffer[y * img.width + x] > max) {
                result.set(x, y, color(255));
            }
            else {
                result.set(x, y, color(0));
            }
        }
    }

    return result;
}

public PImage houghAccumulator(int rDim, int phiDim, int[] acc) {
    PImage hough = createImage(rDim + 2, phiDim + 2, ALPHA);

    for (int i = 0; i < acc.length; i++) {
        hough.pixels[i] = color(min(255, acc[i]));
    }

    hough.updatePixels();

    return hough;
}

public PImage hough(PImage edges) {
    float maxRadius = sqrt(pow(edges.width, 2) + pow(edges.height, 2));

    int phiDim = (int) (Math.PI / discretizationStepsPhi);
    int rDim = (int) (((edges.width + edges.height) * 2 + 1) / discretizationStepsR);
    int rMax = (int) Math.round(sqrt(pow(edges.height, 2) + pow(edges.width, 2)) / discretizationStepsR);

    int[] accumulator = new int[(phiDim + 2) * (rDim + 2)];
    PImage h = createImage(rDim + 2, phiDim + 2, ALPHA);

    for (int y = 0; y < edges.height; y++) {
        for (int x = 0 ; x < edges.width; x++) {
            if (brightness(edges.pixels[y * edges.height + x]) != 0) {
                for (int accPhi = 0; accPhi < phiDim; accPhi++) {
                    //TODO: compute polar coordinate for every line at this pixel
                }
            }
        }
    }

    // return houghAccumulator(rDim, phiDim, accumulator);

    for (int idx = 0; idx < accumulator.length; idx++) {
        if (accumulator[idx] > 20) {
            int accPhi = (int) (idx / (rDim + 2)) - 1;
            int accR = idx - (accPhi + 1) * (rDim + 2) -1;
            float r = (accR - (rDim - 1) * 0.5f) * discretizationStepsR;
            float phi = accPhi * discretizationStepsPhi;

            int x0 = 0;
            int y0 = (int) (r / sin(phi));
            int x1 = (int) (r / cos(phi));
            int y1 = 0;
            int x2 = edges.width;
            int y2 = (int) (-cos(phi) / sin(phi) * x2 + r / sin(phi));
            int y3 = edges.width;
            int x3 = (int) (- (y3 - r / sin(phi)) * (sin(phi) / cos(phi)));

            stroke(204, 102, 0);

            if (y0 > 0) {
                if (x1 > 0)
                    line(x0, y0, x1, y1);
                else if (y2 > 0)
                    line(x0, y0, x2, y2);
                else
                    line (x0, y0, x3, y3);
            }
            else {
                if (x1 > 0) {
                    if (y2 > 0)
                        line(x1, y1, x2, y2);
                    else
                        line(x1, y1, x3, y3);
                }
                else
                    line(x2, y2, x3, y3);
            }
        }
    }

    h.updatePixels();

    return h;
}

public PImage gaussianBlur(PImage img) {
    float[][] kernel = {{9, 12, 9},
                        {12, 15, 12},
                        {9, 12, 9}};
    float weight = 99;
    PImage result = createImage(img.width, img.height, ALPHA);

    for (int x = 1; x < img.width - 1; x++) {
        for (int y = 1; y < img.height - 1; y++) {
            result.set(x, y, color(convoluteKernel(img, kernel, x, y) / weight));
        }
    }

    return result;
}

public PImage hueThreshold(PImage img) {
    PImage result = createImage(img.width, img.height, ALPHA);

    for (int i = 0; i < img.pixels.length; i++) {
        float h = hue(img.pixels[i]);
        float s = saturation(img.pixels[i]);
        float b = brightness(img.pixels[i]);

        if ((h > 110 && h < 135) &&
            (s > 70 && s < 160) &&
            (b > 70 && b < 160)) {
            result.pixels[i] = color(255);
        }
        else {
            result.pixels[i] = color(0);
        }
    }

    return result;
}

public PImage convoluteSobel(PImage img, float[][][] kernel, float[] buffer) {
    PImage result = createImage(img.width, img.height, ALPHA);

    float intensity = 0.0f;
    float[] sum = new float[2];


    for (int x = 1; x < img.width - 1; x++) {
        for (int y = 1; y < img.height - 1; y++) {
            for (int i = 0; i < kernel.length; i++) {
                intensity = convoluteKernel(img, kernel[i], x, y);
                sum[i] = intensity;
            }

            intensity = sqrt(pow(sum[0], 2) + pow(sum[1], 2));
            buffer[x + y * width] = intensity;
        }
    }

    return result;
}

public float convoluteKernel(PImage img, float[][] kernel, int x, int y) {
    float intensity;

    intensity = brightness(img.pixels[(x - 1) + (y - 1) * img.width])
        * kernel[0][0];
    intensity += brightness(img.pixels[(x - 1) + y * img.width])
        * kernel[0][1];
    intensity += brightness(img.pixels[(x - 1) + (y + 1) * img.width])
        * kernel[0][2];
    intensity += brightness(img.pixels[x + (y - 1) * img.width])
        * kernel[1][0];
    intensity += brightness(img.pixels[x + y * img.width])
        * kernel[1][1];
    intensity += brightness(img.pixels[x + (y + 1) * img.width])
        * kernel[1][2];
    intensity += brightness(img.pixels[(x + 1) + (y - 1) * img.width])
        * kernel[2][0];
    intensity += brightness(img.pixels[(x + 1) + y * img.width])
        * kernel[2][1];
    intensity += brightness(img.pixels[(x + 1) + (y + 1) * img.width])
        * kernel[2][2];

    return intensity;
}
class HScrollbar {
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
    HScrollbar(float x, float y, float w, float h) {
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
        if (mousePressed) {
            locked = mouseOver;
        }
        else {
            locked = false;
        }
        if (locked) {
            newSliderPosition = constrain(mouseX - barHeight / 2,
                                          sliderPositionMin, sliderPositionMax);
        }
        if (abs(newSliderPosition - sliderPosition) > 1) {
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
        return min(max(val, minVal), maxVal);
    }

    /**
     * @brief Gets whether the mouse is hovering the scrollbar
     *
     * @return Whether the mouse is hovering the scrollbar
     */
    public boolean isMouseOver() {
        if (mouseX > xPosition && mouseX < xPosition + barWidth
            && mouseY > yPosition && mouseY < yPosition + barHeight) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @brief Draws the scrollbar in its current state
     */
    public void display() {
        noStroke();
        fill(204);
        rect(xPosition, yPosition, barWidth, barHeight);
        if (mouseOver || locked) {
            fill(0, 0, 0);
        } else {
            fill(102, 102, 102);
        }
        rect(sliderPosition, yPosition, barHeight, barHeight);
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
  public static void main(String[] args) {
    String[] appletArgs = new String[] { "ImgProc" };
    if (args != null) {
      PApplet.main(concat(appletArgs, args));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
