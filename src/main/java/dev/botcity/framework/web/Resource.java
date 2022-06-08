package dev.botcity.framework.web;

import lombok.SneakyThrows;

import dev.botcity.framework.web.exceptions.ResourceNotFoundException;

import org.marvinproject.framework.io.MarvinImageIO;
import org.marvinproject.framework.image.MarvinImage;

import java.io.File;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class Resource {

    /**
     * Get the resource as a MarvinImage.
     * <p>
     *
     * @param clazz     The class that contains the resource.
     * @param imagePath The path to the resource.
     * @return The resource as a {@link MarvinImage}.
     */
    @SneakyThrows
    public static MarvinImage getResourceAsMarvinImage(Class<?> clazz, String imagePath) {
        if (new File(imagePath).exists()) {
            // getting image in local file
            return MarvinImageIO.loadImage(imagePath);
        }

        // getting image inside .jar
        InputStream is = clazz.getResourceAsStream(imagePath);
        if (is == null) {
            throw new ResourceNotFoundException(imagePath + " not found at /src/resources or inside jar file.");
        }

        BufferedImage bufferedImage = ImageIO.read(is);
        return new MarvinImage(bufferedImage, "png");
    }
}

