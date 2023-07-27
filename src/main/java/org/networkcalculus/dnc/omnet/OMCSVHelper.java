package org.networkcalculus.dnc.omnet;

import com.google.common.collect.ImmutableMap;
import org.networkcalculus.dnc.curves.ArrivalCurve;
import org.networkcalculus.dnc.curves.ServiceCurve;
import org.networkcalculus.dnc.network.server_graph.Flow;
import org.networkcalculus.dnc.network.server_graph.Server;
import org.networkcalculus.dnc.network.server_graph.ServerGraph;
import org.networkcalculus.num.Num;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * This class, OMCSVHelper, provides helper methods for working with CSV files in the context of the OMNET simulation.
 */
public class OMCSVHelper {
    public static String CSV_HEADER_FOI_ID = "foi_id";
    public static String CSV_HEADER_OMNET_E2E = "omnet_e2e";
    public static String CSV_HEADER_PMOO_E2E = "pmoo_e2e";

    /**
     * Default set of flow properties to be used for CSV generation.
     */
    public static SupportedFlowProps[] DEFAULT_FLOW_PROPS = new SupportedFlowProps[]{
            SupportedFlowProps.AC_RATE,
            SupportedFlowProps.AC_LATENCY,
            SupportedFlowProps.AC_BURST,
    };

    /**
     * Enumeration of supported flow properties for CSV generation.
     */
    public enum SupportedFlowProps {
        AC_RATE("f_%d_acr"),
        AC_BURST("f_%d_acb"),
        AC_LATENCY("f_%d_acl");

        private final String header;

        SupportedFlowProps(String header) {
            this.header = header;
        }

        /**
         * Get the csv header corresponding to the supported flow property.
         *
         * @return The header string.
         */
        public String header() {
            return header;
        }
    }

    /**
     * Functional interface for resolving flow properties.
     */
    @FunctionalInterface
    public interface FlowResolver {
        /**
         * Get the value of the flow property.
         *
         * @param f            The flow for which the property value is to be resolved.
         * @param numberFormat The number format to use for formatting the property value.
         * @return The property value as a string.
         */
        String get(Flow f, NumberFormat numberFormat);
    }

    /**
     * Default set of server properties to be used for CSV generation.
     */
    public static SupportedServerProps[] DEFAULT_SERVER_PROPS = new SupportedServerProps[]{
            SupportedServerProps.SC_RATE,
            SupportedServerProps.SC_LATENCY,
            SupportedServerProps.SC_BURST,
    };

    /**
     * Enumeration of supported server properties for CSV generation.
     */
    public enum SupportedServerProps {
        SC_RATE("s_%d_scr"),
        SC_BURST("s_%d_scb"),
        SC_LATENCY("s_%d_scl");

        private final String header;

        SupportedServerProps(String header) {
            this.header = header;
        }

        /**
         * Get the csv header corresponding to the supported server property.
         *
         * @return The header string.
         */
        public String header() {
            return header;
        }
    }

    /**
     * Functional interface for resolving server properties.
     */
    @FunctionalInterface
    public interface ServerResolver {
        /**
         * Get the value of the server property.
         *
         * @param s            The server for which the property value is to be resolved.
         * @param numberFormat The number format to use for formatting the property value.
         * @return The property value as a string.
         */
        String get(Server s, NumberFormat numberFormat);
    }

    /**
     * Mapping of flow properties to their respective resolvers.
     */
    public static final Map<SupportedFlowProps, FlowResolver> FlowProperties = ImmutableMap.of(
            SupportedFlowProps.AC_RATE, (Flow f, NumberFormat nf) -> nf.format(
                    Optional.ofNullable(f.getArrivalCurve())
                            .map(ArrivalCurve::getUltAffineRate)
                            .map(Num::doubleValue).orElse(Double.NaN)
            ),
            SupportedFlowProps.AC_BURST, (Flow f, NumberFormat nf) -> nf.format(
                    Optional.ofNullable(f.getArrivalCurve())
                            .map(ArrivalCurve::getBurst)
                            .map(Num::doubleValue).orElse(Double.NaN)
            ),
            SupportedFlowProps.AC_LATENCY, (Flow f, NumberFormat nf) -> nf.format(
                    Optional.ofNullable(f.getArrivalCurve())
                            .map(ArrivalCurve::getLatency)
                            .map(Num::doubleValue).orElse(Double.NaN)
            )
    );

    /**
     * Get the value of the specified flow property for the given flow.
     *
     * @param desiredProp  The desired flow property to be retrieved.
     * @param f            The flow for which the property value is to be retrieved.
     * @param numberFormat The number format to use for formatting the property value.
     * @return The property value as a string.
     */
    public static String getFlowValue(SupportedFlowProps desiredProp, Flow f, NumberFormat numberFormat) {
        return FlowProperties.get(desiredProp).get(f, numberFormat);
    }

    /**
     * Mapping of server properties to their respective resolvers.
     */
    public static final Map<SupportedServerProps, ServerResolver> ServerProperties = ImmutableMap.of(
            SupportedServerProps.SC_RATE, (Server s, NumberFormat nf) -> nf.format(
                    Optional.ofNullable(s.getServiceCurve())
                            .map(ServiceCurve::getUltAffineRate)
                            .map(Num::doubleValue).orElse(Double.NaN)
            ),
            SupportedServerProps.SC_BURST, (Server s, NumberFormat nf) -> nf.format(
                    Optional.ofNullable(s.getServiceCurve())
                            .map(ServiceCurve::getLatency)
                            .map(Num::doubleValue).orElse(Double.NaN)
            ),
            SupportedServerProps.SC_LATENCY, (Server s, NumberFormat nf) -> nf.format(
                    Optional.ofNullable(s.getServiceCurve())
                            .map(ServiceCurve::getLatency)
                            .map(Num::doubleValue).orElse(Double.NaN)
            )
    );

    /**
     * Get the value of the specified server property for the given server.
     *
     * @param desiredProp  The desired server property to be retrieved.
     * @param s            The server for which the property value is to be retrieved.
     * @param numberFormat The number format to use for formatting the property value.
     * @return The property value as a string.
     */
    public static String getServerValue(SupportedServerProps desiredProp, Server s, NumberFormat numberFormat) {
        return ServerProperties.get(desiredProp).get(s, numberFormat);
    }

    /**
     * Write the given ServerGraph information to a CSV file using the specified properties.
     *
     * @param csvFile                    The CSV file to write the data to.
     * @param sg                         The ServerGraph containing the network topology.
     * @param foi                        The Flow of Interest (FOI) for which data is to be written.
     * @param sime2e                     The end-to-end delay as computed by OMNET simulation.
     * @param delayBound                 The delay bound to be compared with OMNET end-to-end delay.
     * @param desiredCSVFlowProperties   Array of supported flow properties to be included in the CSV.
     * @param desiredCSVServerProperties Array of supported server properties to be included in the CSV.
     * @throws IOException If there is an error writing to the CSV file.
     */
    public static void ToFile(File csvFile, ServerGraph sg, Flow foi, double sime2e, double delayBound,
                              SupportedFlowProps[] desiredCSVFlowProperties,
                              SupportedServerProps[] desiredCSVServerProperties) throws IOException {

        // Format decimal values the proper way
        DecimalFormat nf = (DecimalFormat) DecimalFormat.getInstance();
        DecimalFormatSymbols csvDFS = new DecimalFormatSymbols();
        // Adjust the decimal separator, so we can use it in the csv
        csvDFS.setDecimalSeparator('.');
        nf.setDecimalFormatSymbols(csvDFS);

        // Dont use grouping so we dont get extra symbols
        nf.setGroupingUsed(false);
        // At most 5 fraction digits are enough
        nf.setMaximumFractionDigits(5);

        // Create a header and data line, we are going to write to these individually
        StringBuilder headerLine = new StringBuilder();
        StringBuilder dataLine = new StringBuilder();

        // Write the foi_id the omnet_e2e and the pmoo_e2e to the file
        headerLine.append(
                new StringJoiner(",")
                        .add(OMCSVHelper.CSV_HEADER_FOI_ID)
                        .add(OMCSVHelper.CSV_HEADER_OMNET_E2E)
                        .add(OMCSVHelper.CSV_HEADER_PMOO_E2E)
        );
        // Write the corresponding data values
        dataLine.append(
                new StringJoiner(",")
                        .add(String.valueOf(foi.getId()))
                        .add(nf.format(sime2e))
                        .add(nf.format(delayBound))
        );

        // As we can never be the first element, we can prefix everything with ',' here
        for (Flow f : sg.getFlows()) {
            // Dump the desired properties
            for (SupportedFlowProps prop : desiredCSVFlowProperties) {
                headerLine.append(",").append(String.format(prop.header(), f.getId()));
                dataLine.append(",").append(OMCSVHelper.getFlowValue(prop, f, nf));
            }
        }

        for (Server s : sg.getServers()) {
            for (SupportedServerProps prop : desiredCSVServerProperties) {
                headerLine.append(",").append(String.format(prop.header(), s.getId()));
                dataLine.append(",").append(OMCSVHelper.getServerValue(prop, s, nf));
            }
        }

        // Write the result.csv file
        FileWriter fileWriter = new FileWriter(csvFile);
        fileWriter.write(headerLine.toString());
        fileWriter.write("\n");
        fileWriter.write(dataLine.toString());
        fileWriter.write("\n");
        fileWriter.close();
    }
}
