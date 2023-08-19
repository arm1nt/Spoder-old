/**
 * Class that contains information about an url, e.g. depth, parent-url, etc.
 */
public class Link {

    private int depth;
    private boolean successfullyConnected;

    private final String parent;
    private final String url;

    Link(String parent, String url) {
        this.parent = parent;
        this.url = url;
    }

    @Override
    public String toString() {
        if (parent == null) return url;

        return parent + url;
    }
}
