import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses text segments and stores the found links, emails telephone numbers.
 */
public class Parser {

    private static final String DEFAULT_LINK_REGEX = "https?:\\/\\/(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_\\+.~#?&//=]*)";
    private static final String DEFAULT_EMAIL_REGEX = "^[mailto:]?[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$";
    private static final String DEFAULT_PHONE_NUMBER_REGEX = "^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$";

    private final Pattern  linkPattern;
    private final Pattern emailPattern;
    private final Pattern phoneNumberPattern;

    //Idea: Instead of storing all found links in memory, we could use a database
    private ConcurrentHashMap<String, String> baseMap = new ConcurrentHashMap<>();
    private final Set<String> collectedLinks = baseMap.keySet("DEFAULT");
    private final Set<String> collectedEmails = baseMap.keySet("DEFAULT");
    private final Set<String> collectedPhoneNumbers = baseMap.keySet("DEFAULT");


    Parser(String linkRegex, String emailRegex, String phoneNumberRegex) {
        this.linkPattern = customOrDefaultPattern(linkRegex, DEFAULT_LINK_REGEX);
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
    public Set<String> parseLine(String line) { //TODO: pass in parent link
        String[] words = line.split(" ");

        Set<String> resultSet = new HashSet<>();

        for (String word : words) {

            Matcher matcher = linkPattern.matcher(word);
            if (matcher.find()) {
                String found = matcher.group();
                if (!collectedLinks.contains(found)) {
                    resultSet.add(found);
                    collectedLinks.add(found);
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


    //TODO: maybe go through attributes myself -> easier to search for other tags as well
    /**
     * Parses the content of a tag and searches for links in hrefs
     *
     * @param attributes text inside a tag
     * @return List of found
     */
    public Set<String> parseAttributes(String attributes) {
        Set<String> foundLinks = new HashSet<>();

        int indexOfFirstAttribute = attributes.indexOf("href");

        if (indexOfFirstAttribute == -1) return foundLinks;

        char quoteChar = attributes.charAt(indexOfFirstAttribute+5);

        attributes = attributes.substring(indexOfFirstAttribute+6);

        String link = attributes.substring(0, attributes.indexOf(quoteChar));

        if (!collectedLinks.contains(link)) {
            foundLinks.add(link);
            collectedLinks.add(link);
        }

        return foundLinks;
    }


    /**
     * Returns a set of all links collected so far by all threads.
     *
     * @return Set of links
     */
    public Set<String> collectLinks() {
        return collectedLinks;
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
}
