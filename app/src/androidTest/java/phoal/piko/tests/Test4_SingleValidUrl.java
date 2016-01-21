package phoal.piko.tests;

/**
 * Based on tests created by Monte Creasor on 2015-05-26.
 * Modified by phoal on 10/1/16.
 */

/**
 * Single Valid URL Test:
 *
 * ACTIONS: Add, download, display, and deletion of a single valid image URL.
 * EXPECTED RESULTS: A single entry in the URL list and a single displayed
 * image in the DisplayImageActivity grid view.
 */

public class Test4_SingleValidUrl extends DownloadImagesActivityBaseTest {

    public void testRun() {
        TestUrlsHelper.doTest(mSolo, mValidUrlList, 1, 0, false);
    }
}
