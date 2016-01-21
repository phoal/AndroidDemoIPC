package phoal.piko.presenter;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import phoal.piko.MVP;
import phoal.piko.common.GenericPresenter;
import phoal.piko.common.Utils;
import phoal.piko.model.ServiceModel;
import phoal.piko.model.aidl.WeatherData;
import phoal.piko.model.datamodel.ReplyMessage;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

/**
 * This class defines all the image-related operations.  It implements
 * the various Ops interfaces so it can be created/managed by the
 * GenericActivity framework.  It plays the role of the "Abstraction"
 * in Bridge pattern and the role of the "Presenter" in the
 * Model-View-Presenter pattern.
 */
public class DataPresenter
       extends GenericPresenter<MVP.RequiredPresenterOps,
                                MVP.ProvidedModelOps,
        ServiceModel>
       implements MVP.ProvidedPresenterOps, MVP.RequiredPresenterOps {

    /**
     * The App is currently set up to not allow concurrent requests (for the aidl services).
     * This flag will prevent both services running at once ( the view can only handle one result at
     * a time.
     */
    private Boolean allowConcurrent = false;
    // This disables any further processing when set to true
    private Boolean isProcessing = false;
    /**
     * Used to enable garbage collection.
     */
    private WeakReference<MVP.RequiredViewOps> mView;

    /**
     * Stores the running total number of images downloaded that must
     * be handled by ServiceResultHandler.
     */
    private int mNumImagesToHandle;

    /**
     * Stores the running total number of images that have been
     * handled by the ServiceResultHandler.
     */
    private int mNumImagesHandled;

    /**
     * Constructor will choose either the Started Service or Bound
     * Service implementation of DataPresenter.
     */
    public DataPresenter() {
    }

    /**
     * Hook method called when a new instance of AcronymPresenter is
     * created.  One time initialization code goes here, e.g., storing
     * a WeakReference to the View layer and initializing the Model
     * layer.
     *
     * @param view A reference to the View layer.
     */
    @Override
    public void onCreate(MVP.RequiredViewOps view) {
        // Set the WeakReference.
        mView = new WeakReference<>(view);

        // Invoke the special onCreate() method in GenericPresenter,
        // passing in the ImageDownloadsModel class to instantiate/manage and
        // "this" to provide ImageDownloadsModel with this MVP.RequiredModelOps
        // instance.
        super.onCreate(ServiceModel.class,
                this);

        // Finish the initialization steps.
        resetFields();
    }

    /**
     * Hook method dispatched by the GenericActivity framework to
     * initialize the DataPresenter object after a runtime
     * configuration change.
     *
     * @param view The currently active DataPresenter.View.
     */
    @Override
    public void onConfigurationChange(MVP.RequiredViewOps view) {
        // Reset the mView WeakReference.
        mView = new WeakReference<>(view);

        // If the content is non-null then we're done, so set the
        // result of the Activity and finish it.
        if (allDownloadsComplete()) {
            // Hide the progress bar.
            mView.get().dismissProgressBar();
            Log.d(TAG,
                    "All images have finished downloading");
        } else if (downloadsInProgress()) {
            // Display the progress bar.
            mView.get().displayProgressBar();

            Log.d(TAG,
                    "Not all images have finished downloading");
        }

        // (Re)display the URLs.
        mView.get().displayUrls();
    }

    /**
     * Hook method called to shutdown the Presenter layer.
     *
     * @param isChangingConfigurations True if a runtime configuration triggered the onDestroy() call.
     */
    @Override
    public void onDestroy(boolean isChangingConfigurations) {
        // Destroy the model.
        getModel().onDestroy(isChangingConfigurations);
    }

    /**
     * Reset the URL and counter fields and redisplay linear layout.
     */
    private void resetFields() {
        // Reset the number of images to handle and which have been
        // handled.
        mNumImagesHandled = 0;
        mNumImagesToHandle = 0;

        // Clear the URL list.
        getUrlList().clear();

        // Redisplay the URLs, which should now be empty.
        if (mView.get() != null) mView.get().displayUrls();
    }

    /**
     * Start all the downloads and processing.  Plays the role of the
     * "Template Method" in the Template Method pattern.
     */
    @Override
    public void startProcessing(Context applicationContext) {
        // Don't block if it's an aidl call and concurrent calls are allowed.
        Boolean block = !(getModel().getAidlMode() && allowConcurrent);
        if (isProcessing && block) Utils.showToast(mView.get().getActivityContext(), "Processing... Pls Wait.");
        else if (getModel().getAidlMode()) processWeather();
        else processImage(applicationContext);
    }
    private void setDownload() {
        // Make the progress bar visible.
        mView.get().displayProgressBar();
        mView.get().disableButtons(isProcessing);
    }
    private void processWeather() {
        getModel().startProcessing(null);
        mView.get().displayProgressBar();
        isProcessing = true;
    }
    private void processImage(Context applicationContext) {
        String msg = "";
        ArrayList<Uri> urlList = getUrlList();
        /**
         * This App only handles processing one list of Urls at a time - an accidental 2nd click
         * must be prevented.
         * Also list should not be added to while processing.
         * Therefor check that processing has not already started & disable buttons in view.
         */
        //Check it's not already processing or list is empty.
        if (urlList.isEmpty() || isProcessing) {
            if (urlList.isEmpty()) msg = "No images provided";
            if (isProcessing) msg = "Processing... Pls Wait.";
            Utils.showToast(mView.get().getActivityContext(), msg);
        } else {
            // Disable any further method calls
            isProcessing = true;
            setDownload();
            // Keep track of number of images to download that must be
            // displayed.
            mNumImagesToHandle = urlList.size();
            /**
             * delegate to the bridge method in the Model layer
             */
            getModel().startProcessing(applicationContext);
        }
    }

    @Override
    public void onProcessingComplete(ReplyMessage reply) {
        Uri pathToImageFile = reply.getImageUri();
        Uri url = reply.getImageURL();
        // Increment the number of images handled regardless of
        // whether this result succeeded or failed to download and
        // image.
        ++mNumImagesHandled;

        if (pathToImageFile == null || reply.getResultCode() == Activity.RESULT_CANCELED)
            // Handle a failed download.
            mView.get().reportDownloadFailure(url, allDownloadsComplete());
        else /* replyMessage.getResultCode() == Activity.RESULT_OK) */
            // Handle a successful download.
            Log.d(TAG,
                    "received image at URI "
                            + pathToImageFile.toString());

        // Try to display all images received successfully.
        tryToDisplayImages();
    }
    @Override
    public void displayResults(WeatherData weatherData, String errorMessage) {
        mView.get().displayResults(weatherData, errorMessage);
        isProcessing = false;
        mView.get().dismissProgressBar();
    }

    /**
     * Launch an Activity to display all the images that were received
     * successfully if all downloads are complete.
     */
    private void tryToDisplayImages() {
        // This is in a method chain called on UI Thread so this should not happen.
        if (mView.get() == null) return;
        // If this is last image handled, display images via
        // DisplayImagesActivity.
        if (allDownloadsComplete()) {
            // Dismiss the progress bar.
            isProcessing = false;
            mView.get().dismissProgressBar();
            mView.get().disableButtons(isProcessing);

            // Initialize state for the next run.
            resetFields();

            // Only start the DisplayImageActivity if the image folder
            // exists and also contains at least 1 image to display.
            // Note that if the directory is empty, File.listFiles()
            // returns null.
            File file = new File(ServiceModel.DIR_PATH.toString());
            if (file.isDirectory())
                // Display the results.
                mView.get().displayResults(ServiceModel.DIR_PATH);
        }
    }

    /**
     * Returns true if all the downloads have completed, else false.
     */
    private boolean allDownloadsComplete() {
        return mNumImagesHandled == mNumImagesToHandle
                && mNumImagesHandled > 0;
    }

    /**
     * Returns true if there are any downloads in progress, else false.
     */
    private boolean downloadsInProgress() {
        return mNumImagesToHandle > 0;
    }

    /**
     * Set the type of Service to use for the ServiceModel implementation.
     *
     * @param serviceType Type of Service, i.e., STARTED_SERVICE
     */
    @Override
    public void setServiceType(int serviceType) {
        getModel().setServiceType(serviceType);
    }
    @Override
    public ArrayList<Uri> getUrlList() {return getModel().getUrlList();}

    /**
     * Delete all the downloaded images.
     */
    @Override
    public void deleteDownloadedImages() {
        // Delete all the downloaded images.
        int fileCount = deleteFiles(ServiceModel.DIR_PATH, 0);
        // Paranoid Check
        if (mView.get() != null) {
            // Indicate how many files were deleted.
            Utils.showToast(mView.get().getActivityContext(),
                fileCount
                    + " downloaded image"
                    + (fileCount == 1 ? " was" : "s were")
                    + " deleted.");

            // Reset the fields for the next run.
            resetFields();
        }
    }

    /**
     * A helper method that deletes files in a specified
     * directory.
     */
    public static Integer deleteFiles(Uri directoryPathname,
                                int fileCount) {
        File imageDirectory =
            new File(directoryPathname.toString());
        File files[] = imageDirectory.listFiles();

        if (files == null || files.length == 0)
            return fileCount;

        /**
         Specific use case - only files are added to directory so just iterate & delete WITHOUT
         DELETING dir. This avoids the cursed Android sporadic FileNotFound bug when trying to
         re-create dir again!!!
        **/

        for (final File f : files) {
            Log.d("DeleteFiles",
                "deleting file "
                    + f.toString()
                    + " with count "
                    + fileCount);
            ++fileCount;
            f.delete();
        }
        return fileCount;
    }
        /**
         * For deleting directory which contains other directories.

        // Android does not allow you to delete a directory with child
        // files, so we need to write code that handles this
        // recursively.
        for (final File f : files) {
            final Uri fileOrDirectoryName = Uri.parse(f.toString());
            if (f.isDirectory())
                fileCount += deleteFiles(fileOrDirectoryName,
                        fileCount);
            Log.d(TAG,
                    "deleting file "
                            + fileOrDirectoryName.toString()
                            + " with count "
                            + fileCount);
            ++fileCount;
            f.delete();
        }

        imageDirectory.delete();
        return fileCount;
    }
    */
    @Override
    public void setId(String id) {
        getModel().setId(id);
    }
    @Override
    public String getServiceDesc() { return getModel().getServiceDesc();}
    /**
     * Return the Activity context.
     */
    @Override
    public Context getActivityContext() {
        return mView.get().getActivityContext();
    }

    /**
     * Return the Application context.
     */
    @Override
    public Context getApplicationContext() {
        return mView.get().getApplicationContext();
    }

}
