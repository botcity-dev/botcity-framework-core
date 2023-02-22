package dev.botcity.framework.web;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;

import org.marvinproject.framework.image.MarvinImage;
import org.marvinproject.plugins.collection.MarvinPluginCollection;

public class CVFind {

    private final double COLOR_SENSIBILITY = 0.04;


    /**
     * Threshold image.
     * <p>
     *
     * @param image source image.
     * @param value threshold value.
     * @return thresholded image.
     */
    public MarvinImage threshold(MarvinImage image, int value) {
        MarvinImage result = image.clone();
        MarvinPluginCollection.thresholding(result, value);
        return result;
    }

    /**
     * Grayscale image.
     * <p>
     *
     * @param image source image.
     * @return grayscaled image.
     */
    public MarvinImage grayscale(MarvinImage image) {
        MarvinImage result = image.clone();
        MarvinPluginCollection.grayScale(image, result);
        return result;
    }

    /**
     * Find all elements in the image.
     * <p>
     *
     * @param visualElement visual element to find.
     * @param screenElement screen element to find.
     * @param region        The region to search.
     * @param matching      The matching index ranging from 0 to 1.
     * @param returnFirst   If true, returns the first element found.
     * @return List of found elements, empty otherwise.
     */
    public List<State> findAllElements(MarvinImage visualElement, MarvinImage screenElement, Region region, double matching, boolean returnFirst) {

        if (region == null) {
            region = new Region(0, 0, screenElement.getWidth(), screenElement.getHeight());
        }

        int subImagePixels = visualElement.getWidth() * visualElement.getHeight();
        boolean[][] processed = new boolean[screenElement.getWidth()][screenElement.getHeight()];

        List<State> elements = new ArrayList<>();

        int r1, g1, b1, r2, g2, b2;
        for (int y = region.getY(); y < region.getY() + region.getHeight(); y++) {
            for (int x = region.getX(); x < region.getX() + region.getWidth(); x++) {

                if (processed[x][y]) {
                    continue;
                }

                int notMatched = 0;
                boolean match = true;
                int colorThreshold = (int) (255 * this.COLOR_SENSIBILITY);

                if (y + visualElement.getHeight() < screenElement.getHeight() && x + visualElement.getWidth() < screenElement.getWidth()) {
                    for (int i = 0; i < visualElement.getHeight(); i++) {
                        for (int j = 0; j < visualElement.getWidth(); j++) {

                            if (processed[x + j][y + i]) {
                                match = false;
                                break;
                            }

                            r1 = screenElement.getIntComponent0(x + j, y + i);
                            g1 = screenElement.getIntComponent1(x + j, y + i);
                            b1 = screenElement.getIntComponent2(x + j, y + i);

                            r2 = visualElement.getIntComponent0(j, i);
                            g2 = visualElement.getIntComponent1(j, i);
                            b2 = visualElement.getIntComponent2(j, i);

                            if (Math.abs(r1 - r2) > colorThreshold || Math.abs(g1 - g2) > colorThreshold || Math.abs(b1 - b2) > colorThreshold) {
                                notMatched++;
                                if (notMatched > (1 - matching) * subImagePixels) {
                                    match = false;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    match = false;
                }

                if (match) {
                    elements.add(new State(x, y, visualElement.getWidth(), visualElement.getHeight(), 1.0 - ((double) notMatched / subImagePixels), true));
                    if (returnFirst) {
                        return elements;
                    }
                }
            }
        }

        return elements;
    }

    /**
     * Returns the element with the best score.
     * <p>
     *
     * @param elements List of elements to search.
     * @return Best element.
     */
    public State findBestElement(List<State> elements) {
        return elements.stream().max(Comparator.comparingDouble(State::getScore)).orElse(null);
    }
}
