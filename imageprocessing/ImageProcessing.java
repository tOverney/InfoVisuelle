import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class ImageProcessing extends PApplet {

    HoughComparator hCompare;

    PImage img;

    // float thresholdDown = 110.0f;
    // float thresholdUp = 130.0f;

    int width = 1500;
    int height = 375;

    int neighbourhood = 10;
    int minVotes = 160;
    int lignesLimit = 6;

    int imgWidth = width / 3;
    int imgHeight = 375;

    float discretizationStepsPhi = 0.06f;
    float discretizationStepsR = 2.5f;

    String imageFolder = "boardImages/";
    String imageName = "board1";
    String imageFormat = ".jpg";

    float[] cosTable;
    float[] sinTable;

    int phiDim, rDim, rMax;

    HScrollbar lower = new HScrollbar(this, 0, 580 + 100, width, 20);
    // HScrollbar upper = new HScrollbar(this, 0, 530 + 100, width, 20);

    public void setup() {
        size(width, height);
        img = loadImage(imageFolder + imageName + imageFormat);
        phiDim = (int) (Math.PI / discretizationStepsPhi);
        rDim = (int) (((img.width + img.height) * 2 + 1)
            / discretizationStepsR);
        rMax = (int) Math.round(sqrt(pow(img.height, 2) 
            + pow(img.width, 2)) / discretizationStepsR);
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

        sobel.resize(imgWidth, imgHeight);
        image(sobel, 0, 0);

        int[] houghAcc = hough(sobel);
        PImage houghVisualisation = houghAccumulator(houghAcc);
        image(houghVisualisation, imgWidth, 0);
        
        img.resize(imgWidth, imgHeight);
        image(img, 2 * imgWidth, 0);
        ArrayList<PVector> candidates = computeLines(lignesLimit, houghAcc);

        translate(2 * imgWidth, 0);
        getIntersections(candidates);
        plotLines(sobel, candidates);
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

    public float convoluteSobel(PImage img, 
        float[][][] kernel, float[] buffer) {

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
                        accumulator[round((accPhi + 1) 
                            * (rDim + 2) + accR + 1)] += 1;
                    }
                }
            }
        }

        return accumulator;
    }

    public ArrayList<PVector> computeLines(int nLines, int[] accumulator) {

        //PImage h = createImage(rDim + 2, phiDim + 2, ALPHA);

        ArrayList<Integer> bestCandidates = new ArrayList<Integer>();

        
        for (int accR = 0; accR < rDim; accR++) {
            for (int accPhi = 0; accPhi < phiDim; accPhi++) {
            // compute current index in the accumulator
                int idx = (accPhi + 1) * (rDim + 2) + accR + 1;
                if (accumulator[idx] > minVotes) {
                    
                    boolean bestCandidate=true;
                    
                    // iterate over the neighbourhood
                    for(int dPhi=-neighbourhood/2; dPhi < neighbourhood/2+1; dPhi++) { 
                        // check we are not outside the image
                        if( accPhi+dPhi < 0 || accPhi+dPhi >= phiDim) continue;
                        for(int dR=-neighbourhood/2; dR < neighbourhood/2 +1; dR++) {
                            // check we are not outside the image
                            if(accR+dR < 0 || accR+dR >= rDim) continue;
                            
                            int neighbourIdx = (accPhi + dPhi + 1) * (rDim + 2) + accR + dR + 1;
                            if(accumulator[idx] < accumulator[neighbourIdx]) {
                                // the current idx is not a local maximum! bestCandidate=false;
                                break;
                            }
                        }
                        if(!bestCandidate) break;
                    }
                    if(bestCandidate) {
                        // the current idx *is* a local maximum
                        bestCandidates.add(idx);
                    }
                }
            }
        }

        Collections.sort(bestCandidates, new HoughComparator(accumulator));

        ArrayList<PVector> canditateVectors = new ArrayList<PVector>();

        for(int idx : bestCandidates.subList(0, nLines)) {
            int accPhi = (int) (idx / (rDim + 2)) - 1;
            int accR = idx - (accPhi + 1) * (rDim + 2) -1;
            float r = (accR - (rDim - 1) * 0.5f) * discretizationStepsR;
            float phi = accPhi * discretizationStepsPhi;

            canditateVectors.add(new PVector(r, phi));
        }

        return canditateVectors;
    }

    public ArrayList<PVector> getIntersections(List<PVector> lines) {
        ArrayList<PVector> intersections = new ArrayList<PVector>();

        for (int i = 0; i < lines.size() - 1; i++) {
            PVector line1 = lines.get(i);
            
            for (int j = i + 1; j < lines.size(); j++) {
                PVector line2 = lines.get(j);
                
                // compute the intersection and add it to 'intersections'
                float d = cos(line2.y) * sin(line1.y) - cos(line1.y) * sin(line2.y);
                float x = (line2.x * sin(line1.y) - line1.x * sin(line2.y)) / d;
                float y = (-line2.x * cos(line1.y) + line1.x * cos(line2.y)) / d;

                intersections.add(new PVector(x, y));
                // draw the intersection
                fill(255, 128, 0);
                ellipse(x, y, 10, 10);
            }
        }
        return intersections;
    }


    public void plotLines(PImage edges, ArrayList<PVector> vertices) {

        for (PVector vertex : vertices) {
            float phi = vertex.y, r = vertex.x;

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
                result.set(x, y,
                    color(convoluteKernel(img, kernel, x, y) / weight));
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

    public static void main(String[] args) {
        String[] appletArgs = new String[] { "ImageProcessing" };
        if (args != null) {
          PApplet.main(concat(appletArgs, args));
        } else {
          PApplet.main(appletArgs);
        }
    }
}
