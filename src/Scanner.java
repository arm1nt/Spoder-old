import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

/**
 * Takes a link, sends a request to the website and extracts all links, emails, telephone numbers.
 * For every link found, a new Task can be added.
 */
public class Scanner implements Runnable {

    private final int depth;
    private final Link link;
    private final Set<String> newFoundLinks = new HashSet<>();
    private final Connection connection;
    private final Parser parser;
    private final ThreadPoolManager threadPoolManager;

    Scanner(ThreadPoolManager threadPoolManager, Parser parser, Connection connection, Link link, int depth) {
        this.threadPoolManager = threadPoolManager;
        this.parser = parser;
        this.connection = connection;
        this.link = link;
        this.depth = depth;
    }

    @Override
    public void run() {
        if (depth == 0) {
            threadPoolManager.decrement();
            return;
        }

        try {

            if (link.toString().startsWith("http://")) {
                HttpURLConnection httpConnection = this.connection.establishHttpConnection(link.toString());

                BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), Charset.defaultCharset()));

                readInput(in);

                in.close();
                httpConnection.disconnect();
            } else if (link.toString().startsWith("https://")) {
                HttpsURLConnection httpsConnection = this.connection.establishHttpsConnection(link.toString());

                BufferedReader in = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream(), Charset.defaultCharset()));

                readInput(in);

                in.close();
                httpsConnection.disconnect();
            } else {
                throw new MalformedURLException("Invalid Protocol");
            }

            for (String link_temp : this.newFoundLinks) {
                Link linkToAdd;
                if (urlStartsWithProtocol(link_temp)) {
                    linkToAdd = new Link(null, link_temp);
                } else {
                    linkToAdd = new Link(this.link.toString(), link_temp);
                }
                threadPoolManager.submit(new Scanner(threadPoolManager, parser, this.connection, linkToAdd, depth-1));
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
            threadPoolManager.decrement();
            return;
        }

        threadPoolManager.decrement();
    }


    /**
     * Read from the passed BufferedReader and sends the text between the tags and the text inside
     * the tags to the parser to extract links, emails, telephone numbers.
     *
     * @param in Reader
     * @throws IOException if an I/O error occurs
     */
    private void readInput(BufferedReader in) throws IOException {
        int input;
        boolean inside = false;
        StringBuilder content = new StringBuilder();
        StringBuilder attributes = new StringBuilder();
        boolean previous_char_blank = false;

        while ((input = in.read()) != -1) {
            if (input == '<') {
                if (content.length() > 0) {
                    this.newFoundLinks.addAll(parser.parseLine(content.toString(), this.link));
                    content.delete(0, content.length());
                }
                inside = true;
                continue;
            }

            if (input == '>') {
                this.newFoundLinks.addAll(parser.parseAttributes(attributes.toString(), this.link));
                attributes.delete(0, attributes.length());
                inside = false;
                continue;
            }

            if (inside) {
                attributes.append((char) input);
                continue;
            }

            if (input == 0xA) {
                if (previous_char_blank) continue;
                content.append(' ');
                previous_char_blank = true;
            } else if (input == 0xD || input == 0x09) {
                continue;
            } else if (input == 0x20) {
                if (previous_char_blank) {
                    continue;
                } else {
                    content.append((char) input);
                    previous_char_blank = true;
                }
            } else {
                previous_char_blank = false;
                content.append((char) input);
            }
        }
    }


    /**
     * Checks whether a given url starts with a valid {@link Main#ALLOWED_PROTOCOLS}.
     *
     * @param url url to be checked.
     * @return Returns true if the url starts with one of the {@link Main#ALLOWED_PROTOCOLS} and false otherwise.
     */
    private boolean urlStartsWithProtocol(String url) {
        for (String protocol : Main.ALLOWED_PROTOCOLS) {
            if (url.startsWith(protocol)) return true;
        }

        return false;
    }

}
