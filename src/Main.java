import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import sun.misc.Signal;

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

        int numberOfThreadsToUse = Runtime.getRuntime().availableProcessors();
        String url = null;
        String cookies = null;
        String emailRegex = null;
        String linkRegex = null;
        String telephoneNumberRegex = null;
        int depth = -1;

        try {
            CommandLine line = commandLineParser.parse(options, args, false);

            url = line.getOptionValue("url");
            validateUrlProtocol(url);

            if (line.hasOption("threads")) {
                numberOfThreadsToUse = Integer.parseInt(line.getOptionValue("threads"));
            }

            if (line.hasOption("cookies")) {
                cookies = line.getOptionValue("cookies");
            }

            if (line.hasOption("depth")) {
                depth = Integer.parseInt(line.getOptionValue("depth"));
                validateDepth(depth);
            }

            if (line.hasOption("cookies")) {
                cookies = line.getOptionValue("cookies");
            }

            if (line.hasOption("depth") && !line.hasOption("recursive")) {
                throw new ParseException("Depth argument can only be used in combination with --recursive");
            }

        } catch (ParseException e) {
            printUsageAndHelp(options);
            System.err.println("\nError: " + e.getMessage());
            System.exit(64);
        }

        Connection connection = new Connection(cookies);
        Parser parser = new Parser(linkRegex, emailRegex, telephoneNumberRegex);
        ThreadPoolManager threadPoolManager = new ThreadPoolManager(numberOfThreadsToUse);

        setupSignalHandling(threadPoolManager);

        Scanner startThread = new Scanner(threadPoolManager, parser, connection, new Link(null, url), depth);
        threadPoolManager.submit(startThread);



        long start = System.currentTimeMillis();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            long end = System.currentTimeMillis();
            System.out.println("Duration: " + (end - start));

            System.out.println("Found links");
            Set<String> collectedLinks = parser.collectLinks();
            for (String collectedLink : collectedLinks) {
                System.out.println(collectedLink);
            }
            System.out.println("Number of links found: " + collectedLinks.size());

        }));
    }


    /**
     * Validates that the url starts with a valid {@link #ALLOWED_PROTOCOLS}
     *
     * @param url url to validate
     * @throws ParseException if url starts with not supported protocol
     */
    private static void validateUrlProtocol(String url) throws ParseException {
        for (String protocol : ALLOWED_PROTOCOLS) {
            if (url.startsWith(protocol)) return;
        }

        throw new ParseException("Only http and https protocol are supported");
    }


    /**
     * Validate that the given depths is greater than or equal to 1.
     *
     * @param depth depth to be validated
     * @throws ParseException Is thrown if the depth is < 1.
     */
    private static void validateDepth(int depth) throws ParseException {
        if (depth >= 1) return;

        throw new ParseException("Depth must be at least 1 or greater");
    }


    /**
     * Manages all accepted options.
     *
     * @return Option object with all accepted options set.
     */
    private static Options generateAcceptedCommandLineOptions() {
        Options options = new Options();

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

}
