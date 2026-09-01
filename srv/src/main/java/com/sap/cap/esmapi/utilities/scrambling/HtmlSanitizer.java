package com.sap.cap.esmapi.utilities.scrambling;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

public final class HtmlSanitizer
{

    //@formatter:off
    // Allowed HTML tags for case descriptions
    private static final Safelist CASE_HTML_SAFELIST =
        Safelist.none()
                .addTags(
                        "p",
                        "br",
                        "strong",
                        "b",
                        "em",
                        "i",
                        "u",
                        "ul",
                        "ol",
                        "li"
                );
    //@formatter:on            

    private HtmlSanitizer()
    {
        // Utility class
    }

    public static String sanitizeCaseHtml(String html)
    {
        if (html == null || html.isBlank())
        {
            return html;
        }

        return Jsoup.clean(html, CASE_HTML_SAFELIST);
    }

}