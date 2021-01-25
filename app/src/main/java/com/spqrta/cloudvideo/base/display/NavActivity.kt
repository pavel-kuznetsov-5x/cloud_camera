package com.spqrta.cloudvideo.base.display

import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.spqrta.cloudvideo.R

import org.jetbrains.annotations.TestOnly

abstract class NavActivity: BaseActivity() {

    protected val allowNotBaseFragments = false

    protected lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        navController = findNavController(R.id.navHost)

        navController.addOnDestinationChangedListener { _, destination, bundle ->
            try {
                getCurrentBaseFragment()?.onLeave()
                onDestinationChanged(destination)
            } catch (e: IndexOutOfBoundsException) {
            }
        }

    }

    protected open fun onDestinationChanged(destination: NavDestination) {

    }

    protected fun getCurrentBaseFragment(): BaseFragment<*>? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHost)
        val res = navHostFragment!!.childFragmentManager.fragments[0]
        if (res is BaseFragment<*>) {
            return res
        } else {
            if(allowNotBaseFragments) {
                return null
            } else {
                throw IllegalStateException("allowNotBaseFragments = false")
            }
        }
        return null
    }

//    protected fun getCurrentFragment(): Fragment {
//        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHost)
//        return navHostFragment!!.childFragmentManager.fragments[0]
//    }

    override fun onBackPressed() {
        if(getCurrentBaseFragment()?.onBackPressed() == false) {
            super.onBackPressed()
        }
    }

    @TestOnly
    fun getNavigationController(): NavController {
        return navController
    }
}