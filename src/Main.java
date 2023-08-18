import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import sun.misc.Signal;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

public class Main {

    public static final String[] ALLOWED_PROTOCOLS = {"http://", "https://"};


    public static void main(String[] args) {

        System.out.println("""
                   _____                 __        \s
                  / ___/____  ____  ____/ /__  _____
                  \\__ \\/ __ \\/ __ \\/ __  / _ \\/ ___/
                 ___/ / /_/ / /_/ / /_/ /  __/ /   \s
                /____/ .___/\\____/\\__,_/\\___/_/    \s
                    /_/                            \s
                """);


        CommandLineParser commandLineParser = new DefaultParser();
        Options options = generateAcceptedCommandLineOptions();

        CommandLineArguments commandLineArguments = new CommandLineArguments();

        try { //argument parsing could be refactored to method
            CommandLine line = commandLineParser.parse(options, args, false);

            commandLineArguments.setUrl(line.getOptionValue("url"));

            if (line.hasOption("output")) {
                commandLineArguments.setOutput(true);
                commandLineArguments.setOutputFile(line.getOptionValue("output"));
            }

            if (line.hasOption("cookies")) {
                commandLineArguments.setCookies(line.getOptionValue("cookies"));
            }

            if (line.hasOption("threads")) {
                commandLineArguments.setNumberOfThreads(Integer.parseInt(line.getOptionValue("threads")));
            }

            if (line.hasOption("depth")) {
                commandLineArguments.setDepth(Integer.parseInt(line.getOptionValue("depth")));
            } else {
                commandLineArguments.setDepthUnlimited();
            }

            if (!line.hasOption("recursive")) {
                commandLineArguments.setDepth(1);
            }

            if (line.hasOption("link")) {
                commandLineArguments.setLinkRegex(line.getOptionValue("link"));
            }

            if (line.hasOption("email")) {
                commandLineArguments.setEmailRegex(line.getOptionValue("email"));
            }

            if (line.hasOption("telephone")) {
                commandLineArguments.setTelephoneNumberRegex(line.getOptionValue("telephone"));
            }

            if (line.hasOption("depth") && !line.hasOption("recursive")) {
                throw new ParseException("Specifying a depth has no effect without specifying --recursive");
            }

        } catch (ParseException | IllegalArgumentException e) {
            printUsageAndHelp(options);
            System.err.println("\nError: " + e.getMessage());
            System.exit(64);
        }

        Connection connection = new Connection(commandLineArguments.getCookies());
        Parser parser = new Parser(
                commandLineArguments.getLinkRegex(),
                commandLineArguments.getEmailRegex(),
                commandLineArguments.getTelephoneNumberRegex());
        ThreadPoolManager threadPoolManager = new ThreadPoolManager(commandLineArguments.getNumberOfThreads());

        setupSignalHandling(threadPoolManager);

        Scanner startThread = new Scanner(
                threadPoolManager,
                parser,
                connection,
                new Link(null, commandLineArguments.getUrl()),
                commandLineArguments.getDepth());
        threadPoolManager.submit(startThread);



        long start = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long end = System.currentTimeMillis();
            System.out.println("Duration: " + (end - start) + "\n");

            try {
                writeOutput(
                        parser.collectLinks(),
                        commandLineArguments.isOutput(),
                        commandLineArguments.getOutputFile(),
                        "Links");

                writeOutput(
                        parser.collectEmails(),
                        commandLineArguments.isOutput(),
                        commandLineArguments.getOutputFile(),
                        "Emails");

                writeOutput(
                        parser.collectPhoneNumbers(),
                        commandLineArguments.isOutput(),
                        commandLineArguments.getOutputFile(),
                        "Phone numbers");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }));
    }


    /**
     * Manages all accepted options.
     *
     * @return Option object with all accepted options set.
     */
    private static Options generateAcceptedCommandLineOptions() {
        Options options = new Options();

        options.addOption(Option.builder("o")
                .longOpt("output")
                .required(false)
                .hasArg(true)
                .desc("Specify an output file")
                .valueSeparator('=')
                .build());

        options.addOption(Option.builder("t")
                .longOpt("threads")
                .hasArg(true)
                .required(false)
                .desc("Specify number of threads")
                .valueSeparator('=')
                .build());

        options.addOption(Option.builder("u")
                .longOpt("url")
                .required(true)
                .hasArg(true)
                .desc("Specify the URL")
                .valueSeparator('=')
                .build());

        options.addOption(Option.builder()
                .longOpt("email")
                .hasArg(true)
                .required(false)
                .desc("Provide a custom regular expression to match email addresses")
                .valueSeparator('=')
                .build());

        options.addOption(Option.builder()
                .longOpt("link")
                .hasArg(true)
                .required(false)
                .desc("Provide a custom regular expression to match links")
                .valueSeparator('=')
                .build());

        options.addOption(Option.builder()
                .longOpt("telephone")
                .hasArg(true)
                .required(false)
                .desc("Provide a custom regular expression to match telephone numbers")
                .valueSeparator('=')
                .build());

        options.addOption(Option.builder("r")
                .longOpt("recursive")
                .hasArg(false)
                .required(false)
                .desc("Specify whether Spoder should recursively follow newly found links")
                .valueSeparator('=')
                .build());

        options.addOption(Option.builder("d")
                .longOpt("depth")
                .required(false)
                .hasArg(true)
                .desc("Specify the recursive depth limit")
                .valueSeparator('=')
                .build());

        options.addOption(Option.builder()
                .longOpt("cookies")
                .required(false)
                .optionalArg(false)
                .hasArg(true)
                .desc("Add cookies seperated by ';' e.g. cookie1;cookie2;cookie3")
                .valueSeparator('=')
                .build());

        return options;
    }


    /**
     * Prints the usage message together with an overview of which options are allowed.
     *
     * @param options accepted options.
     */
    private static void printUsageAndHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("Spoder", options);
    }


    /**
     * Defines the behaviour if the process is interrupted by: SIGINT.
     */
    private static void setupSignalHandling(ThreadPoolManager threadPoolManager) {
        Signal.handle(new Signal("INT"), sig -> {
            System.out.println("Shutting down...");
            threadPoolManager.interrupt();
        });
    }

    private static void writeOutput(Set<String> output, boolean toFile, String fileName, String type) throws IOException {
        if (toFile) {
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true ));
            writer.write(type + ":\n\n");
            for (String line : output) {
                writer.write(line + "\n");
            }
            writer.write("\n Number of " + type.toLowerCase() + " found: " + output.size() + "\n");

            writer.write("""

                    ####################################################################################
                    ####################################################################################
                    """);
            writer.close();
            return;
        }

        StringBuilder collect = new StringBuilder();
        collect.append(type).append(":\n\n");

        for (String line : output) {
            collect.append(line).append("\n");
        }

        collect.append("\nNumber of ").append(type.toLowerCase()).append(" found: ").append(output.size()).append("\n");
        collect.append("""

                ####################################################################################
                ####################################################################################
                """);

        System.out.println(collect);

    }

}
