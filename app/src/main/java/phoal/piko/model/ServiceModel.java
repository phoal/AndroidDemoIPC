package phoal.piko.model;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import phoal.piko.MVP;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
/**
 * Created by phoal on 5/12/2015.
 */

/**
 * This class plays the "Model" role in the Model-View-Presenter (MVP)
 * pattern by defining an interface for providing data that will be
 * acted upon by the "Presenter" and "View" layers in the MVP pattern.
 * It implements the MVP.ProvidedModelOps so it can be created/managed
 * by the GenericModel framework.  This class plays the role of the
 * "Abstraction" in the Bridge pattern to decouple the interface of
 * the Model layer from the particular type of Service used to
 * implement this layer.
 */
public class ServiceModel
        implements MVP.ProvidedModelOps {
    /**
     * Debugging tag used by the Android logger.
     */
    protected final static String TAG =
            ServiceModel.class.getSimpleName();

    /**
     * Stores the directory to be used for all downloaded images.
     */
    public static final Uri DIR_PATH = Uri.parse(Environment.
            getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM) + "/phoal/");
    /**
     * Array of Strings that represent the valid URLs that have
     * been entered.
     */
    private ArrayList<Uri> mUrlList;

    // Unique id string for data downloads
    private String mId;
    // Unique session timestamp
    final String timestamp = "" + System.currentTimeMillis();
    // Unique id for each initial task - accessed from same thread
    long tId = 0L;
    // ServiceType ids
    public final static String SERVICE_TYPE = "service";

    public final static int ASYNCTASK = 10;
    public final static int STARTED_SERVICE = 11;
    public final static int BOUND_SERVICE = 12;
    public final static int AIDL_SYNC = 13;
    public final static int AIDL_ASYNC = 14;

    // ServiceType Messages
    public final static String ASYNCTASK_DESC = "AsyncTask with AsyncTask.THREAD_POOL_EXECUTOR";
    public final static String STARTED_SERVICE_DESC = "IntentService Started Service";
    public final static String BOUND_SERVICE_DESC = "Bound Service with a Messenger";
    public final static String AIDL_SYNC_DESC = "Bound Service using a synchronous two-way AIDL call";
    public final static String AIDL_ASYNC_DESC = "Bound Service using asynchronous one-way AIDL calls";

    /**
     * Indicates the desired type of Service.
     */
    public enum ServiceType {
        ASYNCTASK,
        STARTED_SERVICE,    // Use a Started Service to download an image.
        BOUND_SERVICE
    }

    /**
     * Type of Service (i.e., STARTED_SERVICE) to use for the
     * ServiceModel implementation.
     */
    private int mServiceType;
    // Flag indicating whether AIDL CALLS  are being used (Weather Service)
    private Boolean aidlMode;
    // The active message for the ServiceType.
    private  String serviceDesc;

    /**
     * Reference to the selected implementation.  Play the role of the
     * "Impl" in the Bridge pattern.
     */
    private ModelImpl mModelImpl;

    /**
     * A WeakReference used to access methods in the Presenter layer.
     * The WeakReference enables garbage collection.
     */
    protected WeakReference<MVP.RequiredPresenterOps> mImagePresenter;

    public ServiceModel() {

    }

    /**
     * Hook method called when a new ServiceModel instance is created.
     * Simply forward to the implementation.
     *
     * @param presenter
     *            A reference to the Presenter layer.
     */
    @Override
    public void onCreate(MVP.RequiredPresenterOps presenter) {
        // Set the WeakReference.
        mImagePresenter = new WeakReference<>(presenter);

        // Set up image directory
        // Create a directory path.
        File directoryPath =
                new File(DIR_PATH.getPath());

        // If the directory doesn't exist already then create it.
        if (!directoryPath.exists())
            directoryPath.mkdirs();

        // Create a STARTED_SERVICE.
        setServiceType(STARTED_SERVICE);
        // Initialize the list of URLs.
        mUrlList = new ArrayList<Uri>();
    }

    /**
     * Hook method called to shutdown the Presenter layer.
     */
    @Override
    public void onDestroy(boolean isChangingConfigurations) {
        // Forward the onDestroy().
        mModelImpl.onDestroy(isChangingConfigurations);
    }

    /**
     * Start a download.  When the download finishes its results are
     * passed up to the Presentation layer via the
     * onDownloadComplete() method defined in RequiredPresenterOps.
     *
     * @param applicationContext the ApplicationContext
     */
    @Override
    public void startProcessing(Context applicationContext) {
        if (aidlMode) mModelImpl.startProcess(null, null, mId);
        else for (Uri url : mUrlList)
                mModelImpl.startProcess(applicationContext, url, "00" + tId++ + timestamp);
    }

    /**
     * Set the type of Service to use for the CONCRETE ServiceModel
     * implementation.
     *
     * @param serviceType
     *            Type of Service, i.e., STARTED_SERVICE.
     */
    public void setServiceType(int serviceType) {
        // Only set the new ServiceType if it's different than the one
        // that's already in place.
        if (mServiceType != serviceType) {
            mServiceType = serviceType;
            if (mModelImpl != null)
                // Destroy the existing implementation, if any.
                mModelImpl.onDestroy(false);

            switch (mServiceType) {
                case ASYNCTASK:
                    // Create an implementation that uses AsyncTask
                    mModelImpl = new ImageModelImplAsyncTask();
                    serviceDesc = ASYNCTASK_DESC;
                    break;
                case STARTED_SERVICE:
                    // Create an implementation that uses a Started Service.
                    mModelImpl = new ImageModelImplStartedService();
                    serviceDesc = STARTED_SERVICE_DESC;
                    break;
                case BOUND_SERVICE:
                    // Create an implementation that uses a Bound Service.
                    mModelImpl = new ImageModelImplBoundService();
                    serviceDesc = BOUND_SERVICE_DESC;
                    break;
                case AIDL_SYNC:
                    // Create an implementation that uses a Bound Service.
                    mModelImpl = new AidlImplSync();
                    serviceDesc = AIDL_SYNC_DESC;
                    break;
                case AIDL_ASYNC:
                    // Create an implementation that uses a Bound Service.
                    mModelImpl = new AidlImplAsync();
                    serviceDesc = AIDL_ASYNC_DESC;
                    break;
                default: mModelImpl = new ImageModelImplStartedService();
            }

            // Initialize the ServiceModel implementation.
            mModelImpl.onCreate(mImagePresenter.get());
            // set mode flag
            if (serviceType == AIDL_ASYNC || serviceType == AIDL_SYNC) aidlMode = true;
            else aidlMode = false;
        }
    }
    @Override
    public String getServiceDesc() { return serviceDesc;}
    @Override
    public Boolean getAidlMode () { return aidlMode; }
    /**
     * Get the list of URLs.
     */
    @Override
    public ArrayList<Uri> getUrlList() {
        return mUrlList;
    }
    @Override
    public void setId(String id) {mId = id;}
}

