package com.sap.cap.esmapi.utilities.scrambling;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CL_ScramblingUtils
{

    private static final String ccPatternDash = "(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|"
            + "6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|"
            + "(?:2131|1800|35\\d{3})\\d{11}|"
            + "(?:4\\d{3}|5[1-5]\\d{2}|6011|7\\d{3})-?\\d{4}-?\\d{4}-?\\d{4}|3[4,7]\\d{13})";

    private static final String ccPatternComma = "(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|"
            + "6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|"
            + "(?:2131|1800|35\\d{3})\\d{11}|"
            + "(?:4\\d{3}|5[1-5]\\d{2}|6011|7\\d{3}),?\\d{4},?\\d{4},?\\d{4}|3[4,7]\\d{13})";

    private static final String ccPatternSpace = "(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|"
            + "6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|"
            + "(?:2131|1800|35\\d{3})\\d{11}|"
            + "(?:4\\d{3}|5[1-5]\\d{2}|6011|7\\d{3})\\s?\\d{4}\\s?\\d{4}\\s?\\d{4}|3[4,7]\\d{13})";

    private static final String ccPatternDot = "(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|"
            + "6(?:011|5[0-9][0-9])[0-9]{12}|3[47][0-9]{13}|3(?:0[0-5]|[68][0-9])[0-9]{11}|"
            + "(?:2131|1800|35\\d{3})\\d{11}|"
            + "(?:4\\d{3}|5[1-5]\\d{2}|6011|7\\d{3}).?\\d{4}.?\\d{4}.?\\d{4}|3[4,7]\\d{13})";

    private static final Pattern ccDetectorDash = Pattern.compile(ccPatternDash);
    private static final Pattern ccDetectorComma = Pattern.compile(ccPatternComma);
    private static final Pattern ccDetectorSpace = Pattern.compile(ccPatternSpace);
    private static final Pattern ccDetectorDot = Pattern.compile(ccPatternDot);

    private static final String CC_PLACEHOLDER = "XXXX XXXX XXXX XXXX";

    public static String scrambleText(String text)
    {
        String scrambledTxt = null;

        scrambledTxt = redactCCDash(text);
        scrambledTxt = redactCCComma(scrambledTxt);
        scrambledTxt = redactCCSpace(scrambledTxt);
        scrambledTxt = redactCCDot(scrambledTxt);

        log.info("Scrambled Text : " + scrambledTxt);

        return scrambledTxt;
    }

    public static String redactCCDash(String data)
    {
        Matcher m = ccDetectorDash.matcher(data);
        return m.replaceAll(CC_PLACEHOLDER);
    }

    public static String redactCCComma(String data)
    {
        Matcher m = ccDetectorComma.matcher(data);
        return m.replaceAll(CC_PLACEHOLDER);
    }

    public static String redactCCSpace(String data)
    {
        Matcher m = ccDetectorSpace.matcher(data);
        return m.replaceAll(CC_PLACEHOLDER);
    }

    public static String redactCCDot(String data)
    {
        Matcher m = ccDetectorDot.matcher(data);
        return m.replaceAll(CC_PLACEHOLDER);
    }
}
