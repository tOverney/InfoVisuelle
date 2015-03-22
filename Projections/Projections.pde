final static float NORMALIZATION = 0.5;
final static float STEP = 0.1;
final static float ANGULAR_SPEED = PI / 16;

float lastMouseX = 0;
float scale = 0.5;
float alpha = 0;
float beta = 0;


void setup() {
  size (1000, 1000, P2D);
}

void draw() {
  background(255, 255, 255);
  My3DPoint eye = new My3DPoint(0, 0, -5000);
  My3DPoint origin = new My3DPoint(0, 0, 0);
  My3DBox input3DBox = new My3DBox(origin, 100, 150, 300);
  //rotated around x
  float[][] transform1 = rotateXMatrix(PI/8); 
  input3DBox = transformBox(input3DBox, transform1); 
  projectBox(eye, input3DBox).render();
  //rotated and translated
  float[][] transform2 = translationMatrix(200, 200, 0); 
  input3DBox = transformBox(input3DBox, transform2); 
  projectBox(eye, input3DBox).render();
  //rotated, translated, and scaled
  float[][] transform3 = scaleMatrix(2, 2, 2); 
  input3DBox = transformBox(input3DBox, transform3); 
  projectBox(eye, input3DBox).render();
}

void mouseDragged() 
{
  float delta = mouseX - lastMouseX;
  if (delta == 0) {
    return;
  }
  else if (delta > 0) {
    delta = -STEP;
  }
  else {
    delta = STEP;
  }
  
  lastMouseX = mouseX;
  
  scale = min(max(scale + delta, 0), 2);
}

void keyPressed() {
  if (key == CODED) {
    if (keyCode == UP) {
      alpha += ANGULAR_SPEED;
    } else if (keyCode == DOWN) {
      alpha -= ANGULAR_SPEED;
    }
    if (keyCode == LEFT) {
      beta -= ANGULAR_SPEED;
    } else if (keyCode == RIGHT) {
      beta += ANGULAR_SPEED;
    }
  }
}

My2DPoint projectPoint(My3DPoint eye, My3DPoint p) {
  float xp = (p.x - eye.x)*(- eye.z/(p.z - eye.z));
  float yp = (p.y - eye.y)*(- eye.z/(p.z - eye.z));
  return new My2DPoint(xp, yp);
}

My2DBox projectBox (My3DPoint eye, My3DBox box) {
  box = transformBox(box, rotateXMatrix(alpha));
  box = transformBox(box, rotateYMatrix(beta));
  box = transformBox(box, scaleMatrix(NORMALIZATION + scale, NORMALIZATION + scale, NORMALIZATION + scale));
  My2DPoint[] s = new My2DPoint[box.p.length];

  for (int i = 0; i < box.p.length; i++) {
    s[i] = projectPoint(eye, box.p[i]);
  }

  return new My2DBox(s);
}

float[] homogeneous3DPoint (My3DPoint p) { 
  float[] result = {
    p.x, p.y, p.z, 1
  };
  return result;
}

float[][] rotateXMatrix(float angle) { 
  return(new float[][] {{1, 0, 0, 0},
                        {0, cos(angle), sin(angle), 0}, 
                        {0, -sin(angle), cos(angle), 0}, 
                        {0, 0, 0, 1}});
}

float[][] rotateYMatrix(float angle) {
  return(new float[][] {{cos(angle), 0, sin(angle), 0},
                        {0, 1, 0, 0},
                        {-sin(angle), 0, cos(angle), 0},  
                        {0, 0, 0, 1}});
}

float[][] rotateZMatrix(float angle) {
  return(new float[][] {{cos(angle), -sin(angle), 0, 0},  
                        {sin(angle), cos(angle), 0, 0},
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
  float[] res = new float[a.length];
  
  for( int i = 0; i < a.length; i ++) {
    for( int j = 0; j < a[i].length; j ++) {
      res[i] += a[i][j]*b[j];
    }
  } 
  
  return res;
}

My3DBox transformBox(My3DBox box, float[][] transformMatrix) {
  My3DPoint[] transformed = new My3DPoint[box.p.length];
  
  for( int i = 0; i < box.p.length; i ++) {
    transformed[i] = euclidian3DPoint(matrixProduct(transformMatrix, homogeneous3DPoint(box.p[i])));
  }
  
  return new My3DBox(transformed);
}

My3DPoint euclidian3DPoint (float[] a) {
  My3DPoint result = new My3DPoint(a[0]/a[3], a[1]/a[3], a[2]/a[3]);
  return result;
}

class My2DBox {
  My2DPoint[] s;

  My2DBox(My2DPoint[] s) {
    this.s = s;
  }

  void render() {
    drawFace(0, 3);
    drawFace(4, 7);
    for (int i = 0; i < 4; i++) {
      line(s[i].x, s[i].y, s[i + 4].x, s[i + 4].y);
    }
  }

  void drawFace(int from, int to) {

    for (int i = from; i < to; i++) {
      line(s[i].x, s[i].y, s[i + 1].x, s[i + 1].y);
    }

    line(s[from].x, s[from].y, s[to].x, s[to].y);
  }
}

class My3DBox {
  My3DPoint[] p;

  My3DBox(My3DPoint origin, float dimX, float dimY, float dimZ) {
    float x = origin.x;
    float y = origin.y;
    float z = origin.z;
    this.p = new My3DPoint[] {  
      new My3DPoint(x, y+dimY, z+dimZ), 
      new My3DPoint(x, y, z+dimZ), 
      new My3DPoint(x+dimX, y, z+dimZ), 
      new My3DPoint(x+dimX, y+dimY, z+dimZ), 

      new My3DPoint(x, y+dimY, z), 
      origin, 
      new My3DPoint(x+dimX, y, z), 
      new My3DPoint(x+dimX, y+dimY, z)
    };
  }

  My3DBox(My3DPoint[] p) {
    this.p = p;
  }
}


class My2DPoint { 
  float x;
  float y;

  My2DPoint(float x, float y) {
    this.x = x;
    this.y = y;
  }
}

class My3DPoint extends My2DPoint {
  float z;

  My3DPoint(float x, float y, float z) {
    super(x, y);
    this.z = z;
  }
}

