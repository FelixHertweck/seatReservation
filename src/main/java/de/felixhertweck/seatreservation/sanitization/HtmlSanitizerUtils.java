package de.felixhertweck.seatreservation.sanitization;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizerUtils {

    private static final PolicyFactory POLICY_FACTORY = new HtmlPolicyBuilder().toFactory();

    private HtmlSanitizerUtils() {
        // Private constructor to prevent instantiation
    }

    public static String sanitize(String unsafeHtml) {
        if (unsafeHtml == null) {
            return null;
        }
        return POLICY_FACTORY.sanitize(unsafeHtml);
    }
}
