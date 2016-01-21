# AndroidDemoIPC
Uses the Bridge Pattern to swap between Android frameworks to facilitate communication between an Activity and Services.
5 frameworks are used
- AsyncTask using a ThreadPoolExecutor.
- Bound and Started Services. ( Uses a Messenger for return communication.)
- IPC using AIDL calls.

An MVP pattern is established using RetainedFragment to handle configuration changes.
