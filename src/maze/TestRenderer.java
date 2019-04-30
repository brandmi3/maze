package maze;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import utils.OglUtils;

import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
/**
 * TYPES
 * 0-floor
 * 1-wall
 * 2-temp. floor
 * 3 temp. wall
 * 4
 */

/**
 * trida pro zobrazeni sceny v OpenGL:
 * kamera, skybox
 *
 * @author PGRF FIM UHK
 * @version 2015
 */
public class TestRenderer implements GLEventListener, MouseListener,
        MouseMotionListener, KeyListener {
    private static final double GRAVITY = 0.2;
    private static final int SPEED = 35;
    private double sila = 0;
    private static final int OBJECT_SCALE = 20;
    private static final int DISTANCE = 3; //odstup od zdi
    private static final int DEFAULT_Y = 2; //odstup od zdi
    private List<Integer> obstacles = new ArrayList<>();


    GLU glu;
    GLUT glut;


    int width, height, dx, dy, x, y;
    int ox, oy;

    float zenit;
    float azimut;
    double ex, ey, ez, px, py, pz, cenx, ceny, cenz, ux, uy, uz;
    float step, rot = 0, trans = 0;
    boolean per = true;
    double a_rad, z_rad;
    long oldmils = System.currentTimeMillis();
    boolean jump = false;

    File file;
    Texture SKY, WALL;
    float m[] = new float[16];
    int[][] map;

    @Override
    public void init(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        glu = new GLU();
        glut = new GLUT();
        glut = new GLUT();

        //výchozí poloha pozorovatele (vse bude*(-1))
        px = 30;
        py = DEFAULT_Y;
        pz = 30;

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
        gl.glPolygonMode(GL2.GL_BACK, GL2.GL_FILL);

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        gl.glShadeModel(GL2.GL_SMOOTH);

        OglUtils.printOGLparameters(gl);

//load mapy
        map = loadMap("map.txt");
        obstacles.add(1);
        obstacles.add(3);
//lights
        float[] light_amb = new float[]{1, 1, 1, 1};
        // nastaveni zdroje svetla - difusni slozka
        float[] light_dif = new float[]{1, 1, 1, 1};
        // nastaveni zdroje svetla - zrcadlova slozka
        float[] light_spec = new float[]{0.3f, 0, 0, 1};

        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_AMBIENT, light_amb, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, light_dif, 0);
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_SPECULAR, light_spec, 0);

        System.out.println("Loading SKY...");
        InputStream is = getClass().getResourceAsStream("/11.jpg"); // vzhledem k adresari res v projektu
        if (is == null)
            System.out.println("File not found");
        else
            try {
                SKY = TextureIO.newTexture(is, true, "jpg");
            } catch (IOException e) {
                System.err.println("Chyba cteni souboru s texturou");
            }
        System.out.println("Loading WALL...");
        is = getClass().getResourceAsStream("/wall.jpg"); // vzhledem k adresari res v projektu
        if (is == null)
            System.out.println("File not found");
        else
            try {
                WALL = TextureIO.newTexture(is, true, "jpg");
            } catch (IOException e) {
                System.err.println("Chyba cteni souboru s texturou");
            }
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);

    }

    private int[][] loadMap(String fileName) {

        //Get file from resources folder
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());

        try (Scanner scanner = new Scanner(file)) {
            String firstline = scanner.nextLine();
            int width = 0;
            int height = 0;
            String[] linePositions = firstline.split(",");
            width = new Integer(linePositions[0]);
            height = new Integer(linePositions[1]);

            int[][] map = new int[height][width];
            int y = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                linePositions = line.split(",");
                for (int x = 0; x < linePositions.length; x++) {
                    if (x < width && y < height)
                        map[y][x] = new Integer(linePositions[x]);
                }
                y++;
            }
            return map;

        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void display(GLAutoDrawable glDrawable) {
        move();
        GL2 gl = glDrawable.getGL().getGL2();

        long mils = System.currentTimeMillis();
        step = (mils - oldmils) / 1000.0f;
        //float fps = 1000 / (float) (mils - oldmils);
        oldmils = mils;
        trans = SPEED * step;
        rot += 360 * step / 10f;

        float[] light_position = new float[]{30, 50, 0.0f};
        gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_POSITION, light_position, 0);

        // vymazani obrazovky a Z-bufferu
        gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

        float[] LightPosition = {0.0f, 0.0f, 0.0f, 1f};
        float fogColor[] = {0.3f, 0.3f, 0.3f, 1f};
        gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, LightPosition, 0);
        gl.glEnable(GL2.GL_LIGHT1);  // Enable Light One
        gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP2);  // Fog Mode
        gl.glFogfv(GL2.GL_FOG_COLOR, fogColor, 0);  // Set Fog Color
        gl.glFogf(GL2.GL_FOG_DENSITY, 0.02f);    // How Dense Will The Fog Be
        gl.glHint(GL2.GL_FOG_HINT, GL2.GL_DONT_CARE);  // Fog Hint Value
        gl.glFogf(GL2.GL_FOG_START, 0.0f);    // Fog Start Depth
        gl.glFogf(GL2.GL_FOG_END, 50.0f);      // Fog End Depth
//        gl.glEnable(GL2.GL_FOG);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        if (per)
            glu.gluPerspective(45, width / (float) height, 0.1f, 1500);
        else
            gl.glOrtho(-20 * width / (float) height, 20 * width
                    / (float) height, -20, 20, 0.1f, 50.0f);

        gl.glMatrixMode(GL2.GL_MODELVIEW);


        /** TEST kolize*/


        //transformace pro sky box

        /** vykresleni dratove koule*/

//		 typ sky boxu
        skyBox2(gl);
        gl.glPopMatrix();

        // transformace sceny

        gl.glLoadIdentity();

        ux = 0;
        uy = 1;
        uz = 0;


        glu.gluLookAt(px, py, pz, px - ex, py - ey, pz - ez, ux, uy, uz);


        gl.glPushMatrix();

        /**objekty sceny*/

        for (int i = 0; i < map.length; i++) {
            for (int j = 0; j < map[0].length; j++) {
                gl.glPushMatrix();
                int mapPartType = map[i][j];
                boolean changeType = false;
                if (mapPartType == 2 || mapPartType == 3) {
                    if (Math.random() < 0.001) {
                        changeType = true;
                    }
                    if (changeType) {
                        if (mapPartType == 2) {
                            map[i][j] = 3;
                            mapPartType = 3;
                        } else {
                            map[i][j] = 2;
                            mapPartType = 2;
                        }
                    }
                }
                if (mapPartType == 2 || mapPartType == 3 || mapPartType == 1) {

                    gl.glTranslated(j * OBJECT_SCALE + OBJECT_SCALE / 2, OBJECT_SCALE, i * OBJECT_SCALE + OBJECT_SCALE / 2);
                    renderBlock(gl);
                    gl.glPopMatrix();
                    gl.glPushMatrix();
                }

                switch (mapPartType) {
                    case 0:
                    case 2:
                        //floor

                        gl.glTranslated(j * OBJECT_SCALE + OBJECT_SCALE / 2, -OBJECT_SCALE / 2, i * OBJECT_SCALE + OBJECT_SCALE / 2);
                        renderFloor(gl);


                        break;
                    case 1:
                    case 3:
                        //wall

                        gl.glTranslated(j * OBJECT_SCALE + OBJECT_SCALE / 2, 0, i * OBJECT_SCALE + OBJECT_SCALE / 2);
                        renderBlock(gl);

//                        gl.glTranslated(-j * OBJECT_SCALE + OBJECT_SCALE / 2, 0, -i * OBJECT_SCALE + OBJECT_SCALE / 2);
                      /*  gl.glColor3f(1f, 1f, 1f);

                        SKY.enable(gl);
                        SKY.bind(gl);
                        gl.glTranslated(j * OBJECT_SCALE + OBJECT_SCALE / 2, 0, i * OBJECT_SCALE + OBJECT_SCALE / 2);
                        gl.glScaled(OBJECT_SCALE, OBJECT_SCALE, OBJECT_SCALE);
                        glut.glutSolidCube(1);
*/


                        break;

                }
                gl.glPopMatrix();

            }
        }

        String text = "WASD pohyb, drag mysi pohled";


        OglUtils.drawStr2D(glDrawable, 3, height - 20, text);
    }

    private void renderFloor(GL2 gl) {
        gl.glScaled(OBJECT_SCALE, 1, OBJECT_SCALE);
        gl.glColor3d(1, 0, 1);
        glut.glutSolidCube(1);

    }

    public void renderBlock(GL2 gl) {
        gl.glColor3d(0.5, 0.5, 0.5);

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);

//        gl.glRotated(1, 2d, 3d, 0);
        WALL.enable(gl);
        WALL.bind(gl);
        gl.glBegin(GL2.GL_QUADS);
        int vertexSize = OBJECT_SCALE / 2;

        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3d(-vertexSize, -vertexSize, -vertexSize);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3d(-vertexSize, vertexSize, -vertexSize);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3d(-vertexSize, vertexSize, vertexSize);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3d(-vertexSize, -vertexSize, vertexSize);

        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3d(vertexSize, -vertexSize, -vertexSize);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3d(vertexSize, vertexSize, -vertexSize);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3d(vertexSize, vertexSize, vertexSize);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3d(vertexSize, -vertexSize, vertexSize);

        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3d(-vertexSize, -vertexSize, -vertexSize);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3d(vertexSize, -vertexSize, -vertexSize);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3d(vertexSize, -vertexSize, vertexSize);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3d(-vertexSize, -vertexSize, vertexSize);

        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3d(-vertexSize, vertexSize, -vertexSize);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3d(vertexSize, vertexSize, -vertexSize);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3d(vertexSize, vertexSize, vertexSize);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3d(-vertexSize, vertexSize, vertexSize);

        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3d(-vertexSize, vertexSize, -vertexSize);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3d(-vertexSize, -vertexSize, -vertexSize);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3d(vertexSize, -vertexSize, -vertexSize);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3d(vertexSize, vertexSize, -vertexSize);

        gl.glTexCoord2f(1.0f, 0.0f);
        gl.glVertex3d(-vertexSize, vertexSize, vertexSize);
        gl.glTexCoord2f(1.0f, 1.0f);
        gl.glVertex3d(-vertexSize, -vertexSize, vertexSize);
        gl.glTexCoord2f(0.0f, 1.0f);
        gl.glVertex3d(vertexSize, -vertexSize, vertexSize);
        gl.glTexCoord2f(0.0f, 0.0f);
        gl.glVertex3d(vertexSize, vertexSize, vertexSize);
        gl.glEnd();


        gl.glDisable(GL2.GL_TEXTURE_2D);

    }

    private double computeY(double y) {
        double vzdalenost = Math.abs(y + DEFAULT_Y);

        if (sila > 0) {
            sila = sila - (GRAVITY / vzdalenost);
            y += sila;
        } else {
            sila = 0;
            jump = false;
        }
        System.out.println(vzdalenost + " " + sila + " " + y);

        return y;

    }


    /**
     * DEFINOVANI NEBE
     */
    private double computeCoordsByAzimut() {
        double num = Math.abs(1);
        System.out.println("num " + ((azimut % 180) - 90));
        int iPart = (int) num;
        double fPart = num - iPart;
        return 0;

    }

    private void move() {
        if (pressed.size() > 0) {
            for (int keyCode : pressed) {
                if (keyCode == KeyEvent.VK_W) {
                    double pomx = px - ex * trans;
                    double pomz = pz - ez * trans;
                    computeCoordinates(pomx, py, pomz);
                }
                if (keyCode == KeyEvent.VK_S) {
                    double pomx = px + ex * trans;
                    double pomz = pz + ez * trans;
                    computeCoordinates(pomx, py, pomz);
                }
                if (keyCode == KeyEvent.VK_A) {
                    double pomx = px - Math.sin(a_rad - Math.PI / 2) * trans;
                    double pomz = pz + Math.cos(a_rad - Math.PI / 2) * trans;
                    computeCoordinates(pomx, py, pomz);
                }
                if (keyCode == KeyEvent.VK_D) {
                    double pomx = px + Math.sin(a_rad - Math.PI / 2) * trans;
                    double pomz = pz - Math.cos(a_rad - Math.PI / 2) * trans;
                    computeCoordinates(pomx, py, pomz);
                }
                if (keyCode == KeyEvent.VK_SPACE) {
                    py += 5;
                    if (!jump) {
                        jump = true;
                        sila = 5;
                    }
                }
                if (keyCode == KeyEvent.VK_CONTROL) {
                    py -= 5;

                }

            }
        }
    }

    public void skyBox2(GL2 gl) {
        gl.glColor3d(0.5, 0.5, 0.5);

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);

        SKY.enable(gl);
        SKY.bind(gl);

        GLUquadric quadric = glu.gluNewQuadric();
        glu.gluQuadricTexture(quadric, true);
        glu.gluQuadricNormals(quadric, GLU.GLU_SMOOTH);
        glu.gluSphere(quadric, 1000f, 20, 20);
        glu.gluDeleteQuadric(quadric);

        gl.glDisable(GL2.GL_TEXTURE_2D);
    }


    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
                        int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public synchronized void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
        }
        ox = e.getX();
        oy = e.getY();
    }

    @Override
    public synchronized void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {
        }
    }

    //rozhlizeni
    @Override
    public synchronized void mouseDragged(MouseEvent e) {
        dx = e.getX() - ox;
        dy = e.getY() - oy;
        ox = e.getX();
        oy = e.getY();

        zenit += dy;
        if (zenit > 60)
            zenit = 60;
        if (zenit <= -45)
            zenit = -45;
        azimut += dx;
        azimut = azimut % 360;
        if (azimut < 0) azimut = 360;
        a_rad = azimut * Math.PI / 180;
        z_rad = zenit * Math.PI / 180;
        ex = Math.sin(a_rad) * Math.cos(z_rad);
        ey = Math.sin(z_rad);
        ez = -Math.cos(a_rad) * Math.cos(z_rad);
        ux = Math.sin(a_rad) * Math.cos(z_rad + Math.PI / 2);
        uy = Math.sin(z_rad + Math.PI / 2);
        uz = -Math.cos(a_rad) * Math.cos(z_rad + Math.PI / 2);
    }

    @Override
    public synchronized void mouseMoved(MouseEvent e) {


    }

    private final Set<Integer> pressed = new HashSet<Integer>();

    @Override
    public synchronized void keyPressed(KeyEvent e) {

        pressed.add(e.getKeyCode());

    }


    private void computeCoordinates(double x, double y, double z) {
        /**pomx a pomz jsou 'budouci souřadnice
         * a ty se testuji jesti se tam muze posunout' */
        //  px = px - ex * trans;
        // pz = pz - ez * trans;
        double pomx = x;
        double pomz = z/* + DISTANCE*/;

        //horni
        int indexX1 = (int) ((pomx + DISTANCE) / OBJECT_SCALE);
        int indexY1 = (int) (pomz / OBJECT_SCALE);

        //dolni
        int indexX2 = (int) ((pomx - DISTANCE) / OBJECT_SCALE);
        int indexY2 = (int) (pomz / OBJECT_SCALE);

        //pravy
        int indexX3 = (int) (pomx / OBJECT_SCALE);
        int indexY3 = (int) ((pomz + DISTANCE) / OBJECT_SCALE);

        //levy
        int indexX4 = (int) (pomx / OBJECT_SCALE);
        int indexY4 = (int) ((pomz - DISTANCE) / OBJECT_SCALE);
/*
        pomz = z - DISTANCE;

        //horni
        int indexX5 = (int) ((pomx + DISTANCE) / OBJECT_SCALE);
        int indexY5 = (int) (pomz / OBJECT_SCALE);

        //dolni
        int indexX6 = (int) ((pomx - DISTANCE) / OBJECT_SCALE);
        int indexY6 = (int) (pomz / OBJECT_SCALE);

        //pravy
        int indexX7 = (int) (pomx / OBJECT_SCALE);
        int indexY7 = (int) ((pomz + DISTANCE) / OBJECT_SCALE);

        //levy
        int indexX8 = (int) (pomx / OBJECT_SCALE);
        int indexY8 = (int) ((pomz - DISTANCE) / OBJECT_SCALE);
*/
        boolean col1 = obstacles.contains(map[indexY1][indexX1]);
        boolean col2 = obstacles.contains(map[indexY2][indexX2]);
        boolean col3 = obstacles.contains(map[indexY3][indexX3]);
        boolean col4 = obstacles.contains(map[indexY4][indexX4]);
  /*      boolean col5 = obstacles.contains(map[indexY5][indexX5]);
        boolean col6 = obstacles.contains(map[indexY6][indexX6]);
        boolean col7 = obstacles.contains(map[indexY7][indexX7]);
        boolean col8 = obstacles.contains(map[indexY8][indexX8]);
*/
        if (col1) {
            System.out.println("__X+");

            // px = pomx - ex * trans;
//            if (!col5 &&!col6 && !col7&& !col8)
            if(!col2 && !col4 && !col3)
            pz = z;

            return;

        } else if (col2) {
            System.out.println("__X-");

            // px = pomx - ex * trans;
//            if (!col5 &&!col6 && !col7&& !col8)
            if(!col1 && !col4 && !col3)
            pz = z;

            return;

        } else if (col3) {
            System.out.println("__Z+");
//            if (!col5 &&!col6 && !col7&& !col8)
            if(!col1 && !col4 && !col2)
            px = x;
            //  pz = pomz - ez * trans;

            return;

        } else if (col4) {
            System.out.println("__Z-");
//            if (!col5 &&!col6 && !col7&& !col8)
            if(!col1 && !col2 && !col3)
            px = x;
            //  pz = pomz - ez * trans;

            return;

        } else {
            px = x;
            pz = z;
        }


    }

    @Override
    public synchronized void keyReleased(KeyEvent e) {
        pressed.remove(e.getKeyCode());

    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }

}