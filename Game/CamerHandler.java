import processing.core.*;
import processing.data.*;
import processing.event.*;
import processing.opengl.*;
import processing.video.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class CamerHandler {

    private final static float discretizationStepsPhi = 0.06f;
    private final static float discretizationStepsR = 2.5f;
    private final static int neighbourhood = 10;
    private final static int minVotes = 160;
    private final static int lignesLimit = 6;

    private final Movie cam;
    private final PApplet parent;
    private final Thread imgProc;
    private final QuadGraph qGraph;
    private final TwoDThreeD rotationSolver;
    private final int phiDim, rDim, rMax;

    private float[] cosTable, sinTable;
    private AtomicInteger rotX, rotZ;
    private AtomicBoolean updated;
    private PVector lastRotationArray;

    public CamerHandler(PApplet prnt, String videoPath) {
        cam = new Movie(prnt, videoPath);
        parent = prnt;
        cam.loop();
        cam.read();
        qGraph = new QuadGraph();
        rotX = new AtomicInteger(0);
        rotZ = new AtomicInteger(0);
        updated = new AtomicBoolean(false);
        lastRotationArray = new PVector(0, 0, 0);
        imgProc = new Thread(new ImageProcessor());
        imgProc.start();

        phiDim = (int) (Math.PI / discretizationStepsPhi);
        rDim = (int) (((cam.width + cam.height) * 2 + 1)
            / discretizationStepsR);
        rMax = (int) Math.round(parent.sqrt(parent.pow(cam.height, 2)
            + parent.pow(cam.width, 2)) / discretizationStepsR);
        initTrigoTables();
        rotationSolver = new TwoDThreeD(cam.width, cam.height);
    }

    public PVector currentRotation() {
        if(updated.get()) {
            lastRotationArray = new PVector(Float.intBitsToFloat(rotX.get()),
                                            Float.intBitsToFloat(0),
                                            Float.intBitsToFloat(rotZ.get()));
            updated.set(false);
        }
        return lastRotationArray;
    }

    private class ImageProcessor implements Runnable {
        public ImageProcessor() {

        }

        @Override
        public void run() {
            while (!Thread.interrupted()) {
                if (cam.available()) {
                    cam.read();
                    PImage image = cam.get();
                    List<PVector> quad = processImageToQuad(image);
                    if (!quad.isEmpty()) {
                        updateRotations(quad);
                    }
                }
            }
        }
    }

    private List<PVector> processImageToQuad(PImage source){
        PImage sobel = parent.createImage(source.width, source.height, parent.ALPHA);

        sobel = hueThreshold(source);
        sobel = gaussianBlur(sobel);
        sobel = sobel(sobel);
        int[] houghAcc = hough(sobel);

        ArrayList<PVector> candidates = computeLines(lignesLimit, houghAcc);
        qGraph.build(candidates, sobel.width, sobel.height);
        List<int[]> quadsRaw = qGraph.findCycles();

        return buildCleanQuad(candidates, quadsRaw);
    }

    private void updateRotations(List<PVector> quad) {
        PVector r = rotationSolver.get3DRotations(quad);
        rotX.set(Float.floatToIntBits(r.x));
        rotZ.set(Float.floatToIntBits(r.z));
        updated.set(true);
    }

    private void initTrigoTables() {
        cosTable = new float[phiDim];
        sinTable = new float[phiDim];

        for (int i = 0; i < phiDim; i += 1) {
                cosTable[i] = (float) Math.cos(i * discretizationStepsPhi);
                sinTable[i] = (float) Math.sin(i * discretizationStepsPhi);
        }
    }

    public PImage sobel(PImage img) {
        float[][] hKernel = {{0, 1, 0,},
                             {0, 0, 0},
                             {0, -1, 0}};
        float[][] vKernel = {{0, 0, 0},
                             {1, 0, -1},
                             {0, 0, 0}};

        float[][][] allKernels = { hKernel, vKernel };

        PImage result = parent.createImage(img.width, img.height, parent.ALPHA);

        float[] buffer = new float[img.width * img.height];

        float max = convoluteSobel(img, allKernels, buffer);

        for (int y = 2; y < img.height - 2; y++) {
            for (int x = 2; x < img.width - 2; x++) {
                if (buffer[y * img.width + x] > max * 0.25f) {
                    result.set(x, y, parent.color(255));
                }
                else {
                    result.set(x, y, parent.color(0));
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

                intensity = parent.sqrt(parent.pow(sum[0], 2) +
                    parent.pow(sum[1], 2));
                int currentIndex = x + y * img.width;
                buffer[currentIndex] = intensity;
                max = buffer[currentIndex] > max ? buffer[currentIndex] : max;
            }
        }

        return max;
    }

    public PImage houghAccumulator(int[] acc) {
        PImage hough = parent.createImage(rDim + 2, phiDim + 2, parent.ALPHA);

        for (int i = 0; i < acc.length; i++) {
            hough.pixels[i] = parent.color(parent.min(255, acc[i]));
        }

        hough.updatePixels();

        return hough;
    }

    public int[] hough(PImage edges) {
        float maxRadius = parent.sqrt(parent.pow(edges.width, 2) + parent.pow(edges.height, 2));

        int[] accumulator = new int[(phiDim + 2) * (rDim + 2)];

        for (int y = 0; y < edges.height; y++) {
            for (int x = 0 ; x < edges.width; x++) {
                if (parent.brightness(edges.pixels[y * edges.width + x]) != 0) {
                    for (int accPhi = 0; accPhi < phiDim; accPhi++) {
                        float r = x * cosTable[accPhi] + y * sinTable[accPhi];
                        float accR = r / discretizationStepsR + (rDim - 1) * 0.5f;
                        accumulator[parent.round((accPhi + 1)
                            * (rDim + 2) + accR + 1)] += 1;
                    }
                }
            }
        }

        return accumulator;
    }

        public PImage gaussianBlur(PImage img) {
        float[][] kernel = {{9, 12, 9},
                            {12, 15, 12},
                            {9, 12, 9}};
        float weight = 99;
        PImage result = parent.createImage(img.width, img.height, parent.ALPHA);

        for (int x = 1; x < img.width - 1; x++) {
            for (int y = 1; y < img.height - 1; y++) {
                result.set(x, y,
                    parent.color(convoluteKernel(img, kernel, x, y) / weight));
            }
        }

        return result;
    }

    public PImage hueThreshold(PImage img) {
        PImage result = parent.createImage(img.width, img.height, parent.ALPHA);

        for (int i = 0; i < img.pixels.length; i++) {
            float h = parent.hue(img.pixels[i]);
            float s = parent.saturation(img.pixels[i]);
            float b = parent.brightness(img.pixels[i]);

            if ((h > 100 && h < 135) &&
                (s > 128) &&
                (b > 10 && b < 135)) {
                result.pixels[i] = parent.color(255);
            }
            else {
                result.pixels[i] = parent.color(0);
            }
        }

        return result;
    }

    public float convoluteKernel(PImage img, float[][] kernel, int x, int y) {

        float intensity = parent.brightness(img.pixels[(x - 1) + (y - 1) * img.width])
            * kernel[0][0];
        intensity += parent.brightness(img.pixels[(x - 1) + y * img.width])
            * kernel[0][1];
        intensity += parent.brightness(img.pixels[(x - 1) + (y + 1) * img.width])
            * kernel[0][2];
        intensity += parent.brightness(img.pixels[x + (y - 1) * img.width])
            * kernel[1][0];
        intensity += parent.brightness(img.pixels[x + y * img.width])
            * kernel[1][1];
        intensity += parent.brightness(img.pixels[x + (y + 1) * img.width])
            * kernel[1][2];
        intensity += parent.brightness(img.pixels[(x + 1) + (y - 1) * img.width])
            * kernel[2][0];
        intensity += parent.brightness(img.pixels[(x + 1) + y * img.width])
            * kernel[2][1];
        intensity += parent.brightness(img.pixels[(x + 1) + (y + 1) * img.width])
            * kernel[2][2];

        return intensity;
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

        for(int idx : bestCandidates.subList(0,
            parent.min(nLines, bestCandidates.size()))) {

            int accPhi = (int) (idx / (rDim + 2)) - 1;
            int accR = idx - (accPhi + 1) * (rDim + 2) -1;
            float r = (accR - (rDim - 1) * 0.5f) * discretizationStepsR;
            float phi = accPhi * discretizationStepsPhi;

            canditateVectors.add(new PVector(r, phi));
        }

        return canditateVectors;
    }

    public PVector intersection(PVector line1, PVector line2) {
        float d = parent.cos(line2.y) * parent.sin(line1.y) - parent.cos(line1.y) * parent.sin(line2.y);
        float x = (line2.x * parent.sin(line1.y) - line1.x * parent.sin(line2.y)) / d;
        float y = (-line2.x * parent.cos(line1.y) + line1.x * parent.cos(line2.y)) / d;

        return new PVector(x, y);
    }

    public List<PVector> buildCleanQuad(ArrayList<PVector> lines,
        List<int[]> quads) {

        ArrayList<List<PVector>> cleanedQuads = new ArrayList<List<PVector>>();

        for (int[] quad : quads) {
            if(quad.length == 4) {
                PVector l1 = lines.get(quad[0]);
                PVector l2 = lines.get(quad[1]);
                PVector l3 = lines.get(quad[2]);
                PVector l4 = lines.get(quad[3]);
                // (intersection() is a simplified version of the
                // intersections() method you wrote last week, that simply
                // return the coordinates of the intersection between 2 lines)
                PVector c12 = intersection(l1, l2);
                PVector c23 = intersection(l2, l3);
                PVector c34 = intersection(l3, l4);
                PVector c41 = intersection(l4, l1);

                // we return the fist vaild quad we find.
                if(qGraph.isConvex(c12, c23, c34, c41) &&
                    qGraph.validArea(c12, c23, c34, c41, 700000000, 50000) &&
                    qGraph.nonFlatQuad(c12, c23, c34, c41)) {
                    return new ArrayList<PVector>(
                        Arrays.asList(c12, c23, c34, c41));
                }
            }
        }

        return new ArrayList<PVector>(); 
    }
}