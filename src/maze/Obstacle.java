package maze;

import com.jogamp.opengl.util.texture.Texture;
import javafx.geometry.Point3D;

public class Obstacle {
    private ObstacleType type;
    private Point3D position;
    private Texture texture;
    private boolean moving;
    private boolean down;

    public Obstacle() {
    }

    public Obstacle(ObstacleType type, Point3D position, Texture texture) {
        this.type = type;
        this.position = position;
        this.texture = texture;
    }

    public ObstacleType getType() {
        return type;
    }

    public void setType(ObstacleType type) {
        this.type = type;
    }

    public Point3D getPosition() {
        return position;
    }

    public void setPosition(Point3D position) {
        this.position = position;
    }

    public Texture getTexture() {
        return texture;
    }

    public void setTexture(Texture texture) {
        this.texture = texture;
    }

    public double getX() {
        return position.getX();
    }

    public double getY() {
        return position.getY();
    }

    public double getZ() {
        return position.getZ();
    }

    public boolean isMoving() {
        return moving;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public boolean isDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public void setY(double y) {
        this.position = new Point3D(getX(), y, getZ())
        ;
    }

}
