# list of scheduled WorkManager works
`` adb shell dumpsys jobscheduler`` for API 23+ 
https://stackoverflow.com/questions/55879642/adb-command-to-list-all-scheduled-work-using-workmanager

"force stop" the app will cancel the scheduled update check.
But it will reinitialise after WorkManager is reinitialise (when calling WorkManager.getInstance(context))
Some OS will heavily force-stop apps.

A period work returned "failed" (instead of "success") will not be executed again.
