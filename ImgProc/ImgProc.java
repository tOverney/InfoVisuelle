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

int width = 1500;
int height = 375;

int imgWidth = 500;
int imgHeight = 375;

float discretizationStepsPhi = 0.06f;
float discretizationStepsR = 2.5f;

float[] cosTable;
float[] sinTable;

int phiDim, rDim, rMax;

// HScrollbar lower = new HScrollbar(0, 580 + 100, width, 20);
// HScrollbar upper = new HScrollbar(0, 530 + 100, width, 20);

public void setup() {
    size(width, height);
    img = loadImage("board1.jpg");
    phiDim = (int) (Math.PI / discretizationStepsPhi);
    rDim = (int) (((img.width + img.height) * 2 + 1) / discretizationStepsR);
    rMax = (int) Math.round(sqrt(pow(img.height, 2) + pow(img.width, 2)) / discretizationStepsR);
    initTrigoTables();
}

public void initTrigoTables() {
    cosTable = new float[phiDim];
    sinTable = new float[phiDim];

    for (int i = 0; i < phiDim; i += 1) {
            cosTable[i] = (float) Math.cos(i * discretizationStepsPhi);
            sinTable[i] = (float) Math.sin(i * discretizationStepsPhi);
    }
}

public void draw() {
    PImage result = createImage(imgWidth, imgHeight, RGB);
    PImage sobel;

    result = hueThreshold(img);
    result = gaussianBlur(result);
    sobel = sobel(result);

    PImage sobelDisp = createImage(imgWidth, imgHeight, RGB);;
    sobelDisp.copy(sobel, 0, 0, sobel.width, sobel.height,
                          0, 0, imgWidth, imgHeight);
    image(sobelDisp, 0, 0);

    int[] houghAcc = hough(sobel);
    PImage houghVisualisation = houghAccumulator(houghAcc);
    image(houghVisualisation, imgWidth, 0);

    computeLines(sobel, houghAcc);
    result.copy(img, 0, 0, sobel.width, sobel.height,
                     0, 0, imgWidth, imgHeight);
    result.resize(imgWidth, imgHeight);
    image(result, 2 * imgWidth, 0);
}

public PImage sobel(PImage img) {
    float[][] hKernel = {{0, 1, 0,},
                         {0, 0, 0},
                         {0, -1, 0}};
    float[][] vKernel = {{0, 0, 0},
                         {1, 0, -1},
                         {0, 0, 0}};

    float[][][] allKernels = { hKernel, vKernel };

    PImage result = createImage(img.width, img.height, ALPHA);

    float[] buffer = new float[img.width * img.height];

    float max = convoluteSobel(img, allKernels, buffer);

    for (int y = 2; y < img.height - 2; y++) {
        for (int x = 2; x < img.width - 2; x++) {
            if (buffer[y * img.width + x] > max * 0.25f) {
                result.set(x, y, color(255));
            }
            else {
                result.set(x, y, color(0));
            }
        }
    }

    return result;
}

public float convoluteSobel(PImage img, float[][][] kernel, float[] buffer) {

    float intensity = 0.0f;
    float[] sum = new float[2];
    float max = 0.0f;


    for (int x = 1; x < img.width - 1; x++) {
        for (int y = 1; y < img.height - 1; y++) {
            for (int i = 0; i < kernel.length; i++) {
                intensity = convoluteKernel(img, kernel[i], x, y);
                sum[i] = intensity;
            }

            intensity = sqrt(pow(sum[0], 2) + pow(sum[1], 2));
            int currentIndex = x + y * img.width;
            buffer[currentIndex] = intensity;
            max = buffer[currentIndex] > max ? buffer[currentIndex] : max;
        }
    }

    return max;
}

public PImage houghAccumulator(int[] acc) {
    PImage hough = createImage(rDim + 2, phiDim + 2, ALPHA);

    for (int i = 0; i < acc.length; i++) {
        hough.pixels[i] = color(min(255, acc[i]));
    }

    hough.updatePixels();
    hough.resize(imgWidth, imgHeight);

    return hough;
}

public int[] hough(PImage edges) {
    float maxRadius = sqrt(pow(edges.width, 2) + pow(edges.height, 2));

    int[] accumulator = new int[(phiDim + 2) * (rDim + 2)];

    for (int y = 0; y < edges.height; y++) {
        for (int x = 0 ; x < edges.width; x++) {
            if (brightness(edges.pixels[y * edges.width + x]) != 0) {
                for (int accPhi = 0; accPhi < phiDim; accPhi++) {
                    float r = x * cosTable[accPhi] + y * sinTable[accPhi];
                    float accR = r / discretizationStepsR + (rDim - 1) * 0.5f;
                    accumulator[round((accPhi + 1) * (rDim + 2) + accR + 1)] += 1;
                }
            }
        }
    }

    return accumulator;
}

public void computeLines(PImage edges, int[] accumulator) {

    //PImage h = createImage(rDim + 2, phiDim + 2, ALPHA);
    int count = 0;

    translate(2 * imgWidth, 0);

    for (int idx = 0; idx < accumulator.length; idx++) {
        if (accumulator[idx] > 200) {
            count ++;
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
    //h.updatePixels();

    //return h;
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
            (s > 70 && s < 180) &&
            (b > 70 && b < 160)) {
            result.pixels[i] = color(255);
        }
        else {
            result.pixels[i] = color(0);
        }
    }

    return result;
}

public float convoluteKernel(PImage img, float[][] kernel, int x, int y) {

    float intensity = brightness(img.pixels[(x - 1) + (y - 1) * img.width])
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
        return mouseX > xPosition && mouseX < xPosition + barWidth
            && mouseY > yPosition && mouseY < yPosition + barHeight;
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
