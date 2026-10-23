package com.hippo.ehviewer.client.data;

import org.junit.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

public class GalleryWorkDeduplicatorTest {
    private GalleryInfo gallery(long gid, String title, String language, int pages) {
        GalleryInfo info = new GalleryInfo();
        info.gid = gid;
        info.title = title;
        info.simpleLanguage = language;
        info.pages = pages;
        info.category = 1;
        return info;
    }

    @Test
    public void picksChineseEvenWhenItAppearsAfterAnotherTranslation() {
        GalleryInfo english = gallery(10, "[Artist] Story Name [English]", "EN", 24);
        GalleryInfo chinese = gallery(11, "[Artist] Story Name [Chinese]", "ZH", 26);
        GalleryInfo japanese = gallery(12, "[Artist] Story Name", null, 23);
        List<GalleryInfo> result = GalleryWorkDeduplicator.reducePage(
                Arrays.asList(english, japanese, chinese), "ZH");
        assertEquals(1, result.size());
        assertEquals(11, result.get(0).gid);
    }

    @Test
    public void keepsOneFallbackIfPreferredTranslationDoesNotExist() {
        GalleryInfo japanese = gallery(20, "[Artist] Another Story", "JA", 22);
        GalleryInfo english = gallery(21, "[Artist] Another Story [English]", "EN", 23);
        List<GalleryInfo> result = GalleryWorkDeduplicator.reducePage(
                Arrays.asList(japanese, english), "ZH");
        assertEquals(1, result.size());
        assertEquals(20, result.get(0).gid);
    }

    @Test
    public void showsAllWhenGroupingDisabled() {
        GalleryInfo a = gallery(30, "Shared Title [English]", "EN", 20);
        GalleryInfo b = gallery(31, "Shared Title [Chinese]", "ZH", 20);
        assertEquals(2, GalleryWorkDeduplicator.reducePage(
                Arrays.asList(a, b), GalleryWorkDeduplicator.SHOW_ALL).size());
    }

    @Test
    public void doesNotMergeDifferentChaptersOrCategories() {
        GalleryInfo first = gallery(40, "[Artist] Work chapter 1 [Chinese]", "ZH", 25);
        GalleryInfo second = gallery(41, "[Artist] Work chapter 2 [English]", "EN", 25);
        GalleryInfo otherCategory = gallery(42, "[Artist] Work chapter 1 [English]", "EN", 25);
        otherCategory.category = 2;
        assertEquals(3, GalleryWorkDeduplicator.reducePage(
                Arrays.asList(first, second, otherCategory), "ZH").size());
    }

    @Test
    public void doesNotMergeDramaticallyDifferentPageCounts() {
        GalleryInfo shortWork = gallery(50, "Work With Enough Title", "EN", 12);
        GalleryInfo longWork = gallery(51, "Work With Enough Title [Chinese]", "ZH", 90);
        assertFalse(GalleryWorkDeduplicator.isSameWork(shortWork, longWork));
    }

    @Test
    public void findsSameWorkAcrossSeparatePages() {
        GalleryInfo earlier = gallery(60, "[Circle (Artist)] Sample Work [English]", "EN", 18);
        GalleryInfo later = gallery(61, "[Circle (Artist)] Sample Work [Chinese] [某某汉化组]", "ZH", 20);
        assertTrue(GalleryWorkDeduplicator.isSameWork(earlier, later));
        assertTrue(GalleryWorkDeduplicator.prefer(later, earlier, "ZH"));
    }
    @Test
    public void recognizesAlternateBilingualTitles() {
        GalleryInfo first = gallery(100, "[Artist] Midnight Rain | 夜半の雨物語 [English]", "EN", 22);
        GalleryInfo second = gallery(101, "夜半の雨物語 [Chinese]", "ZH", 23);
        assertTrue(GalleryWorkDeduplicator.isSameWork(first, second));
    }

    @Test
    public void sameCoverAndCreatorCanMatchEntirelyDifferentTranslatedTitles() {
        GalleryInfo original = gallery(200, "夜更けの物語", "JA", 24);
        GalleryInfo translated = gallery(201, "A Tale of the Night [English]", "EN", 25);
        original.simpleTags = new String[]{"artist:sample creator", "language:japanese"};
        translated.simpleTags = new String[]{"artist:sample creator", "language:english"};
        original.thumb = "https://ehgt.org/t/abc123def456/89abcdef7654321_100.jpg";
        translated.thumb = "https://another.ehgt.org/t/abc123def456/89abcdef7654321_250.jpg";
        assertTrue(GalleryWorkDeduplicator.isSameWork(original, translated));
    }

    @Test
    public void doNotGroupSameCreatorWhenCoverDiffers() {
        GalleryInfo first = gallery(300, "First Unrelated Work", "EN", 20);
        GalleryInfo second = gallery(301, "Second Unrelated Work", "ZH", 20);
        first.simpleTags = new String[]{"artist:sample creator"};
        second.simpleTags = new String[]{"artist:sample creator"};
        first.thumb = "https://ehgt.org/t/abc123def456/89abcdef7654321_100.jpg";
        second.thumb = "https://ehgt.org/t/abc123def456/001122334455667_100.jpg";
        assertFalse(GalleryWorkDeduplicator.isSameWork(first, second));
    }

    @Test
    public void doNotGroupDifferentTitlesUsingSharedPlaceholder() {
        GalleryInfo first = gallery(400, "First Unrelated Work", "EN", 20);
        GalleryInfo second = gallery(401, "Second Unrelated Work", "ZH", 20);
        first.simpleTags = new String[]{"artist:sample creator"};
        second.simpleTags = new String[]{"artist:sample creator"};
        first.thumb = "https://ehgt.org/t/placeholder/default_gallery_cover.png";
        second.thumb = first.thumb;
        assertFalse(GalleryWorkDeduplicator.isSameWork(first, second));
    }

}
