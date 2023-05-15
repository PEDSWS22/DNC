package org.networkcalculus.dnc.omnet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

/**
 * A utility class for extracting values from OMNeT++ Scalar Result Files (Sca files).
 */
public class ScaExtractor {

    /**
     * Exception thrown when a value is not found in the sca file.
     */
    static class ValueNotFoundException extends Exception {
        public ValueNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Retrieves the end-to-end delay value from the specified result file.
     *
     * @param resultFile The path to the result file.
     * @return The end-to-end delay value, or -1 if it was not found or an error occurred.
     */
    public static double getSimulationEndToEndDelay(Path resultFile) throws ValueNotFoundException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(resultFile.toFile()))) {
            String line;
            boolean foundStatistic = false;

            while ((line = bufferedReader.readLine()) != null) {
                // Check if we found the statistic section
                String[] spaceSeparated = line.split(" ");

                if (!foundStatistic) {
                    // If the line does not start with statistic, lets skip it
                    if (!line.startsWith(STATISTIC_IDENTIFIER)) {
                        continue;
                    }

                    // Check if the header is valid
                    if (spaceSeparated.length < 3) {
                        throw new ValueNotFoundException("statistic field with invalid syntax found in sca file:\n" + line);
                    }

                    // Matching against measurementRecorder is possible because our FOI is the only defined recorder.
                    // It is not needed to match against the specific flow name.
                    if (spaceSeparated[SECTION_NODE_NAME_IDX].contains(MEASUREMENT_RECORDER_IDENTIFIER) &&
                            spaceSeparated[SECTION_STATISTIC_NAME_IDX].contains(MEAN_BIT_LIFE_TIME_PER_PACKET_IDENTIFIER)) {
                        foundStatistic = true;
                    }
                } else {
                    // We found something, lets see if it conforms to our specifications
                    if (spaceSeparated.length < 3) {
                        throw new ValueNotFoundException("encountered invalid data format in statistic section:\n" + line);
                    }

                    // We ran out of fields, continue searching elsewhere
                    if (!spaceSeparated[FIELD_TYPE_IDX].equals(FIELD_TYPE_FIELD)) {
                        System.err.println("the statistic section we tried to find had no fields directly after it:\n" + line);
                        foundStatistic = false;
                        continue;
                    }

                    // If the field name is "mean" we found our match
                    if (spaceSeparated[FIELD_NAME_IDX].equals(FIELD_MEAN)) {
                        return Double.parseDouble(spaceSeparated[FIELD_VALUE_IDX]);
                    }
                }
            }
        } catch (IOException e) {
            throw new ValueNotFoundException("error while trying to open file\n" + e.getMessage());
        }

        throw new ValueNotFoundException("mean value not found in scalar file");
    }

    private static final int SECTION_NODE_NAME_IDX = 1;
    private static final int SECTION_STATISTIC_NAME_IDX = 2;
    private static final int FIELD_TYPE_IDX = 0;
    private static final int FIELD_NAME_IDX = 1;
    private static final int FIELD_VALUE_IDX = 2;
    private static final String STATISTIC_IDENTIFIER = "statistic";
    private static final String MEASUREMENT_RECORDER_IDENTIFIER = "measurementRecorder";
    private static final String MEAN_BIT_LIFE_TIME_PER_PACKET_IDENTIFIER = "meanBitLifeTimePerPacket";
    private static final String FIELD_TYPE_FIELD = "field";
    private static final String FIELD_MEAN = "mean";
}
