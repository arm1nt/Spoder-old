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
    private Link link;
    private final Set<Link> newFoundLinks = new HashSet<>();
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

        BufferedReader in = null;

        try {

            if (link.toString().startsWith("http://")) {
                HttpURLConnection httpConnection = this.connection.establishHttpConnection(link.toString());

                in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), Charset.defaultCharset()));

                readInput(in);

                in.close();
                httpConnection.disconnect();
            } else if (link.toString().startsWith("https://")) {
                HttpsURLConnection httpsConnection = this.connection.establishHttpsConnection(link.toString());

                in = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream(), Charset.defaultCharset()));

                readInput(in);

                in.close();
                httpsConnection.disconnect();
            } else {
                throw new MalformedURLException("Invalid Protocol");
            }

            for (Link link_temp : this.newFoundLinks) {
                threadPoolManager.submit(new Scanner(threadPoolManager, parser, this.connection, link_temp, depth-1));
            }

        } catch (IOException e) {
            System.out.println("ERROR: " + e.getMessage());
            threadPoolManager.decrement();
            return;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
                if (attributes.toString().split(" ")[0].contains("base")) {
                    this.setBaseUrl(getHrefOfBaseTag(attributes.toString()));
                } else {
                    this.newFoundLinks.addAll(parser.parseAttributes(attributes.toString(), this.link));
                }

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
     * If we find a base tag in the head of the html element, we replace the current
     * Link with a new link based on the href attribute of the base tag.
     *
     * @param baseUrl value of the href attribute of the base tag.
     */
    private void setBaseUrl(String baseUrl) {
        if (baseUrl == null) return;

        if (urlStartsWithProtocol(baseUrl)) {
            this.link = new Link(null, baseUrl);
        } else {
            this.link = new Link(link.toString(), baseUrl);
        }
    }


    /**
     * Returns the value of the href attribute.
     *
     * @param attributeTag base tags, whose href value should be extracted
     * @return null if the tag does not have a href attribute, otherwise the value of the href attribute
     */
    private String getHrefOfBaseTag(String attributeTag) {
        int index = attributeTag.indexOf("href");

        if (index == -1) return null;

        char valueDelim = attributeTag.charAt(index+5);
        String valueAfterFirstDelim = attributeTag.substring(index+6);
        int indexOfEndDelim = valueAfterFirstDelim.indexOf(valueDelim);

        return valueAfterFirstDelim.substring(0, indexOfEndDelim);
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
