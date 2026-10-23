package com.hippo.ehviewer.client.data;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Groups gallery list entries that are likely to contain the same work.
 *
 * This is deliberately conservative: matching titles and approximately matching
 * page counts are required. It never changes, deletes or blocks gallery records.
 */
public final class GalleryWorkDeduplicator {

    public static final String SETTING_KEY = "gallery_preferred_work_language";
    public static final String DEFAULT_LANGUAGE = "ZH";
    public static final String SHOW_ALL = "ALL";
    public static final String AUTO = "AUTO";

    // Includes SHOW_ALL and AUTO for the browse dialog.
    public static final String[] LANGUAGE_CHOICES = {
            SHOW_ALL, AUTO, "ZH", "JA", "EN", "KO", "ES", "RU", "FR",
            "PT", "TH", "DE", "IT", "VI", "PL", "HU", "NL"
    };

    private static final Pattern BRACKETED = Pattern.compile("\\[[^\\]]{1,80}\\]|\\([^)]{1,80}\\)");
    private static final Pattern NON_WORD = Pattern.compile("[\\p{P}\\p{S}\\s]+");
    private static final Pattern JAPANESE_KANA = Pattern.compile("[\\u3040-\\u30ff]");
    private static final Pattern NOTE_LANGUAGE = Pattern.compile(
            "(?i)(?:^|\\W)(?:chinese|english|japanese|korean|spanish|russian|french|"
                    + "portuguese|thai|german|italian|vietnamese|polish|hungarian|dutch|"
                    + "中文|中国語|中國語|日本語|英文|英訳|英語|韓国語|汉化|漢化|翻译|翻譯|翻訳|"
                    + "translated|translation|scanlation)(?:$|\\W)");
    private static final Pattern TRANSLATOR_GROUP = Pattern.compile(
            ".*(?:汉化|漢化|翻译|翻譯|翻訳|scanlation|translated|translation).*",
            Pattern.CASE_INSENSITIVE);

    private GalleryWorkDeduplicator() { }

    public static boolean isEnabled(String language) {
        return language != null && !SHOW_ALL.equals(language);
    }

    /**
     * Prefers a requested translation, falling back to the earliest listed
     * edition if the translation is unavailable.
     */
    public static List<GalleryInfo> reducePage(List<GalleryInfo> source, String language) {
        List<GalleryInfo> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        if (!isEnabled(language)) {
            result.addAll(source);
            return result;
        }
        for (GalleryInfo candidate : source) {
            if (candidate == null) {
                continue;
            }
            int existingPosition = -1;
            for (int i = 0; i < result.size(); i++) {
                if (isSameWork(result.get(i), candidate)) {
                    existingPosition = i;
                    break;
                }
            }
            if (existingPosition == -1) {
                result.add(candidate);
            } else if (prefer(candidate, result.get(existingPosition), language)) {
                result.set(existingPosition, candidate);
            }
        }
        return result;
    }

    public static boolean prefer(GalleryInfo candidate, GalleryInfo current, String language) {
        return score(candidate, language) > score(current, language);
    }

    private static int score(GalleryInfo info, String language) {
        if (info == null || AUTO.equals(language) || SHOW_ALL.equals(language)) {
            return 0;
        }
        String actualLanguage = info.simpleLanguage;
        if (actualLanguage == null && info.simpleTags != null) {
            for (String tag : info.simpleTags) {
                if (tag != null && tag.equalsIgnoreCase("language:japanese")) {
                    actualLanguage = "JA";
                    break;
                }
            }
        }
        // Some original Japanese works carry no language tag at all.
        // Prefer them only when the title contains kana; otherwise keep them as fallback.
        if ("JA".equalsIgnoreCase(language) && actualLanguage == null
                && ((info.title != null && JAPANESE_KANA.matcher(info.title).find())
                || (info.titleJpn != null && JAPANESE_KANA.matcher(info.titleJpn).find()))) {
            return 1;
        }
        return language.equalsIgnoreCase(actualLanguage) ? 1 : 0;
    }

    public static boolean isSameWork(GalleryInfo a, GalleryInfo b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.gid != 0 && a.gid == b.gid) {
            return true;
        }
        // A manga and an image set with the same generic title are not reliably the same work.
        if (a.category != b.category) {
            return false;
        }
        if (a.pages > 0 && b.pages > 0) {
            int tolerance = Math.max(4, Math.max(a.pages, b.pages) / 5);
            if (Math.abs(a.pages - b.pages) > tolerance) {
                return false;
            }
        }
        String aTitle = normalize(a.title);
        String aOriginal = normalize(a.titleJpn);
        String bTitle = normalize(b.title);
        String bOriginal = normalize(b.titleJpn);
        return equalNonempty(aTitle, bTitle)
                || equalNonempty(aTitle, bOriginal)
                || equalNonempty(aOriginal, bTitle)
                || equalNonempty(aOriginal, bOriginal);
    }

    private static boolean equalNonempty(String first, String second) {
        // Avoid merging unrelated galleries with extremely generic short titles.
        return first.length() >= 5 && first.equals(second);
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String text = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .replace('【', '[').replace('】', ']')
                .replace('（', '(').replace('）', ')');
        Matcher matcher = BRACKETED.matcher(text);
        StringBuffer stripped = new StringBuffer();
        while (matcher.find()) {
            String note = matcher.group();
            String contents = note.substring(1, note.length() - 1).trim();
            boolean translatorGroup = TRANSLATOR_GROUP.matcher(contents).matches();
            boolean explicitLanguage = NOTE_LANGUAGE.matcher(" " + contents + " ").find();
            matcher.appendReplacement(stripped, (translatorGroup || explicitLanguage) ? " " : Matcher.quoteReplacement(note));
        }
        matcher.appendTail(stripped);
        return NON_WORD.matcher(stripped.toString().toLowerCase(Locale.ROOT))
                .replaceAll("").trim();
    }
}
