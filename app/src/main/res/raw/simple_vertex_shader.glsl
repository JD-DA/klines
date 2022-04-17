uniform mat4 u_Matrix;

attribute vec4 a_Position;

varying vec4 v_Color;
 void  main()
 {
     gl_Position = u_Matrix*a_Position;
     gl_PointSize = 10.0;

 }