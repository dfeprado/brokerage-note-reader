package dev.dfeprado.brokeragenote.cmdrunner;

import com.beust.jcommander.Parameter;

@SuppressWarnings({"unused", "FieldMayBeFinal"})
class Arguments {
    // INPUT PARAMETERS
    @Parameter(names = {"--input-note", "-i"}, required = true,
            description = "The input brokerage note file's path")
    private String inputNote;

    // OUTPUT PARAMETERS
    @Parameter(names = {"--output-file", "-o"}, description = "The output file's path")
    private String outputFile;

    // MISC PARAMETERS
    @Parameter(names = {"--help", "-h"}, help = true, description = "Show this help.")
    private boolean help;


    // GETTERS
    public String getInputNote() {
        return inputNote;
    }

    public String getOutputFile() {
        return outputFile;
    }

    public boolean isHelp() {
        return help;
    }
}
