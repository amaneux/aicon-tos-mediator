package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.web.pages.DataStore;

import java.util.ArrayList;
import java.util.List;

public class BaseController {
    private final static DataStore dataStore = DataStore.getInstance();

    BaseController() {
        throw new IllegalStateException("Utility class");
    }

    public static String splitReportLinesIfTooLong(String report) {
        if (report == null || report.isEmpty()) {
            return ""; // Prevent null pointer errors
        }

        List<String> sentences = List.of(report.split("\n"));
        List<String> reportSentences = new ArrayList<>();

        for (String sentence : sentences) {
            reportSentences.add(splitLineAndIndent(sentence));
        }

        return String.join("\n", reportSentences);
    }

    private static String splitLineAndIndent(String input) {

        int splitPoint = findSplitPointWithCharacters(input, 0, new char[]{',', ' '});

        if (splitPoint != -1) {
            int nrOfTabsAtBeginning = countLeadingTabs(input);
            String sameTabs = "\t".repeat(nrOfTabsAtBeginning);
            input = input.substring(0, splitPoint + 1) + "\n\t" + sameTabs + input.substring(splitPoint + 2);


            while ((splitPoint != -1) && (input.length() - splitPoint > dataStore.getViewportWidth())) {
                splitPoint = findSplitPointWithCharacters(input, splitPoint + 3, new char[]{',', ' '});
                if (splitPoint != -1) {
                    input = input.substring(0, splitPoint + 1) + "\n\t" + sameTabs + input.substring(splitPoint + 2);
                }
            }
        }
        return input;
    }

    private static int findSplitPointWithCharacters(String sentence, int from, char[] characters) {
        int splitPoint = -1;
        int to = from + Math.min((sentence.length() - from), dataStore.getViewportWidth());
        String tmpSentence = sentence.substring(from, to);

        if (sentenceTooLong(tmpSentence)) {
            for (char character : characters) {
                splitPoint = tmpSentence.lastIndexOf(character);
                if (splitPoint != -1) {
                    splitPoint += from;
                    break;
                }
            }
        }
        return splitPoint;
    }

    private static boolean sentenceTooLong(String part) {
        return part.length() >= dataStore.getViewportWidth();
    }

    private static int countLeadingTabs(String input) {
        int count = 0;
        while (count < input.length() && input.charAt(count) == '\t') {
            count++;
        }
        return count;
    }
}
