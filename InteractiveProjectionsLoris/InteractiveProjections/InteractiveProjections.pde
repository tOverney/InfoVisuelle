final static int NB_OF_VERTICES_2DBOX = 8;

void setup() {
  size(1000, 1000, P2D);
}

//scaling following the mouse
float scale = 1.0;
float scalingFactor = 0.1;
float oldMouseY = mouseY;

//rotation in function of UP/DOWN/RIGHT/LEFT arrow keys pressed
float xRotation = 0.0;
float yRotation = 0.0;
float rotFactor = PI/32.0;

void mouseDragged() {
  if(oldMouseY - mouseY > 0.0) {
    //dragged down
    scale -= scalingFactor;
  } else if(oldMouseY - mouseY < 0.0) {
    //dragged up
    scale += scalingFactor;
  }
  oldMouseY = mouseY;
}

void keyPressed() {
  if(key == CODED) {
    if(keyCode == UP) {
      xRotation += rotFactor;
    } else if(keyCode == DOWN) {
      xRotation -= rotFactor;
    } else if(keyCode == RIGHT) {
      yRotation -= rotFactor;
    } else if(keyCode == LEFT) {
      yRotation += rotFactor;
    }
  }
}

class Custom2DPoint {
  float x, y;
  
  Custom2DPoint(float x, float y) {
    this.x = x;
    this.y = y;
  }
}

class Custom3DPoint {
  float x, y, z;
  
  Custom3DPoint(float x, float y, float z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
  
  Custom3DPoint() {
    this.x = 0;
    this.y = 0;
    this.z = 0;
  }
}

class Custom2DBox {
  Custom2DPoint[] vertices;
  
  Custom2DBox(Custom2DPoint[] vertice_tab) {
    this.vertices = vertice_tab;
  }
  
  void render() {
    line(vertices[0].x, vertices[0].y, vertices[1].x, vertices[1].y);
    line(vertices[0].x, vertices[0].y, vertices[3].x, vertices[3].y);
    line(vertices[0].x, vertices[0].y, vertices[4].x, vertices[4].y);
    line(vertices[1].x, vertices[1].y, vertices[2].x, vertices[2].y);
    line(vertices[1].x, vertices[1].y, vertices[5].x, vertices[5].y);
    line(vertices[2].x, vertices[2].y, vertices[3].x, vertices[3].y);
    line(vertices[2].x, vertices[2].y, vertices[6].x, vertices[6].y);
    line(vertices[3].x, vertices[3].y, vertices[7].x, vertices[7].y);
    line(vertices[4].x, vertices[4].y, vertices[5].x, vertices[5].y);
    line(vertices[4].x, vertices[4].y, vertices[7].x, vertices[7].y);
    line(vertices[5].x, vertices[5].y, vertices[6].x, vertices[6].y);
    line(vertices[6].x, vertices[6].y, vertices[7].x, vertices[7].y);
  }
}

class My3DBox {
  Custom3DPoint[] p;
  
  My3DBox(Custom3DPoint origin, float dimX, float dimY, float dimZ) {
    float x = origin.x;
    float y = origin.y;
    float z = origin.z;
    this.p = new Custom3DPoint[] {new Custom3DPoint(x, y+dimY, z+dimZ),
                                  new Custom3DPoint(x, y, z+dimZ),
                                  new Custom3DPoint(x+dimX, y, z+dimZ),
                                  new Custom3DPoint(x+dimX, y+dimY, z+dimZ),
                                  new Custom3DPoint(x, y+dimY, z),
                                  origin,
                                  new Custom3DPoint(x+dimX, y, z),
                                  new Custom3DPoint(x+dimX, y+dimY, z)
                                 };
  }
  
  My3DBox(Custom3DPoint[] p) {
    this.p = p;
  }
  
  My3DBox() {
    this.p = new Custom3DPoint[8];
  }
}

Custom2DBox projectBox (Custom3DPoint eye, My3DBox box) {
  Custom2DPoint[] box2D = new Custom2DPoint[NB_OF_VERTICES_2DBOX];
  
  for(int i = 0; i < NB_OF_VERTICES_2DBOX; ++i) {
    box2D[i] = projectPoint(eye, box.p[i]);
  }
  
  return new Custom2DBox(box2D); 
}

Custom2DPoint projectPoint(Custom3DPoint eye, Custom3DPoint p) {
  Custom2DPoint p2D = new Custom2DPoint(0,0);
  
  p2D.x = (p.x - eye.x)*(-eye.z/(p.z - eye.z));
  p2D.y = (p.y - eye.y)*(-eye.z/(p.z - eye.z));
  
  return p2D;
}

float[] homogeneous3DPoint(Custom3DPoint p) {
  float[] result = {p.x, p.y, p.z, 1};
  return result;
}

float[][] rotateXMatrix(float angle) {
  return(new float[][] {{1, 0, 0, 0},
                        {0, cos(angle), sin(angle), 0},
                        {0, -sin(angle), cos(angle), 0},
                        {0, 0, 0, 1}});
}

float[][] rotateYMatrix(float angle) {
  return(new float[][] {{cos(angle), 0, -sin(angle), 0},
                        {0, 1, 0, 0},
                        {sin(angle), 0, cos(angle), 0},
                        {0, 0, 0, 1}});
}

float[][] rotateZMatrix(float angle) {
  return(new float[][] {{cos(angle), sin(angle), 0, 0},
                        {-sin(angle), cos(angle), 0, 0},
                        {0, 0, 1, 0},
                        {0, 0, 0, 1}});
}

float[][] scaleMatrix(float x, float y, float z) {
  return(new float[][] {{x, 0, 0, 0},
                        {0, y, 0, 0},
                        {0, 0, z, 0},
                        {0, 0, 0, 1}});
}

float[][] translationMatrix(float x, float y, float z) {
  return(new float[][] {{1, 0, 0, x},
                        {0, 1, 0, y},
                        {0, 0, 1, z},
                        {0, 0, 0, 1}});
}

float[] matrixProduct(float[][] a, float[] b) {
  float[] c = new float[b.length];
  for(int i = 0; i < a.length; ++i) {
    for(int j = 0; j < a[i].length; ++j) {
      c[i] += (a[i][j] * b[j]);
    }
  }
  return c;
}

Custom3DPoint euclidian3DPoint(float[] a) {
  Custom3DPoint result = new Custom3DPoint(a[0]/a[3], a[1]/a[3], a[2]/a[3]);
  return result;
}

My3DBox transformBox(My3DBox box, float[][] transformMat) {
  My3DBox result = new My3DBox();
  
  for(int i = 0; i < box.p.length; ++i) {
    result.p[i] = euclidian3DPoint(matrixProduct(transformMat, homogeneous3DPoint(box.p[i])));
  }
  return result;
}

void draw() {
  background(255, 255, 255);
  Custom3DPoint eye = new Custom3DPoint(0, 0, -5000);
  Custom3DPoint origin  = new Custom3DPoint(-50, -75, -150);
  My3DBox input3DBox = new My3DBox(origin, 100, 150, 300);
  
  //rotating with keys UP/DOWN/RIGHT/LEFT arrows
  float[][] rotateXArrows = rotateXMatrix(xRotation);
  float[][] rotateYArrows = rotateYMatrix(yRotation);
  
  input3DBox = transformBox(input3DBox, rotateXArrows);
  input3DBox = transformBox(input3DBox, rotateYArrows);
  
  //default position
  float[][] transform = rotateXMatrix(PI/8.0);
  input3DBox = transformBox(input3DBox, transform);
 
  transform = translationMatrix(250, 250, 0);
  input3DBox = transformBox(input3DBox, transform);
  
  //default scale
  transform = scaleMatrix(2,2,2);
  input3DBox = transformBox(input3DBox, transform);
  projectBox(eye, input3DBox).render();
}
