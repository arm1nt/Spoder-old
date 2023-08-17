/**
 * Helper class to manage the given arguments. For optional arguments a default value is assigned.
 */
public class CommandLineArguments {

    private String url;
    private String cookies;

    private int numberOfThreads = Runtime.getRuntime().availableProcessors() * 2;
    private int depth = 1;

    private String linkRegex;
    private String emailRegex;
    private String telephoneNumberRegex;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) throws IllegalArgumentException {

        validateUrlProtocol(url);
        this.url = url;
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = cookies;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) throws IllegalArgumentException {
        validateThreadNumber(numberOfThreads);

        this.numberOfThreads = numberOfThreads;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) throws IllegalArgumentException {
        validateDepth(depth);

        this.depth = depth;
    }

    public void setDepthUnlimited() {
        this.depth = -1;
    }

    public String getLinkRegex() {
        return linkRegex;
    }

    public void setLinkRegex(String linkRegex) {
        this.linkRegex = linkRegex;
    }

    public String getEmailRegex() {
        return emailRegex;
    }

    public void setEmailRegex(String emailRegex) {
        this.emailRegex = emailRegex;
    }

    public String getTelephoneNumberRegex() {
        return telephoneNumberRegex;
    }

    public void setTelephoneNumberRegex(String telephoneNumberRegex) {
        this.telephoneNumberRegex = telephoneNumberRegex;
    }


    /**
     * Validates that the url starts with a valid {@link Main#ALLOWED_PROTOCOLS}
     *
     * @param url url to validate
     * @throws IllegalArgumentException if url starts with not supported protocol
     */
    private void validateUrlProtocol(String url) throws IllegalArgumentException {

        for (String protocol : Main.ALLOWED_PROTOCOLS) {
            if (url.startsWith(protocol)) return;
        }

        throw new IllegalArgumentException("Only http and https protocol are supported");
    }


    /**
     * Validate that the given depths is greater than or equal to 1.
     *
     * @param depth depth to be validated
     * @throws IllegalArgumentException Is thrown if the depth is < 1.
     */
    private void validateDepth(int depth) throws IllegalArgumentException {
        if (depth >= 1) return;

        throw new IllegalArgumentException("Depth must be at least 1 or greater");
    }


    /**
     * Validate that the number of threads is greater than or equal to 1.
     *
     * @param threads Number of threads
     * @throws IllegalArgumentException Is thrown if number of threads is less than 1.
     */
    private void validateThreadNumber(int threads) throws IllegalArgumentException {
        if (threads >= 1) return;

        throw new IllegalArgumentException("Number of threads must be equal to 1 or greater");
    }


}
