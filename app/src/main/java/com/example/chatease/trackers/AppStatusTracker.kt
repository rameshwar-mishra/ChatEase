package com.example.chatease.trackers

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.example.chatease.activities.SignInActivity
import com.example.chatease.activities.SignUpActivity
import com.google.firebase.auth.FirebaseAuth

class AppStatusTracker : Application(), Application.ActivityLifecycleCallbacks {

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        if(FirebaseAuth.getInstance().currentUser != null && activity !is SignInActivity && activity !is SignUpActivity) {
            HeartBeatManager.startHeartBeat(FirebaseAuth.getInstance().currentUser!!.uid)
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if(FirebaseAuth.getInstance().currentUser != null && activity !is SignInActivity && activity !is SignUpActivity) {
            HeartBeatManager.stopHeartBeat()
        }
    }



    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}

}

//override fun onCreate() {
//    super.onCreate()
//    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
//}
//
//fun onForeground() {
//
//}
//
//fun onBackgroun() {
//
//}