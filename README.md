**Android Demo IPC**

**A compact demo app which unifies my study of programming Android Services offered by Vanderbilt University**
- Uses the Bridge Pattern to swap between Android frameworks.
- Each framework demonstrates a solution for handling Interprocess Communication in Android when background Services are required by the main Activity.
- An MVP pattern is established using RetainedFragment to ensure data is persisted during configuration changes.

**5 frameworks are used:**
- AsyncTask (in conjunction with a custom ThreadPoolExecutor).
- Bound and Started Services. ( Uses a Messenger for return communication.)
- IPC using AIDL calls.


