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

    private final Set<String> collectedLinks =  ConcurrentHashMap.newKeySet();
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


    /**
     * Parses the content of a tag and searches for links in hrefs
     *
     * @param attributes text inside a tag
     * @return List of found links
     */
    public Set<String> parseAttributes(String attributes) {
        Set<String> foundLinks = new HashSet<>();

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
                valueCounter++;
                boolean endFound = false;

                while (valueCounter < attributes.length()) {
                    if (attributes.charAt(valueCounter) == delim) {
                        endFound = true;
                        break;
                    }
                    word.append(attributes.charAt(valueCounter));
                    valueCounter++;
                }

                if (!endFound) break;

                i = valueCounter+1;

                if (relevantKeyword) {
                    Matcher matcher = hrefLinkPattern.matcher(word.toString());
                    if (matcher.find()) {
                        String found = matcher.group();
                        if (!collectedLinks.contains(found)) {
                            foundLinks.add(found);
                            collectedLinks.add(found);
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
}
