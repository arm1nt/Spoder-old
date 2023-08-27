import java.util.Objects;

/**
 * Class that contains information about an url, e.g. depth, parent-url, etc.
 */
public class Link {
    private final String parent;
    private final String url;

    Link(String parent, String url) {

        if (parent != null) {
            StringBuilder buildParentUrl = new StringBuilder();
            buildParentUrl.append(getProtocol(parent));

            String urlWithoutProtocol = removeProtocolFromUrl(parent);

            if (url.startsWith("?")) {
                //if parent ends with / remove the slash and append the query parameters
                //else just append the query parameters right away
                if (parent.endsWith("/")) {
                    buildParentUrl.append(urlWithoutProtocol.substring(0, urlWithoutProtocol.length()-1));
                } else {
                    buildParentUrl.append(urlWithoutProtocol);
                }
            } else if (url.startsWith("/")) {
                //if relative url starts with '/': append the relative url after the domain
                int index = urlWithoutProtocol.indexOf("/");

                if  (index == -1) {
                    buildParentUrl.append(urlWithoutProtocol);
                } else {
                    buildParentUrl.append(urlWithoutProtocol, 0, index);
                }
            } else {
                //if relative url does not start with '/' or '?' append relative url to the current path
                //so append it after the last occurrence of '/' in the parent url
                int index = urlWithoutProtocol.lastIndexOf("/");

                if (index == -1) {
                    buildParentUrl.append(urlWithoutProtocol).append("/");
                } else {
                    buildParentUrl.append(urlWithoutProtocol, 0, index+1);
                }
            }

            this.parent = buildParentUrl.toString();
            this.url = url;
            return;
        }

        this.parent = parent;
        this.url = url;
    }

    @Override
    public String toString() {
        if (parent == null) return url;

        return parent + url;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Link link = (Link) o;
        return Objects.equals(parent, link.parent) && Objects.equals(url, link.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent, url);
    }

    /**
     * Return the url without the protocol.
     *
     * @param url url whose protocol should be removed.
     * @return url without the protocol.
     */
    private String removeProtocolFromUrl(String url) {
        if (url.startsWith("http://")) return url.substring(7);

        return url.substring(8);
    }

    /**
     * Extract the protocol from the url.
     *
     * @param url url from which the protocol should be extracted.
     * @return the extracted protocol.
     */
    private String getProtocol(String url) {
        if (url.startsWith("http://")) return "http://";

        return "https://";
    }
}
