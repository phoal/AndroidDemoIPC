package phoal.piko.common;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import phoal.piko.MVP;

/**
 * This Activity provides a framework for mediating access to a object
 * residing in the Presenter layer in the Model-View-Presenter (MVP)
 * pattern.  It automatically handles runtime configuration changes in
 * conjunction with an instance of PresenterType, which must implement
 * the PresenterOps interface.  It extends LifecycleLoggingActivity so
 * that all lifecycle hook method calls are automatically logged.  It
 * also implements the ContextView interface that provides access to
 * the Activity and Application contexts in the View layer.
 *
 * The three types used by a GenericActivity are the following:
 * <ol>
 *     <li><code>RequiredViewOps</code>, the class or interface that
 *     defines the methods available to the Presenter object from the
 *     View layer.</li> 
 *     <li><code>ProvidedPresenterOps</code>, the class or interface
 *     that defines the methods available to the View layer from the
 *     Presenter object.</li>
 *     <li><code>PresenterType</code>, the class created/used by the
 *     GenericActivity framework to implement an Presenter object.</li>
 * </ol>
 */
public abstract class GenericActivity<RVO extends MVP.RequiredViewOps,
                                      PPO extends MVP.ProvidedPresenterOps,
                                      PresenterType extends PPO>
       extends AppCompatActivity
       implements MVP.RequiredViewOps {

    protected final String TAG = getClass().getSimpleName();
    /**
     * Used to retain the ProvidedPresenterOps state between runtime
     * configuration changes.
     *
     * For a COMMON RETAINED FRAGMENT use FRAG_ID for identifier
     * For a UNIQUE RETAINED FRAGMENT use TAG.
     * In each case the PRESENTER from another MVP framework can be retrieved using
     * PRESENTERCLASS.class.getSimpleName() for CROSSOVER DATA.
     */
    protected final String FRAG_ID = "fragId";
    private final RetainedFragmentManager mRetainedFragmentManager
            /**
             * Use FRAG_ID since both DownloadActivity and DisplayWeatherActivity share both the
             * Retained Fragment and the MP classes.
             */
        = new RetainedFragmentManager(this.getFragmentManager(),
                                      FRAG_ID);
 
    /**
     * Instance of the Presenter type.
     */
    private PresenterType mPresenterInstance;

    /**
     * Initialize or reinitialize the Presenter layer.  This must be
     * called *after* the onCreate(Bundle saveInstanceState) method.
     *
     * @param opsType 
     *            Class object that's used to create a Presenter object.
     * @param view
     *            Reference to the RequiredViewOps object in the View layer.
     */
    public void onCreate(Class<PresenterType> opsType,
                         RVO view) {
        // Handle configuration-related events, including the initial
        // creation of an Activity and any subsequent runtime
        // configuration changes.
        try {
            // If this method returns true it's the first time the
            // Activity has been created.
            if (mRetainedFragmentManager.firstTimeIn()) {
                Log.d(TAG,
                      "First time calling onCreate()");

                // Initialize the GenericActivity fields.
                initialize(opsType,
                           view);
            } else {
                Log.d(TAG,
                      "Second (or subsequent) time calling onCreate()");

                // The RetainedFragmentManager was previously
                // initialized, which means that a runtime
                // configuration change occurred.
                reinitialize(opsType,
                             view);
            }
        } catch (InstantiationException
                 | IllegalAccessException e) {
            Log.d(TAG, 
                  "onCreate() " 
                  + e);
            // Propagate this as a runtime exception.
            throw new RuntimeException(e);
        }
    }

    /**
     * Return the initialized ProvidedPresenterOps instance for use by
     * application logic in the View layer.
     */

    public PPO getPresenter() {
        return  mPresenterInstance;
    }

    /**
     * Return the RetainedFragmentManager.
     */
    public RetainedFragmentManager getRetainedFragmentManager() {
        return mRetainedFragmentManager;
    }

    /**
     * Initialize the GenericActivity fields.
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    private void initialize(Class<PresenterType> opsType,
                            MVP.RequiredViewOps view)
            throws InstantiationException, IllegalAccessException {
        // Create the PresenterType object.
        mPresenterInstance = opsType.newInstance();

        // Put the PresenterInstance into the RetainedFragmentManager under
        // the simple name.
        mRetainedFragmentManager.put(opsType.getSimpleName(),
                                     mPresenterInstance);

        // Perform the first initialization.
        mPresenterInstance.onCreate(view);
    }

    /**
     * Reinitialize the GenericActivity fields after a runtime
     * configuration change.
     * @throws IllegalAccessException 
     * @throws InstantiationException 
     */
    private void reinitialize(Class<PresenterType> opsType,
                              MVP.RequiredViewOps view)
            throws InstantiationException, IllegalAccessException {
        // Try to obtain the PresenterType instance from the
        // RetainedFragmentManager.
        mPresenterInstance =
            mRetainedFragmentManager.get(opsType.getSimpleName());

        // This check shouldn't be necessary under normal
        // circumstances, but it's better to lose state than to
        // crash!
        if (mPresenterInstance == null) 
            // Initialize the GenericActivity fields.
            initialize(opsType,
                       view);
        else
            // Inform it that the runtime configuration change has
            // completed.
            mPresenterInstance.onConfigurationChange(view);
    }

    /**
     * Return the Activity context.
     */
    @Override
    public Context getActivityContext() {
        return this;
    }
    
    /**
     * Return the Application context.
     */
    @Override
    public Context getApplicationContext() {
        return super.getApplicationContext();
    }
}

