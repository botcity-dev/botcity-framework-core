package dev.botcity.framework.web;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.awt.Point;

/**
 * Represents a state of element found.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class State {
    private int x;
    private int y;
    private int width;
    private int height;
    private double score;
    private boolean available;

    /**
     * Returns the center point of the element.
     * <p>
     *
     * @return the center point of the element.
     */
    public Point getCenteredPosition() {
        if (!this.isAvailable()) {
            return new Point(0, 0);
        }

        return new Point(this.x + this.width / 2, this.y + this.height / 2);
    }
}
