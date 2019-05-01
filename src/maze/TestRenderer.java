package maze;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import javafx.geometry.Point3D;
import utils.OglUtils;

import java.awt.event.*;
import java.io.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static maze.ObstacleType.*;

public class TestRenderer implements GLEventListener, MouseListener,
        MouseMotionListener, KeyListener {

    private static final double GRAVITY = 0.2;
    private static final double WALL_SPEED = 0.03;
    private static final int SPEED = 35;
    private double sila = 0;
    private static final int OBJECT_SCALE = 20;
    private static final int DISTANCE = 3; //odstup od zdi
    private static final int DEFAULT_Y = 2; //odstup od zdi
    private static final double COLLISION_HEIGHT = -(OBJECT_SCALE / 2 - DEFAULT_Y);
    private double start_x;
    private double start_z;
    private boolean dead = false;
    private boolean moved = false;
    private boolean finish = false;

    GLU glu;
    GLUT glut;

    int width, height, dx, dy;
    int ox, oy;

    float zenit;
    float azimut;
    double ex, ey, ez, px, py, pz, ux, uy, uz;
    float step, rot = 0, trans = 0;
    boolean per = true;
    double a_rad, z_rad;
    long oldmils = System.currentTimeMillis();
    boolean jump = false;

    Obstacle[][] map;

    Texture SKY, WALL, FINISH, WHITE;

    @Override
    public void init(GLAutoDrawable glDrawable) {
        GL2 gl = glDrawable.getGL().getGL2();
        glu = new GLU();
        glut = new GLUT();
        glut = new GLUT();

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

        System.out.println("Loading FINISH...");
        is = getClass().getResourceAsStream("/finish.jpg"); // vzhledem k adresari res v projektu
        if (is == null)
            System.out.println("File not found");
        else
            try {
                FINISH = TextureIO.newTexture(is, true, "jpg");
            } catch (IOException e) {
                System.err.println("Chyba cteni souboru s texturou");
            }

        System.out.println("Loading WHITE...");
        is = getClass().getResourceAsStream("/white.jpg"); // vzhledem k adresari res v projektu
        if (is == null)
            System.out.println("File not found");
        else
            try {
                WHITE = TextureIO.newTexture(is, true, "jpg");
            } catch (IOException e) {
                System.err.println("Chyba cteni souboru s texturou");
            }

        loadMap("map.txt");

        px = start_x;
        py = DEFAULT_Y;
        pz = start_z;

        gl.glEnable(GL2.GL_DEPTH_TEST);
        gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
        gl.glPolygonMode(GL2.GL_BACK, GL2.GL_FILL);

        gl.glEnable(GL2.GL_LIGHTING);
        gl.glEnable(GL2.GL_LIGHT0);

        gl.glShadeModel(GL2.GL_SMOOTH);

        OglUtils.printOGLparameters(gl);


        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
        gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);

    }

    private void showMenu(GLAutoDrawable glDrawable) {
        if (!moved) {

            if (dead && !finish) {
                String text = "";
                text += "You are dead! Try it again!";

                OglUtils.drawStr2D(glDrawable, width / 2 - 2 * text.length(), height / 2 - 50, text);
            } else if (!finish) {
                String text = "WASD - movement";

                OglUtils.drawStr2D(glDrawable, width / 2 - 2 * text.length(), height / 2, text);
                text = "Mouse drag - looking around";

                OglUtils.drawStr2D(glDrawable, width / 2 - 2 * text.length(), height / 2 + 50, text);

            } else {
                String text = "!!! CONGRATULATION !!!";

                OglUtils.drawStr2D(glDrawable, width / 2 - 2 * text.length(), height / 2, text);

            }
        }
    }

    private void loadMap(String fileName) {

        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        int x = 0;
        int z = 0;
        try {
            Scanner scanner = new Scanner(file);

            String[] linePositions;

            while (scanner.hasNextLine()) {

                String line = scanner.nextLine();
                if (z == 0) {
                    linePositions = line.split(",");
                    x = linePositions.length;
                }
                z++;
            }

            int width = x;
            int height = z;
            System.out.println("MAP SIZE IS: " + width + " x " + height);
            map = new Obstacle[height][width];
            scanner = new Scanner(file);

            z = 0;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                linePositions = line.split(",");
                for (x = 0; x < linePositions.length; x++) {

                    Obstacle obstacle = new Obstacle();
                    obstacle.setPosition(new Point3D(x, DEFAULT_Y, z));
                    /**
                     * TYPES
                     * 0-floor
                     * 1-wall
                     * 2-temp. wall
                     * 3-start
                     * 4-finish
                     */
                    //vychozi pozice
                    if (linePositions[x].equals("3")) {
                        start_x = x * OBJECT_SCALE * 1.5;
                        start_z = z * OBJECT_SCALE * 1.5;
                    }

                    switch (linePositions[x]) {
                        case "0":
                        case "3":
                            obstacle.setType(E_FLORR);
                            obstacle.setTexture(WALL);
                            break;
                        case "1":
                            obstacle.setType(E_WALL);
                            break;
                        case "2":
                            obstacle.setType(T_WALL);
                            obstacle.setTexture(WALL);
                            break;
                        case "4":
                            obstacle.setType(ObstacleType.FINISH);
                            obstacle.setTexture(FINISH);
                            break;
                    }
                    map[z][x] = obstacle;
                }
                z++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        gl.glFogf(GL2.GL_FOG_DENSITY, 0.02f);    // How Dense Will The Fogmi Be
        gl.glHint(GL2.GL_FOG_HINT, GL2.GL_DONT_CARE);  // Fog Hint Value
        gl.glFogf(GL2.GL_FOG_START, 0.0f);    // Fog Start Depth
        gl.glFogf(GL2.GL_FOG_END, 50.0f);      // Fog End Depth
        gl.glEnable(GL2.GL_FOG);

        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();

        if (per)
            glu.gluPerspective(45, width / (float) height, 0.1f, 1500);
        else
            gl.glOrtho(-20 * width / (float) height, 20 * width
                    / (float) height, -20, 20, 0.1f, 50.0f);

        gl.glMatrixMode(GL2.GL_MODELVIEW);

        skyBox(gl);
        gl.glPopMatrix();

        gl.glLoadIdentity();

        glu.gluLookAt(px, py, pz, px - ex, py - ey, pz - ez, ux, uy, uz);

        /*minimapa*/
        for (Obstacle[] obstacles : map) {
            for (Obstacle obstacle : obstacles) {
                gl.glPushMatrix();

                gl.glTranslated(obstacle.getZ() + OBJECT_SCALE, obstacle.getX(), OBJECT_SCALE);
                renderBlock(gl, obstacle.getType(), true);

                gl.glPopMatrix();
            }
        }
        gl.glPushMatrix();

        /**objekty sceny*/
        for (Obstacle[] obstacles : map) {
            for (Obstacle obstacle : obstacles) {
                gl.glPushMatrix();
                if (obstacle.getType().equals(T_WALL)) {
                    if (!obstacle.isMoving()) {
                        if (Math.random() < 0.007) {
                            obstacle.setMoving(true);
                            obstacle.setDown(!obstacle.isDown());
                        }
                    } else {
                        if (obstacle.getY() < -OBJECT_SCALE && obstacle.isDown()) {
                            obstacle.setMoving(false);

                        } else if (obstacle.getY() > 0 && !obstacle.isDown()) {
                            obstacle.setMoving(false);
                        }
                        if (obstacle.isDown()) {
                            obstacle.setY(obstacle.getY() - WALL_SPEED);

                        } else {
                            obstacle.setY(obstacle.getY() + WALL_SPEED);
                        }
                    }
                    //kolize s padajici zdi

                    if (map[(int) pz / OBJECT_SCALE][(int) px / OBJECT_SCALE] == obstacle && obstacle.getY() < COLLISION_HEIGHT) {
                        endOfGame();
                    }
                }
                if (obstacle.getType().equals(ObstacleType.FINISH) && map[(int) pz / OBJECT_SCALE][(int) px / OBJECT_SCALE] == obstacle) {
                    finish();

                }
                double x = obstacle.getX();
                double y = obstacle.getY();
                double z = obstacle.getZ();
                if (obstacle.getType().equals(E_WALL) || obstacle.getType().equals(T_WALL)) {

                    gl.glTranslated(x * OBJECT_SCALE + OBJECT_SCALE / 2, OBJECT_SCALE, z * OBJECT_SCALE + OBJECT_SCALE / 2);
                    renderBlock(gl, E_WALL, false);
                    gl.glPopMatrix();
                    gl.glPushMatrix();
                }

                switch (obstacle.getType()) {
                    case E_FLORR:
                        //floor

                        gl.glTranslated(x * OBJECT_SCALE + OBJECT_SCALE / 2, -OBJECT_SCALE / 2, z * OBJECT_SCALE + OBJECT_SCALE / 2);
                        renderFloor(gl);

                        break;
                    case E_WALL:
                        gl.glTranslated(x * OBJECT_SCALE + OBJECT_SCALE / 2, 0, z * OBJECT_SCALE + OBJECT_SCALE / 2);
                        renderBlock(gl, E_WALL, false);

                        break;
                    case T_WALL:
                        //wall

                        gl.glTranslated(x * OBJECT_SCALE + OBJECT_SCALE / 2, y +OBJECT_SCALE, z * OBJECT_SCALE + OBJECT_SCALE / 2);
                        renderBlock(gl, T_WALL, false);

                        gl.glPopMatrix();
                        gl.glPushMatrix();

                        gl.glTranslated(x * OBJECT_SCALE + OBJECT_SCALE / 2, -OBJECT_SCALE / 2, z * OBJECT_SCALE + OBJECT_SCALE / 2);
                        renderFloor(gl);

                        break;
                    case FINISH:
                        gl.glTranslated(x * OBJECT_SCALE + OBJECT_SCALE / 2, -OBJECT_SCALE / 2, z * OBJECT_SCALE + OBJECT_SCALE / 2);
                        renderFloor(gl);

                        gl.glPopMatrix();
                        gl.glPushMatrix();

                        gl.glTranslated(x * OBJECT_SCALE + OBJECT_SCALE / 2, 0, z * OBJECT_SCALE + OBJECT_SCALE / 2);
                        renderBlock(gl, ObstacleType.FINISH, false);
                        break;
                }
                gl.glPopMatrix();
            }
        }
        showMenu(glDrawable);
    }

    private void renderFloor(GL2 gl) {
        gl.glScaled(OBJECT_SCALE, 1, OBJECT_SCALE);
        gl.glColor3d(1, 0, 1);
        glut.glutSolidCube(1);
    }

    private void endOfGame() {
        dead = true;
        moved = false;
        finish = false;

        px = start_x;
        pz = start_z;
    }

    private void finish() {
        finish = true;
        moved = false;


    }

    public void renderBlock(GL2 gl, ObstacleType type, boolean minimap) {
        gl.glColor3d(0.5, 0.5, 0.5);

        gl.glEnable(GL2.GL_TEXTURE_2D);
        gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE);
        if (minimap && type.equals(T_WALL))
            type = E_FLORR;
        switch (type) {
            case E_FLORR:
                WHITE.enable(gl);
                WHITE.bind(gl);
                break;
            case E_WALL:
            case T_WALL:
                WALL.enable(gl);
                WALL.bind(gl);
                break;
            case FINISH:
                FINISH.enable(gl);
                FINISH.bind(gl);
                if (!minimap) {
                    gl.glScaled(0.5, 0.5, 0.5);
                    gl.glTranslated(0, -OBJECT_SCALE / 2, 0);
                }
                break;
        }


        gl.glBegin(GL2.GL_QUADS);
        double vertexSize = OBJECT_SCALE / 2;
        if (minimap) {
            vertexSize = 0.5;
        }

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

    public void skyBox(GL2 gl) {
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
        if (!moved)
            moved = true;
        pressed.add(e.getKeyCode());
    }

    private void computeCoordinates(double x, double y, double z) {
        /**pomx a pomz jsou 'budouci souřadnice
         * a ty se testuji jesti se tam muze posunout' */

        double pomx = x;
        double pomz = z;

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


        boolean col1 = map[indexY1][indexX1].getType().equals(E_WALL) || (map[indexY1][indexX1].getType().equals(T_WALL) && map[indexY1][indexX1].getY() < COLLISION_HEIGHT);
        boolean col2 = map[indexY2][indexX2].getType().equals(E_WALL) || (map[indexY2][indexX2].getType().equals(T_WALL) && map[indexY2][indexX2].getY() < COLLISION_HEIGHT);
        boolean col3 = map[indexY3][indexX3].getType().equals(E_WALL) || (map[indexY3][indexX3].getType().equals(T_WALL) && map[indexY3][indexX3].getY() < COLLISION_HEIGHT);
        boolean col4 = map[indexY4][indexX4].getType().equals(E_WALL) || (map[indexY4][indexX4].getType().equals(T_WALL) && map[indexY4][indexX4].getY() < COLLISION_HEIGHT);


        if (col1) {
            if (!col2 && !col4 && !col3)
                pz = z;
            return;

        } else if (col2) {
            if (!col1 && !col4 && !col3)
                pz = z;
            return;

        } else if (col3) {
            if (!col1 && !col4 && !col2)
                px = x;
            return;

        } else if (col4) {
            if (!col1 && !col2 && !col3)
                px = x;
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