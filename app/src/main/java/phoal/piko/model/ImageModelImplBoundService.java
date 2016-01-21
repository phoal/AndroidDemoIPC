package phoal.piko.model;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

import phoal.piko.common.Utils;
import phoal.piko.model.datamodel.RequestMessage;
import phoal.piko.model.services.DownloadImagesBoundService;

/**
 * This class implements the image downloading operations using an
 * Android Bound Service.  It plays the role of the "Concrete
 * Implementor" in the Bridge pattern.
 */
public class ImageModelImplBoundService 
       extends ModelImpl {
    /**
     * Reference to the reply Messenger that's passed to the
     * DownloadImagesBoundService and used to return image results via
     * the Handler.
     */
    private Messenger mReplyMessenger = null;

    /**
     * Reference to the request Messenger that's implemented in the
     * DownloadImagesBoundService and used to send request messages to
     * the Service.
     */
    private Messenger mRequestMessengerRef = null;

    /** 
     * Used to receive a reference to the RequestMessenger after
     * binding to the DownloadImagesBoundService using bindService().
     */
    private ServiceConnection mServiceConnection = 
        new ServiceConnection() {
            /**
             * Called by the Android Binder framework after the
             * DownloadImagesBoundService is connected to convey the
             * result returned from onBind().
             */
            public void onServiceConnected(ComponentName className,
                                           IBinder binder) {
                Log.d(TAG,
                      "onServiceConnected() " 
                      + className);

                // Create a new Messenger that encapsulates the
                // returned IBinder object and store it for later use
                // in mRequestMessengerRef.
                // TODO -- you fill in here.
                mRequestMessengerRef = new Messenger(binder);

            }

            /**
             * Called if the Service crashes and is no longer
             * available.  The ServiceConnection will remain bound,
             * but the Service will not respond to any requests.
             */
            public void onServiceDisconnected(ComponentName className) {
                Log.d(TAG,
                      "onServiceDisconnected ");
                // Reset the reference to the RequestMessenger to
                // null, thereby preventing send() calls until it's
                // reconnected.
                // TODO -- you fill in here.
                mRequestMessengerRef = null;
            }
	};

    /**
     * Constructor initializes the Reply Messenger.
     */
    public ImageModelImplBoundService() {
        // Initialize the Reply Messenger.
        mReplyMessenger = 
            new Messenger(this);
    }        

    /**
     * Initiate the protocol for binding the Services.
     */
    @Override
    protected void bindService() {
        Log.d(TAG,
              "calling bindService()");

        if (mRequestMessengerRef == null) {
            // Create a new intent to the DownloadImagesBoundService
            // that can download an image from the URL given by the
            // user.
            // TODO - you fill in here.
            /**
             *** IMPORTANT ***
             * It is important to use the correct Context in the two places it's required - refer
             * to Doug's AcronymExpander app ( bindServices() method in AcronymModel)
             * 1. The makeIntent() constructor method PREFERS the use of ActivityContext since this
             *  is a one-off reference to the current Activity. It's used to retrieve the Android
             *  application package name when when the Intent(packageContext ...) constructor is called.
             * 2. The bindService() method MUST be called by APPLICATION Context since binding is in
             *  the Model layer and can extend beyond the current Activity's lifecycle.
             */
            // An intent is used in a one-off reference and prefers the use of the current ActivityContext
            Intent boundIntent = DownloadImagesBoundService.makeIntent(mPresenter.get()
                    .getActivityContext());

            Log.d(TAG,
                  "calling bindService()");

            // Bind to the Service associated with the Intent.
            // TODO -- you fill in here.
            // ApplicationContext is REQUIRED here - the binding can extend beyond the calling
            // Activity's lifecycle.
            mPresenter.get().getApplicationContext().
                    bindService(boundIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Initiate the protocol for unbinding the Services.
     */
    @Override
    protected void unbindService() {
        Log.d(TAG,
              "calling unbindService()");
        if (mRequestMessengerRef != null) {
            Log.d(TAG,
                  "calling unbindService()");
            // Unbind from the Service.
            // TODO -- you fill in here.
            mPresenter.get().getApplicationContext().unbindService(mServiceConnection);
            /**
             * Set this field to null to trigger a call to
             * bindService() next time bindService() is called.
             * Also allows DownloadImagesBoundService to be GC'ed by removing this stored reference
             * to it.
             */
            // TODO -- you fill in here.
            mRequestMessengerRef = null;
        } 
    }

    /**
     * Start a download.  When the download finishes its results are
     * passed up to the Presentation layer via the
     * onDownloadComplete() method defined in RequiredPresenterOps.
     * This method plays the role of the "Primitive Operation"
     * (a.k.a., "Abstract Hook Method") in the Template Method
     * pattern.
     *
     * @param url
     *        URL of the image to download.
     * @param fileId
     *        Uri of the directory to store the downloaded image.
     */
    @Override
    public void startProcess(Context context, Uri url,
                              String fileId) {
        if (mRequestMessengerRef == null) 
            Utils.showToast(mPresenter.get().getActivityContext(),
                            "not bound to the service");
        else {
            try {
                // Create a RequestMessage that indicates the
                // DownloadImagesBoundService should send the reply
                // back to ReplyHandler encapsulated by the Messenger.
                final RequestMessage requestMessage =
                    RequestMessage.makeRequestMessage
                    (OperationType.DOWNLOAD_IMAGE.ordinal(),
                     url,
                     ServiceModel.DIR_PATH,
                     fileId,
                     mReplyMessenger);

                Log.d(TAG,
                      "sending a request message to DownloadImagesBoundService for "
                      + url.toString());

                // Send the request Message to the
                // DownloadImagesBoundService.
                // TODO -- you fill in here.
                mRequestMessengerRef.send(requestMessage.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
