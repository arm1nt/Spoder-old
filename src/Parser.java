import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses text segments and stores the found links, emails telephone numbers.
 */
public class Parser {

    private static final String DEFAULT_LINK_REGEX = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9äüöÄÜÖ@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()äüöÄÜÖ@:%_\\+.~#?&//=]*)";
    private static final String DEFAULT_HREF_LINK = "(^(?!www\\.|(?:http|ftp)s?://|[A-Za-z]:\\\\|//).*)|(https?:\\/\\/(www\\.)?[-a-zA-Z0-9äüöAÜÖ@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()äüöÄÜÖ@:%_\\+.~#?&//=]*))";
    private static final String DEFAULT_EMAIL_REGEX = "^[mailto:]?[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
    private static final String DEFAULT_PHONE_NUMBER_REGEX = "^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$";

    private final Pattern linkPattern;
    private final Pattern hrefLinkPattern;
    private final Pattern emailPattern;
    private final Pattern phoneNumberPattern;

    private final Set<Link> collectedLinks =  ConcurrentHashMap.newKeySet();
    private final Set<String> collectedEmails = ConcurrentHashMap.newKeySet();
    private final Set<String> collectedPhoneNumbers = ConcurrentHashMap.newKeySet();


    Parser(String linkRegex, String emailRegex, String phoneNumberRegex) {
        this.linkPattern = customOrDefaultPattern(linkRegex, DEFAULT_LINK_REGEX);
        this.hrefLinkPattern = customOrDefaultPattern(linkRegex, DEFAULT_HREF_LINK);
        this.emailPattern = customOrDefaultPattern(emailRegex, DEFAULT_EMAIL_REGEX);
        this.phoneNumberPattern = customOrDefaultPattern(phoneNumberRegex, DEFAULT_PHONE_NUMBER_REGEX);
    }


    /**
     * Parses the given line word by word and collects all links, emails, phone numbers that have not been found yet.
     * A set of the new-found links is returned.
     *
     * @param line line to be parsed
     * @return Set of the new-found links
     */
    public Set<Link> parseLine(String line, Link parentLink) {
        String[] words = line.split(" ");

        Set<Link> resultSet = new HashSet<>();

        for (String word : words) {

            Matcher matcher = linkPattern.matcher(word);
            if (matcher.find()) {
                String found = matcher.group();

                Link temp_link = generateAbsoluteLink(parentLink, found);

                if (!collectedLinks.contains(temp_link)) {
                    resultSet.add(temp_link);
                    collectedLinks.add(temp_link);
                }
                continue;
            }

            matcher = this.emailPattern.matcher(word);
            if (matcher.find()) {
                this.collectedEmails.add(matcher.group());
                continue;
            }

            matcher = this.phoneNumberPattern.matcher(word);
            if (matcher.find()) {
                this.collectedPhoneNumbers.add(matcher.group());
            }
        }

        return resultSet;
    }


    /**
     * Parses the content of a tag and searches for links in hrefs
     *
     * @param attributes text inside a tag
     * @return List of found links
     */
    public Set<Link> parseAttributes(String attributes, Link parentLink) {
        Set<Link> foundLinks = new HashSet<>();

        StringBuilder word = new StringBuilder();

        //Go through the attributes' word by word and as soon as we find a key that we are interested in
        //we parse its value (key='value' or key="value").
        for (int i = 0; i < attributes.length(); i++) {
            if (attributes.charAt(i) == ' ' || attributes.charAt(i) == '\n' || attributes.charAt(i) == '\t') {
                word.delete(0, word.length());
                continue;
            } else if (attributes.charAt(i) == '=') {
                boolean relevantKeyword = relevantKeyword(word.toString());

                word.delete(0, word.length());
                int valueCounter = i+1;
                char delim = attributes.charAt(valueCounter);
                boolean linkWithoutQuotes = false;
                if (delim != '\"' && delim != '\'') {
                    linkWithoutQuotes = true;
                    word.append(attributes.charAt(valueCounter));
                }

                String otherDelims = new String(new char[]{' ', '>'});
                valueCounter++;
                boolean endFound = false;

                while (valueCounter < attributes.length()) {

                    if (!linkWithoutQuotes && attributes.charAt(valueCounter) == delim) {
                        endFound = true;
                        break;
                    }
                    if (linkWithoutQuotes && otherDelims.contains(String.valueOf(attributes.charAt(valueCounter)))) {
                        endFound = true;
                        break;
                    }
                    word.append(attributes.charAt(valueCounter));
                    valueCounter++;
                }

                if (!endFound && !linkWithoutQuotes) break;

                i = valueCounter+1;

                if (relevantKeyword) {
                    Matcher matcher = hrefLinkPattern.matcher(word.toString());
                    if (matcher.find()) {
                        String found = matcher.group();

                        Link temp_link = generateAbsoluteLink(parentLink, found);

                        if (!collectedLinks.contains(temp_link)) {
                            foundLinks.add(temp_link);
                            collectedLinks.add(temp_link);
                        }

                        word.delete(0, word.length());
                        continue;
                    }


                    matcher = emailPattern.matcher(word.toString());
                    if (matcher.find()) {
                        this.collectedEmails.add(matcher.group());
                    }
                }

                word.delete(0, word.length());
                continue;
            }

            word.append(attributes.charAt(i));

        }
        return foundLinks;
    }


    /**
     * Returns a set of all links collected so far by all threads.
     *
     * @return Set of links
     */
    public Set<String> collectLinks() {
        Set<String> temp = new HashSet<>();
        for (Link link : collectedLinks) {
            temp.add(link.toString());
        }
        return temp;
    }


    /**
     * Returns a set of all emails collected so far by all threads.
     *
     * @return Set of emails
     */
    public Set<String> collectEmails() {
        return collectedEmails;
    }


    /**
     * Returns a set of all phone numbers collected so far by all threads.
     *
     * @return set of phone numbers
     */
    public Set<String> collectPhoneNumbers() {
        return collectedPhoneNumbers;
    }


    /**
     * Takes a custom regex and an according default regex. If the custom regex is null, the default pattern is returned
     * In every other case the custom pattern is returned.
     *
     * @param custom custom regular expression
     * @param standard default regular expression
     * @return Compiled regex
     */
    private Pattern customOrDefaultPattern(String custom, String standard) {
        if (custom == null) return Pattern.compile(standard);

        return Pattern.compile(custom);
    }

    /**
     * Check if the given keyword is a relevant attribute
     *
     * @param key key to be checked
     * @return true if it is a relevant attribute, false otherwise
     */
    private boolean relevantKeyword(String key) {
        String[] keywords = {"href"};

        for (String keyword : keywords) {
            if (keyword.equals(key)) return true;
        }

        return false;
    }

    /**
     * Creates a new absolute link
     *
     * @param parentLink the link on the page on which we found the link
     * @param found the found link
     * @return new Link Object that represent the absolute link
     */
    private Link generateAbsoluteLink(Link parentLink, String found) {
        Link toAdd;
        if (urlStartsWithProtocol(found)) {
            toAdd = new Link(null, found);
        } else {
            toAdd = new Link(parentLink.toString(), found);
        }
        return toAdd;
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
