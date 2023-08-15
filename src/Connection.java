import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Class that allows to establish an HTTP / HTTPS connection.
 */
public class Connection {

    private static final String REQUEST_METHOD = "GET";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11";
    private final String cookies;


    Connection(String cookies) {
        this.cookies = cookies;
    }


    /**
     * Connects via https or Http to the website specified by the given link.
     *
     * @param link link of website to connect to
     * @return Connection object.
     * @throws IOException If the URL is malformed or there was an error connecting to the site.
     */
    public HttpsURLConnection establishHttpsConnection(String link) throws IOException {
        URL url = new URL(link);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        connection.setRequestMethod(REQUEST_METHOD);
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (cookies != null) {
            connection.setRequestProperty("Cookie", cookies);
        }

        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(500);
        connection.connect();

        //TODO: Parse response code

        return connection;
    }


    /**
     * Connects via http to the website specified by the given link.
     *
     * @param link link of website to connect to.
     * @return Connection object.
     * @throws IOException If the URL is malformed or there was an error connecting to site.
     */
    public HttpURLConnection establishHttpConnection(String link) throws IOException {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(REQUEST_METHOD);
        connection.setRequestProperty("User-Agent", USER_AGENT);

        if (cookies != null) {
            connection.setRequestProperty("Cookie", cookies);
        }

        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(500);
        connection.connect();

        //TODO: Parse response code

        return connection;
    }

}
