package phoal.piko.tests;

/**
 * Based on tests created by Monte Creasor on 2015-05-26.
 * Modified by phoal on 10/1/16.
 */

/**
 * Configuration Change Test (2 screen rotations):
 *
 * ACTIONS: Same as [Multiple Valid URLs Test] but with 2 screen
 * rotations performed immediately after invoking the download image command.
 * EXPECTED RESULTS: same as for [Multiple valid URLs Test] above.
 */

public class Test7_MultipleValidUrlsWithScreenRotations extends DownloadImagesActivityBaseTest {
    public void testRun() {
        TestUrlsHelper.doTest(mSolo,
                              mValidUrlList,
                              mValidUrlList.length,
                              0,
                              true);
    }
}
