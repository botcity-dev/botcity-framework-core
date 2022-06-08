package dev.botcity.framework.web;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents a region on the screen.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Region {
    private int x;
    private int y;
    private int width;
    private int height;
}

