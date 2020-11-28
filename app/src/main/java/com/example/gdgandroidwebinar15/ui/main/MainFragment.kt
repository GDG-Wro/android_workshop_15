package com.example.gdgandroidwebinar15.ui.main

import android.app.Activity.RESULT_CANCELED
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gdgandroidwebinar15.R
import com.example.gdgandroidwebinar15.clicks
import com.example.gdgandroidwebinar15.consume
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.ActivityResult.RESULT_IN_APP_UPDATE_FAILED
import com.google.android.play.core.install.model.InstallErrorCode
import com.google.android.play.core.ktx.*
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import kotlinx.android.synthetic.main.main_fragment.*
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private val Fragment.viewCoroutineScope: LifecycleCoroutineScope
    get() = viewLifecycleOwner.lifecycleScope

class MainFragment : Fragment(R.layout.main_fragment) {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var updateManager: AppUpdateManager
    private val viewModel: MainViewModel by viewModels { MainViewModelFactory(requireContext()) }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        updateManager = AppUpdateManagerFactory.create(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        setUpRefreshButton()
        requestInAppUpdate()
    }

    private fun requestInAppUpdate() = viewCoroutineScope.launch {
        updateManager.requestUpdateFlow()
            .catch {
                Log.e("UPDATE", "In-App Update error", it)
                Firebase.crashlytics.recordException(it)
            }
            .collect { result ->
                updateProgress.isVisible = result is AppUpdateResult.InProgress &&
                        result.installState.installErrorCode == InstallErrorCode.NO_ERROR
                when (result) {
                    AppUpdateResult.NotAvailable -> Toast.makeText(
                        requireContext(),
                        "Update not available",
                        Toast.LENGTH_SHORT
                    ).show()
                    is AppUpdateResult.Available -> showUpdateDialog(result)
                    is AppUpdateResult.InProgress -> showProgress(result)
                    is AppUpdateResult.Downloaded -> showCompleteUpdateDialog(result)
                }
            }
    }

    private fun showUpdateDialog(result: AppUpdateResult.Available) {
        AlertDialog.Builder(requireContext())
            .setTitle("App update is available")
            .setMessage("Choose update mode:")
            .apply {
                if (result.updateInfo.isFlexibleUpdateAllowed) setPositiveButton("Flexible") { _, _ ->
                    result.startFlexibleUpdate(this@MainFragment, 103)
                }
                if (result.updateInfo.isImmediateUpdateAllowed) setNegativeButton("Immediate") { _, _ ->
                    result.startImmediateUpdate(this@MainFragment, 103)
                }
            }
            .show()
    }

    private fun showProgress(result: AppUpdateResult.InProgress) {
        if (result.installState.installErrorCode == InstallErrorCode.NO_ERROR) {
            updateProgress.isVisible = true
            updateProgress.progress = result.installState.runCatching {
                bytesDownloaded() * 100 / totalBytesToDownload()
            }.recover { 0L }.getOrThrow().toInt()
        } else {
            updateProgress.isVisible = false
            Toast.makeText(
                requireContext(),
                "Install error ${result.installState.installErrorCode}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private suspend fun showCompleteUpdateDialog(result: AppUpdateResult.Downloaded) {
        suspendCancellableCoroutine<Boolean> { continuation ->
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("App update is ready")
                .setMessage("Do you want to apply the update and restart app?")
                .setPositiveButton("Yes") { _, _ -> continuation.resume(true) }
                .setNegativeButton("No") { _, _ -> continuation.resume(false) }
                .show()
            continuation.invokeOnCancellation { dialog.dismiss() }
        }.let { shouldCompleteUpdate -> if (shouldCompleteUpdate) result.completeUpdate() }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 103) {
            when (resultCode) {
                RESULT_CANCELED -> Toast.makeText(
                    requireContext(),
                    "Update cancelled",
                    Toast.LENGTH_SHORT
                ).show()
                RESULT_IN_APP_UPDATE_FAILED -> Toast.makeText(
                    requireContext(),
                    "Update failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun setUpRefreshButton() {
        viewCoroutineScope.launch {
            refreshButton.clicks().collect { fetchForecast() }
        }
        refreshButton.setOnLongClickListener {
            Firebase.analytics.setUserProperty("curious", "yes")
            Toast.makeText(context, "\uD83D\uDC4B", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private suspend fun fetchForecast() {
        val shouldAskForReview = viewCoroutineScope.async {
            Firebase.remoteConfig.runCatching { fetchAndActivate() }
            return@async Firebase.remoteConfig.getBoolean("ask_for_review")
        }
        val fetchForecastJob = viewModel.fetchForecast()
        if (!shouldAskForReview.await()) return

        val reviewManager = ReviewManagerFactory.create(requireContext())
        val reviewInfo = reviewManager.requestReview()
        fetchForecastJob.join()
        reviewManager.launchReview(requireActivity(), reviewInfo)
    }

    private fun setUpRecyclerView() = with(weatherRecyclerView) {
        val weatherAdapter = WeatherAdapter()
        adapter = weatherAdapter
        layoutManager = LinearLayoutManager(context)
        addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        observeUiModels(weatherAdapter)
    }

    private fun observeUiModels(weatherAdapter: WeatherAdapter) {
        viewCoroutineScope.launch {
            viewModel.models.collect {
                weatherAdapter.submitList(it.forecasts)
                loadingContainer.isVisible = it.isLoading
                it.error.consume {
                    Toast.makeText(context, R.string.fetch_error, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
