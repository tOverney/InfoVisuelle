import processing.core._ 
import processing.core.PConstants._
import processing.opengl._

import scala.language.postfixOps
import scala.math._

object ImgProc extends PApplet {

    val img = loadImage("board1.jpg")
    val thresholdDown = 110.0f
    val thresholdUp = 130.0f

    val discretizationStepsPhi = 0.06f
    val discretizationStepsR = 2.5f


// HScrollbar lower = new HScrollbar(0, 580 + 100, width, 20);
// HScrollbar upper = new HScrollbar(0, 530 + 100, width, 20);

override def setup() {
    width = 800
    height = 600
    size(width, height, OPENGL)
}

override def draw() {
    var result = createImage(width, height, RGB)

    result = hueThreshold(img)
    result = gaussianBlur(result)
    var sobel = sobelFunc(result)

    image(sobel, 0, 0)

    result = hough(sobel);
}

def sobelFunc(img : PImage) : PImage = {
    val hKernel : Array[Array[Float]] = Array(Array(0, 1, 0),
    Array(0, 0, 0),
    Array(0, -1, 0))
    val vKernel : Array[Array[Float]] = Array(Array(0, 0, 0),
    Array(1, 0, -1),
    Array(0, 0, 0))

    val allKernels = Array(hKernel, vKernel)

    val buffer = new Array[Float](img.width * img.height)
    val max = 80.0f

    val result = convoluteSobel(img, allKernels, buffer)

    for (y <- 0 until img.height; x <- 2 until (img.width-2)) {
        if (buffer(y * img.width + x) > max) {
            result.set(x, y, color(255))
        }
        else {
            result.set(x, y, color(0))
        }
    }

    result
}

def houghAccumulator(rDim : Int, phiDim : Int, acc : Array[Int]) : PImage = {
    val hough = createImage(rDim + 2, phiDim + 2, ALPHA);

    for (i <- 0 until acc.length) {
        hough.pixels(i) = color(min(255, acc(i)))
    }

    hough.updatePixels()

    hough
}

def hough(edges : PImage) : PImage = {
    val maxRadius = sqrt(pow(edges.width, 2) + pow(edges.height, 2))

    val phiDim = Pi / discretizationStepsPhi toInt
    val rDim = ((edges.width + edges.height) * 2 + 1) / discretizationStepsR toInt
    val rMax = round(sqrt(pow(edges.height, 2) + pow(edges.width, 2)) / discretizationStepsR) toInt

    val accumulator = new Array[Int]((phiDim + 2) * (rDim + 2))
    val h = createImage(rDim + 2, phiDim + 2, ALPHA)

    for (y <- 0 until edges.height; x <- 0 until edges.width) {
        if (brightness(edges.pixels(y * edges.height + x)) != 0) {
            for (accPhi <- 0 until phiDim) {
                //TODO: compute polar coordinate for every line at this pixel
            }
        }
    }

    // return houghAccumulator(rDim, phiDim, accumulator);

    for (idx <- 0 until accumulator.length) {
        if (accumulator(idx) > 20) {
            val accPhi = idx / (rDim + 2) - 1 toInt
            val accR = idx - (accPhi + 1) * (rDim + 2) -1
            val r = (accR - (rDim - 1) * 0.5f) * discretizationStepsR
            val phi = accPhi * discretizationStepsPhi

            val x0 = 0
            val y0 = r / sin(phi) toInt
            val x1 = r / cos(phi) toInt
            val y1 = 0
            val x2 = edges.width
            val y2 : Int = -cos(phi) / sin(phi) * x2 + r / sin(phi) toInt
            val y3 = edges.width
            val x3 : Int = - (y3 - r / sin(phi)) * (sin(phi) / cos(phi)) toInt

            stroke(204, 102, 0)

            if (y0 > 0) {
                if (x1 > 0)
                line(x0, y0, x1, y1)
                else if (y2 > 0)
                line(x0, y0, x2, y2)
                else
                line (x0, y0, x3, y3)
            }
            else {
                if (x1 > 0) {
                    if (y2 > 0)
                    line(x1, y1, x2, y2)
                    else
                    line(x1, y1, x3, y3)
                }
                else
                line(x2, y2, x3, y3)
            }
        }
    }

    h.updatePixels()

    h
}

def gaussianBlur(img : PImage) : PImage = {
    val kernel : Array[Array[Float]] = Array(Array(9, 12, 9),
    Array(12, 15, 12),
    Array(9, 12, 9))
    val weight = 99.0f
    val result = createImage(img.width, img.height, ALPHA)

    for (x <- 1 until (img.width - 1); y <- 1 until (img.height - 1)) {
        result.set(x, y, color(convoluteKernel(img, kernel, x, y) / weight));
    }

    result
}

def hueThreshold(img : PImage) : PImage = {
    val result = createImage(img.width, img.height, ALPHA)

    for (i <- 0 until img.pixels.length) {
        val h = hue(img.pixels(i))
        val s = saturation(img.pixels(i))
        val b = brightness(img.pixels(i))

        if ((h > 110 && h < 135) &&
            (s > 70 && s < 160) &&
            (b > 70 && b < 160)) {
            result.pixels(i) = color(255)
        }
        else {
            result.pixels(i) = color(0)
        }
    }

    result
}

def convoluteSobel(img : PImage, kernel : Array[Array[Array[Float]]], buffer : Array[Float]) : PImage = {
    val result = createImage(img.width, img.height, ALPHA)

    var intensity = 0.0f
    val sum = new Array[Float](2)


    for (x <- 1 until (img.width - 1); y <- 1 until (img.height - 1)) {
        for (i <- 0 until kernel.length) {
            intensity = convoluteKernel(img, kernel(i), x, y)
            sum(i) = intensity
        }

        intensity = sqrt(pow(sum(0), 2) + pow(sum(1), 2)).toFloat
        buffer(x + (y) * width) = intensity
    }

    result
}

def convoluteKernel(img : PImage, kernel : Array[Array[Float]], x : Int, y : Int) : Float = {

    var intensity = brightness(img.pixels((x - 1) + (y - 1) * img.width)) * kernel(0)(0)
    intensity += brightness(img.pixels((x - 1) + y * img.width)) * kernel(0)(1)
    intensity += brightness(img.pixels((x - 1) + (y + 1) * img.width)) * kernel(0)(2)
    intensity += brightness(img.pixels(x + (y - 1) * img.width)) * kernel(1)(0)
    intensity += brightness(img.pixels(x + y * img.width)) * kernel(1)(1)
    intensity += brightness(img.pixels(x + (y + 1) * img.width)) * kernel(1)(2)
    intensity += brightness(img.pixels((x + 1) + (y - 1) * img.width)) * kernel(2)(0)
    intensity += brightness(img.pixels((x + 1) + y * img.width)) * kernel(2)(1)
    intensity += brightness(img.pixels((x + 1) + (y + 1) * img.width)) * kernel(2)(2)

    intensity
}

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
class HScrollbar(val x : Float, val y : Float, val w : Float, val h : Float) {
    val barWidth = w // Bar's width in pixels
    val barHeight = h // Bar's height in pixels
    val xPosition = x // Bar's x position in pixels
    val yPosition = y // Bar's y position in pixels
    var sliderPosition = xPosition + barWidth / 2 - barHeight / 2
    var newSliderPosition = sliderPosition
    var sliderPositionMin = xPosition
    var sliderPositionMax = xPosition + barWidth - barHeight
    var mouseOver = isMouseOver
    var locked = false

    /**
     * @brief Updates the state of the scrollbar according to the mouse movement
     */
     def update() {
        mouseOver = isMouseOver
        if (mousePressed) {
            locked = mouseOver
        }
        else {
            locked = false
        }
        if (locked) {
            newSliderPosition = (sliderPositionMin max (mouseX - barHeight / 2)) min sliderPositionMax
        }
        if (abs(newSliderPosition - sliderPosition) > 1) {
            sliderPosition = sliderPosition
            + (newSliderPosition - sliderPosition)
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
    def constrain(value : Float, minVal : Float, maxVal : Float) {
        min(max(value, minVal), maxVal)
    }

    /**
     * @brief Gets whether the mouse is hovering the scrollbar
     *
     * @return Whether the mouse is hovering the scrollbar
     */
    def isMouseOver() : Boolean = {
        mouseX > xPosition && mouseX < (xPosition + barWidth) && mouseY > yPosition && mouseY < (yPosition + barHeight)
    }

    /**
     * @brief Draws the scrollbar in its current state
     */
    def display() {
        noStroke()
        fill(204)
        rect(xPosition, yPosition, barWidth, barHeight)
        if (mouseOver || locked) {
            fill(0, 0, 0);
        } else {
            fill(102, 102, 102);
        }
        rect(sliderPosition, yPosition, barHeight, barHeight)
    }

    /**
     * @brief Gets the slider position
     *
     * @return The slider position in the interval [0,1] corresponding to
     *         [leftmost position, rightmost position]
     */
    def getPos() {
        (sliderPosition - xPosition) / (barWidth - barHeight)
    }
}

def main(args : Array[String]) {
    val frame = new javax.swing.JFrame("Hough")
    frame getContentPane() add(this)
    init

    frame pack()
    frame setVisible true
  }
}
