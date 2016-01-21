package phoal.piko;

import java.util.ArrayList;

import phoal.piko.common.ContextView;
import phoal.piko.common.ModelOps;
import phoal.piko.common.PresenterOps;
import phoal.piko.model.aidl.WeatherData;
import phoal.piko.model.datamodel.ReplyMessage;

import android.content.Context;
import android.net.Uri;

/**
 * Defines the interfaces for the Download Image Viewer application
 * that are required and provided by the layers in the
 * Model-View-Presenter (MVP) pattern.  This design ensures loose
 * coupling between the layers in the app's MVP-based architecture.
 */
public interface MVP {
    /**
     * This interface defines the minimum API needed by the
     * DataPresenter class in the Presenter layer to interact with
     * DownloadActivity in the View layer.  It extends the
     * ContextView interface so the Model layer can access Context's
     * defined in the View layer.
     */
    public interface RequiredViewOps
           extends ContextView {
        /**
         * Make the ProgressBar visible.
         */
        void displayProgressBar();

        /**
         * Make the ProgressBar invisible.
         */
        void dismissProgressBar();

        /**
         * Display the URLs provided by the user thus far.
         */
        void displayUrls();

        /**
         * Handle failure to download an image.
         */
        void reportDownloadFailure(Uri url,
                                   boolean downloadsComplete);

        /**
         * Start the DisplayImagesActivity to display the results of
         * the download to the user.
         */
        void displayResults(Uri directoryPathname);
        /**
         * Start the DisplayWeatherActivity to display the results of
         * the download to the user.
         */
        void displayResults(WeatherData weatherData,
                            String errorMessage);

        /**
         * Disables addUrl and Download buttons if processing has already started
         * @param processing set by presenter when startProcessing called
         */
        void disableButtons(Boolean processing);
    }

    /**
     * This interface defines the minimum API needed by the
     * DataPresenter class in the Presenter layer to interact with
     * DownloadActivity in the View layer.  It extends the
     * ContextView interface so the Model layer can access Context's
     * defined in the View layer.
     */
    public interface RequiredDisplayViewOps
        extends ContextView {
        void downloadCompleted();
    }
    /**
     * This interface defines the minimum public API provided by the
     * DataPresenter class in the Presenter layer to the
     * DownloadActivity in the View layer.  It extends the
     * PresenterOps interface, which is instantiated by the
     * MVP.RequiredViewOps interface used to define the parameter
     * that's passed to the onConfigurationChange() method.
     */
    public interface ProvidedPresenterOps
           extends PresenterOps<MVP.RequiredViewOps> {
        /**
         * Get the list of URLs.
         */
        ArrayList<Uri> getUrlList();

        /**
         * Start all the downloading and filtering.
         *
         */
        void startProcessing(Context applicationContext);

        /**
         * Set the type of Service to use for the ServiceModel
         * implementation.
         *
         * @param serviceType
         *            Type of Service, i.e., STARTED_SERVICE
         */
        void setServiceType(int serviceType);
        String getServiceDesc();
        /**
         *
         * @param id - an id string if necessary
         */
        void setId(String id);

        /**
         * Delete all the downloaded images.
         */
        void deleteDownloadedImages();

        /**
         * Return the initialized ProvidedModelOps instance for use by
         * the application.
         */
        MVP.ProvidedModelOps getModel();
    }

    /**
     * This interface defines the minimum API needed by the ServiceModel
     * class in the Model layer to interact with DataPresenter class
     * in the Presenter layer.  It extends the ContextView interface
     * so the Model layer can access Context's defined in the View
     * layer.
     */
    public interface RequiredPresenterOps
           extends ContextView {
        /**
         * Interact with the View layer to display the
         * downloaded/filtered images when all processing 
         * is complete.
         */
        void onProcessingComplete(ReplyMessage replyMessage);

        /**
         *
         * @param weatherData data object with fields parsed from json from service
         * @param errorMessage error msg if parsing fails
         */
        void displayResults(WeatherData weatherData,
                            String errorMessage);
    }

    /**
     * This interface defines the minimum public API provided by the
     * ServiceModel class in the Model layer to the DataPresenter class
     * in the Presenter layer.  It extends the ModelOps interface,
     * which is parameterized by the MVP.RequiredPresenterOps
     * interface used to define the argument passed to the
     * onConfigurationChange() method.
     */
    public interface ProvidedModelOps
           extends ModelOps<MVP.RequiredPresenterOps> {
        /**
         * Download the image located at the provided Internet url
         * using the URL class, and store it on the android file system
         * using a FileOutputStream.
         * When the download finishes its results
         * are passed up to the Presentation layer via the
         * onDownloadComplete() method defined in RequiredPresenterOps.
         *
         * @param applicationContext
         *          The context in which to write the file.
         */

        void startProcessing(Context applicationContext);

        /**
         * Set the type of Service to use for the ServiceModel
         * implementation.
         *
         * @param serviceType
         *            Type of Service, i.e., STARTED_SERVICE
         */
        void setServiceType(int serviceType);
        Boolean getAidlMode();
        String getServiceDesc();

        /**
         *
         * @param id - an id string if necessary
         */
        void setId(String id);

        /**
         *
         * @return The list of url s to download
         */
        public ArrayList<Uri> getUrlList();

    }
}
