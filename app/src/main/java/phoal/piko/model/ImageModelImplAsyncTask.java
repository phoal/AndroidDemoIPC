package phoal.piko.model;

import java.lang.ref.WeakReference;

import phoal.piko.MVP;
import phoal.piko.common.BitmapUtils;
import phoal.piko.common.GenericAsyncTask;
import phoal.piko.common.GenericAsyncTaskOps;
import phoal.piko.common.Utils;
import phoal.piko.model.datamodel.ReplyMessage;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

/**
 * This class plays the "Model" role in the Model-View-Presenter (MVP)
 * pattern by defining an interface for providing data that will be
 * acted upon by the "Presenter" and "View" layers in the MVP pattern.
 * It implements the MVP.ProvidedModelOps so it can be created/managed
 * by the GenericPresenter framework.
 */

/**
 * Created by phoal on 5/12/2015.
 */
public class ImageModelImplAsyncTask extends ModelImpl implements
            GenericAsyncTaskOps<ImageModelImplAsyncTask.Task, Void, Runnable>  {
    public static final Boolean KEEP_ORIGINAL = false;
    /**
     * Debugging tag used by the Android logger.
     */
    protected final static String TAG =
            ImageModelImplAsyncTask.class.getSimpleName();

    /**
     * A WeakReference used to access methods in the Presenter layer.
     * The WeakReference enables garbage collection.
     */
    private WeakReference<MVP.RequiredPresenterOps> mPresenter;

    /**
     * Hook method called when a new instance of AcronymModel is
     * created.  One time initialization code goes here, e.g., storing
     * a WeakReference to the Presenter and initializing the sync and
     * async Services.
     *
     * @param presenter
     *            A reference to the Presenter layer.
     */
    @Override
    public void onCreate(MVP.RequiredPresenterOps presenter) {
        // Safe to set reference - both presenter & model are retained singleton instances.
        mPresenter = new WeakReference<MVP.RequiredPresenterOps>(presenter);

    }

    /**
     * Hook method called to shutdown the Model layer.
     *
     * @param isChangingConfigurations
     *        True if a runtime configuration triggered the onDestroy() call.
     */
    @Override
    public void onDestroy(boolean isChangingConfigurations) {
        // No-op.
    }


    @Override
    public void startProcess(Context context, Uri url, String fileId) {
        /**
         *  Uses GenericAsyncTask framework to download each url in a separate AsyncTask.
         *  When that completes it creates another AsyncTask to filter the image.
         *  When the filter completes it calls back to the presenter.
         *  All AsyncTasks MUST run on AsyncTask.THREAD_POOL_EXECUTOR.
         */
        Task firstTask = new Task(false, url, url, ServiceModel.DIR_PATH, context, fileId);
        new GenericAsyncTask<Task, Void, Runnable, ImageModelImplAsyncTask>(this).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR, firstTask);

    }

    /**
     *  Required by GenericAsyncTask framework.
     *  For convenience it wraps result in a Runnable for the UI Thread
     *
     * @param tasks - A task contains necessary parameters to perform I/O operations. The endChain
     *              flag indicates which task needs to be actioned.
     *
     * @return - Runnable.
     */
    public Runnable doInBackground(Task... tasks) {

        /**
         *  Performs a blocking I/O task in a background thread then wraps result in a Runnable for
         *  UI Thread to process.
         *
         *  If endChain is set to false it will download the url image and then
         *  create another AsyncTask (with endChain set to true) for filtering .
         *
         *  With endChain set to true it will apply a grayscale filter then call back to presenter.
         */
        if (tasks == null || tasks.length == 0) return null;
        final Task task = tasks[0];

        if (!task.endChain) {
            // Perform blocking download task in background thread.
            final Uri image = downloadImage(task.url, task.directoryPathname, task.fileId);
            // Prepare the next stage.
            final Task grayTask = new Task(true, task.url, image, task.directoryPathname, task.context, task.fileId + "g");

            // Wrap result for UI Thread in a Runnable which creates another AsyncTask and calls executeOnExecutor().
            return new Runnable() {
                @Override
                public void run() {
                    if (grayTask.imageLocation != null) {
                        new GenericAsyncTask<Task, Void, Runnable, ImageModelImplAsyncTask>
                                (ImageModelImplAsyncTask.this).executeOnExecutor(
                                AsyncTask.THREAD_POOL_EXECUTOR, grayTask);
                        Log.i(TAG, "DONE");
                    }else { // image is null - save time & return now
                        final ReplyMessage reply = ReplyMessage.makeReplyMessage(null, task.url,
                                OperationType.DOWNLOAD_IMAGE.ordinal());
                        mPresenter.get().onProcessingComplete(reply);
                    }
                }
            };
        } else {
            // Perform grayFilter in background
            final Uri grayImage = grayFilter(task.context, task.imageLocation,
                    task.directoryPathname, KEEP_ORIGINAL, task.fileId);
            final ReplyMessage reply = ReplyMessage.makeReplyMessage(grayImage, task.url,
                    OperationType.DOWNLOAD_IMAGE.ordinal());
            // Wraps result for UI Thread - A Runnable which calls back the presenter with the image details.
            return new Runnable() {
                @Override
                public void run() {
                    mPresenter.get().onProcessingComplete(reply);
                }
            };
        }
    }

    public void onPostExecute(Runnable r) {
        if (r != null) post(r);
    }

    /**
     * Download the image located at the provided Internet url using
     * the URL class, store it on the android file system using a
     * FileOutputStream, and return the path to the image file on
     * disk.
     *
     * @param url
     *          The URL of the image to download.
     * @param fileId
     *          Pathname of the file to write the image to.
     *
     * @return
     *        Absolute path to the downloaded image file on the file
     *        system.
     */
    public Uri downloadImage(Uri url, Uri directoryPath, String fileId) {

        // Method which delegates to model how to download and store image file.
        // In this case delegate to DownloadUtils method.

        return Utils.downloadFile(url, directoryPath, fileId);

    }

    // Method which delegates to model how to copy image, apply a filter and whether
    // to delete original. In this case it delegates to DownloadUtils method.
    public Uri grayFilter(Context context,
                          Uri pathToImageFile,
                          Uri directoryPathname,
                          Boolean keepOriginal,
                          String fileId){
        return BitmapUtils.grayScaleFilter(context, pathToImageFile, directoryPathname,
                keepOriginal, fileId);
    }

    public static class Task {
        // make immutable values public;
        public final Boolean endChain;
        public final Uri url;
        public final Uri directoryPathname;
        public final Uri imageLocation;
        public final String fileId;
        public final Context context;

        public Task(Boolean endChain, Uri url, Uri imageLocation, Uri directoryPathname, Context context, String fileId){
            this.endChain = endChain;
            this.url = url;
            this.directoryPathname = directoryPathname;
            this.imageLocation = imageLocation;
            this.context = context;
            this.fileId = fileId;
        }

    }
}
