package com.sap.cap.esmapi.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.util.StringUtils;

import com.sap.cap.esmapi.exceptions.EX_ESMAPI;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringsUtility
{
    public static String replaceURLwithParams(String baseString, String[] replStrings, String separator)
    {
        if (!StringUtils.hasText(baseString))
        {
            return null;
        }

        int countOccurances = org.apache.commons.lang3.StringUtils.countMatches(baseString, separator);
        int countREpl = replStrings.length;

        log.info("Ocurences to Replace : {}", countOccurances);
        log.info("Replacement Values : {}", countREpl);

        if (countOccurances > countREpl)
        {
            if (countREpl == 1)
            {
                return baseString.replace(separator, replStrings[0]);
            }

            throw new EX_ESMAPI("Occurences to Replace - " + countOccurances + " in String - " + baseString
                    + " are more than replacement values : " + countREpl);
        }

        if (countOccurances < countREpl)
        {
            throw new EX_ESMAPI("Occurences to Replace - " + countOccurances + " in String - " + baseString
                    + " are less than replacement values : " + countREpl);
        }

        String[] allparts = baseString.split(Pattern.quote(separator), -1);

        StringBuilder parsedUrl = new StringBuilder();

        for (int i = 0; i < replStrings.length; i++)
        {
            parsedUrl.append(allparts[i]);
            parsedUrl.append(replStrings[i]);
        }

        parsedUrl.append(allparts[allparts.length - 1]);

        return parsedUrl.toString();
    }

    public static void reverseArray(String[] arr)
    {
        int left = 0;
        int right = arr.length - 1;

        while (left < right)
        {
            String temp = arr[left];
            arr[left] = arr[right];
            arr[right] = temp;

            left++;
            right--;
        }
    }

    public static String[] cleanReverseAndPad(String[] input)
    {
        if (input == null)
        {
            return null;
        }

        int size = input.length;

        // 1️⃣ Collect non-null values
        List<String> nonNullValues = new ArrayList<>();
        for (String val : input)
        {
            if (val != null && !val.trim().isEmpty())
            {
                nonNullValues.add(val);
            }
        }

        // 2️⃣ Reverse valid values
        Collections.reverse(nonNullValues);

        // 3️⃣ Create result array of original size
        String[] result = new String[size];

        // 4️⃣ Copy reversed values to top
        for (int i = 0; i < nonNullValues.size(); i++)
        {
            result[i] = nonNullValues.get(i);
        }

        // Remaining positions automatically stay null

        return result;
    }

}
