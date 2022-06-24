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
     * @param classLoader The class loader to use.
     * @param imagePath   The path to the resource.
     * @return The resource as a {@link MarvinImage}.
     */
    @SneakyThrows
    public static MarvinImage getResourceAsMarvinImage(ClassLoader classLoader, String imagePath) {
        if (new File(imagePath).exists()) {
            // getting image in local file
            return MarvinImageIO.loadImage(imagePath);
        }

        // getting image inside .jar
        InputStream is = classLoader.getResourceAsStream(imagePath);
        if (is == null) {
            throw new ResourceNotFoundException(imagePath + " not found at resources folder or inside jar file.");
        }

        BufferedImage bufferedImage = ImageIO.read(is);
        return new MarvinImage(bufferedImage, "png");
    }
}

