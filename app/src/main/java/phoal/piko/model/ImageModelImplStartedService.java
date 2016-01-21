package phoal.piko.model;

import phoal.piko.model.services.DownloadImagesStartedService;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

/**
 * This class implements image downloading operations using an Android
 * Started Service.  It plays the role of the "Concrete Implementor"
 * in the Bridge pattern and.
 */
public class ImageModelImplStartedService 
       extends ModelImpl {
    /**
     * Start a download.  When the download finishes its results are
     * passed up to the Presentation layer via the
     * onDownloadComplete() method defined in RequiredPresenterOps.
     * This method plays the fole of the "Primitive Operation"
     * (a.k.a., "Abstract Hook Method") in the Template Method
     * pattern.
     *
     * @param url
     *        URL of the image to download.
     * @param context ApplicationContext
     *        Uri of the directory to store the downloaded image.
     */
    public void startProcess(Context context, Uri url, String fileId) {
        // Create an intent to download the image via the
        // DownloadImagesStartedService.
        Intent intent =
            DownloadImagesStartedService.makeIntent
                (mPresenter.get().getActivityContext(),
                 OperationType.DOWNLOAD_IMAGE.ordinal(),
                 url,
                 ServiceModel.DIR_PATH,
                 fileId,
                 this);
        Log.d(TAG,
              "starting the DownloadImageStartedService for "
              + url.toString());

        // Start the service.
        mPresenter.get().getActivityContext().startService(intent);
    }
}
