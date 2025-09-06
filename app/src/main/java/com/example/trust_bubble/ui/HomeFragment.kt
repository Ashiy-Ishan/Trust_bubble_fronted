package com.example.trust_bubble.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.trust_bubble.databinding.FragmentHomeBinding
import com.example.trust_bubble.network.RetrofitClient
import com.example.trust_bubble.services.BubbleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImageSelection(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        updateUiStatus()
        fetchHistoryAndCalculatePercentage()
    }

    private fun setupClickListeners() {
        binding.btnActiveScan.setOnClickListener {
            toggleBubbleService()
        }
        binding.btnScanImage.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
        binding.btnScanText.setOnClickListener {
            Toast.makeText(requireContext(), "Scan text is coming soon!", Toast.LENGTH_SHORT).show()
        }
        binding.cardProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Profile page is coming soon!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleBubbleService() {
        if (isMyServiceRunning(BubbleService::class.java)) {
            requireActivity().stopService(Intent(requireContext(), BubbleService::class.java))
            Toast.makeText(requireContext(), "Bubble deactivated", Toast.LENGTH_SHORT).show()
        } else {
            if (Settings.canDrawOverlays(requireContext())) {
                requireActivity().startService(Intent(requireContext(), BubbleService::class.java))
                Toast.makeText(requireContext(), "Bubble activated", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Overlay permission is required", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${requireActivity().packageName}"))
                startActivity(intent)
            }
        }
        view?.postDelayed({ updateUiStatus() }, 300)
    }

    private fun updateUiStatus() {
        if (isMyServiceRunning(BubbleService::class.java)) {
            binding.btnActiveScan.text = "Deactivate Scan Bubble"
        } else {
            binding.btnActiveScan.text = "Activate Scan Bubble"
        }
    }

    private fun fetchHistoryAndCalculatePercentage() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.getHistory()
                if (response.isSuccessful) {
                    val history = response.body()?.history ?: emptyList()
                    val percentage = if (history.isNotEmpty()) {
                        val goodCount = history.count { it.classification.equals("Good", ignoreCase = true) }
                        (goodCount.toDouble() / history.size * 100).toInt()
                    } else {
                        0
                    }
                    withContext(Dispatchers.Main) {
                        binding.progressCircular.progress = percentage
                        binding.tvProgressPercentage.text = "$percentage%"
                        binding.tvProgressLabel.text = if (history.isNotEmpty()) "$percentage% reliable shares" else "No reliable shares yet"
                    }
                }
            } catch (e: Exception) {
                // Could show an error message if desired
            }
        }
    }

    private fun handleImageSelection(uri: Uri) {
        Toast.makeText(requireContext(), "Preparing image...", Toast.LENGTH_SHORT).show()
        // Launch a coroutine on a background thread to handle the slow file reading
        lifecycleScope.launch(Dispatchers.IO) {
            val inputStream = requireActivity().contentResolver.openInputStream(uri)
            val imageBytes = inputStream?.readBytes()
            inputStream?.close()

            imageBytes?.let {
                // The uploadImage function already handles its own background work,
                // so we can call it directly from here.
                uploadImage(it)
            }
        }
    }

    private fun uploadImage(imageBytes: ByteArray) {
        Toast.makeText(requireContext(), "Analyzing image...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 1. Create the RequestBody directly from the image data
                val requestFile = imageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())

                // 2. Send that RequestBody directly. No more Multipart.
                val response = RetrofitClient.instance.analyzeScreenshot(requestFile)

                withContext(Dispatchers.Main) {
                    val message = if (response.isSuccessful) {
                        "Analysis: ${response.body()?.classification ?: "Unknown"}"
                    } else {
                        "Error: ${response.message()}"
                    }
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failure: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        try {
            val manager = requireActivity().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}