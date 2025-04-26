package dev.dfeprado.brokeragenote.cmdrunner;

import com.beust.jcommander.Parameter;

class Arguments {
  // INPUT PARAMETERS
  @Parameter(names = {"--input-note", "-i"}, required = true,
      description = "The input brokerage note file's path")
  private String inputNote;

  @Parameter(names = {"--input-type", "-it"}, required = true, description = "Input file's format")
  private InputType inputType = InputType.SINACOR;

  @Parameter(names = {"--input-format", "-if"}, required = true,
      description = "Input file's format")
  private InputFormat inputFormat = InputFormat.PDF;

  // OUTPUT PARAMETERS
  @Parameter(names = {"--output-file", "-o"}, description = "The output file's path")
  private String outputFile;

  @Parameter(names = {"--output-type", "-ot"}, required = true, description = "Output file type")
  private OutputFormat outputType = OutputFormat.STATUSINVEST;

  // MISC PARAMETERS
  @Parameter(names = {"--help", "-h"}, help = true, description = "Show this help.")
  private boolean help;


  // GETTERS
  public String getInputNote() {
    return inputNote;
  }

  public InputType getInputType() {
    return inputType;
  }

  public InputFormat getInputFormat() {
    return inputFormat;
  }

  public String getOutputFile() {
    return outputFile;
  }

  public OutputFormat getOutputType() {
    return outputType;
  }

  public boolean isHelp() {
    return help;
  }
}
