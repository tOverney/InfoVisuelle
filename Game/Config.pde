/*
 *  This file regroup all the constants of this project
 *  to avoid useless cluttering
 */
final static float EYE_AWAY = 800;

final static int FIELD_DIMENSION = 450;
final static int FIELD_THICKNESS = 20;
final static int SPHERE_RADIUS = 20;


final static float CYLINDER_RADIUS = 20;
final static float CYLINDER_HEIGHT = 40;
final static int CYLINDER_RES = 60;

final static float MIN_CYL_POSITION = - FIELD_DIMENSION / 2 + CYLINDER_RADIUS;
final static float MAX_CYL_POSITION = FIELD_DIMENSION / 2 - CYLINDER_RADIUS;

final static int MIN_SPHERE_POSITION = - FIELD_DIMENSION / 2 + SPHERE_RADIUS;
final static int MAX_SPHERE_POSITION = FIELD_DIMENSION / 2 - SPHERE_RADIUS;

final static int SIDE_VIEW = FIELD_DIMENSION + 50; 

final static float GRAVITY_COEF = 0.15;
final static float FRICTION_MAGNITUDE = 1 * 0.03;

final static float ANGULAR_CHANGE = radians(0.5);
final static float MAX_TILT = radians(60);

final static boolean DEBUG = false;