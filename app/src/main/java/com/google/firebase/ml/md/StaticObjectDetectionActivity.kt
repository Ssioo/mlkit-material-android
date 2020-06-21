/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.firebase.ml.md

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.common.collect.ImmutableList
import com.google.firebase.ml.md.databinding.ActivityStaticObjectKotlinBinding
import com.google.firebase.ml.md.models.Product
import com.google.firebase.ml.md.objectdetection.DetectedObject
import com.google.firebase.ml.md.objectdetection.StaticObjectDotView
import com.google.firebase.ml.md.productsearch.PreviewCardAdapter
import com.google.firebase.ml.md.productsearch.SearchEngine
import com.google.firebase.ml.md.productsearch.SearchedObject
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.objects.FirebaseVisionObject
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions
import java.io.IOException
import java.util.*

/** Demonstrates the object detection and visual search workflow using static image.  */
class StaticObjectDetectionActivity : AppCompatActivity() {

    private val searchedObjectMap = TreeMap<Int, SearchedObject>()

    private lateinit var binding: ActivityStaticObjectKotlinBinding

    private var inputBitmap: Bitmap? = null
    private var dotViewSize: Int = 0
    private var detectedObjectNum = 0
    private var currentSelectedObjectIndex = 0

    private var detector: FirebaseVisionObjectDetector? = null
    private var searchEngine: SearchEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchEngine = SearchEngine(applicationContext)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_static_object_kotlin)

        binding.cardRecyclerView.setHasFixedSize(true)

        dotViewSize = resources.getDimensionPixelOffset(R.dimen.static_image_dot_view_size)

        binding.closeButton.setOnClickListener { finish() }
        binding.photoLibraryButton.setOnClickListener { Utils.openImagePicker(this) }

        detector = FirebaseVision.getInstance()
                .getOnDeviceObjectDetector(
                        FirebaseVisionObjectDetectorOptions.Builder()
                                .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                                .enableMultipleObjects()
                                .build()
                )
        intent.data?.let(::detectObjects)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            detector?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close the detector!", e)
        }

        searchEngine?.shutdown()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Utils.REQUEST_CODE_PHOTO_LIBRARY && resultCode == Activity.RESULT_OK) {
            data?.data?.let(::detectObjects)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }


    private fun detectObjects(imageUri: Uri) {
        binding.inputImageView.setImageDrawable(null)
        binding.bottomPromptChip.visibility = View.GONE
        binding.cardRecyclerView.adapter = PreviewCardAdapter(ImmutableList.of()) {
            //아이템 터치 이벤트

        }
        binding.cardRecyclerView.clearOnScrollListeners()
        binding.dotViewContainer.removeAllViews()
        currentSelectedObjectIndex = 0

        try {
            inputBitmap = Utils.loadImage(this, imageUri, MAX_IMAGE_DIMENSION)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load file: $imageUri", e)
            showBottomPromptChip("Failed to load file!")
            return
        }

        binding.inputImageView.setImageBitmap(inputBitmap)
        binding.loadingView.visibility = View.VISIBLE
        val image = FirebaseVisionImage.fromBitmap(inputBitmap!!)
        detector?.processImage(image)
                ?.addOnSuccessListener { objects -> onObjectsDetected(image, objects) }
                ?.addOnFailureListener { onObjectsDetected(image, ImmutableList.of()) }
    }

    @MainThread
    private fun onObjectsDetected(image: FirebaseVisionImage, objects: List<FirebaseVisionObject>) {
        detectedObjectNum = objects.size
        Log.d(TAG, "Detected objects num: $detectedObjectNum")
        if (detectedObjectNum == 0) {
            binding.loadingView.visibility = View.GONE
            showBottomPromptChip(getString(R.string.static_image_prompt_detected_no_results))
        } else {
            searchedObjectMap.clear()
            for (i in objects.indices) {
                searchEngine?.search(DetectedObject(objects[i], i, image)) { detectedObject, products ->
                    onSearchCompleted(detectedObject, products)
                }
            }
        }
    }

    private fun onSearchCompleted(detectedObject: DetectedObject, productList: List<Product>) {
        Log.d(TAG, "Search completed for object index: ${detectedObject.objectIndex}")
        searchedObjectMap[detectedObject.objectIndex] = SearchedObject(detectedObject, productList)
        if (searchedObjectMap.size < detectedObjectNum) {
            // Hold off showing the result until the search of all detected objects completes.
            return
        }

        showBottomPromptChip(getString(R.string.static_image_prompt_detected_results))
        binding.loadingView.visibility = View.GONE
        binding.cardRecyclerView.adapter = PreviewCardAdapter(ImmutableList.copyOf(searchedObjectMap.values)) {
            // 요부분 아이템 클릭 리스너
            // 음악재생해야함.

        }
        binding.cardRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        Log.d(TAG, "New card scroll state: $newState")
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            for (i in 0 until recyclerView.childCount) {
                                val childView = recyclerView.getChildAt(i)
                                if (childView.x >= 0) {
                                    val cardIndex = recyclerView.getChildAdapterPosition(childView)
                                    if (cardIndex != currentSelectedObjectIndex) {
                                        selectNewObject(cardIndex)
                                    }
                                    break
                                }
                            }
                        }
                    }
                })

        for (searchedObject in searchedObjectMap.values) {
            val dotView = createDotView(searchedObject)
            dotView.setOnClickListener {
                // dot 클릭했을 때 리스너 부분.
                if (searchedObject.objectIndex == currentSelectedObjectIndex) {
                    //showSearchResults(searchedObject)
                } else {
                    selectNewObject(searchedObject.objectIndex)
                    //showSearchResults(searchedObject)
                    binding.cardRecyclerView.smoothScrollToPosition(searchedObject.objectIndex)
                }
            }

            binding.dotViewContainer.addView(dotView)
            val animatorSet = AnimatorInflater.loadAnimator(this, R.animator.static_image_dot_enter) as AnimatorSet
            animatorSet.setTarget(dotView)
            animatorSet.start()
        }
    }

    private fun createDotView(searchedObject: SearchedObject): StaticObjectDotView {
        val viewCoordinateScale: Float
        val horizontalGap: Float
        val verticalGap: Float
        val inputBitmap = inputBitmap ?: throw NullPointerException()
        val inputImageViewRatio = binding.inputImageView.width.toFloat() / binding.inputImageView.height
        val inputBitmapRatio = inputBitmap.width.toFloat() / inputBitmap.height
        if (inputBitmapRatio <= inputImageViewRatio) { // Image content fills height
            viewCoordinateScale = binding.inputImageView.height.toFloat() / inputBitmap.height
            horizontalGap = (binding.inputImageView.width - inputBitmap.width * viewCoordinateScale) / 2
            verticalGap = 0f
        } else { // Image content fills width
            viewCoordinateScale = binding.inputImageView.width.toFloat() / inputBitmap.width
            horizontalGap = 0f
            verticalGap = (binding.inputImageView.height - inputBitmap.height * viewCoordinateScale) / 2
        }

        val boundingBox = searchedObject.boundingBox
        val boxInViewCoordinate = RectF(
                boundingBox.left * viewCoordinateScale + horizontalGap,
                boundingBox.top * viewCoordinateScale + verticalGap,
                boundingBox.right * viewCoordinateScale + horizontalGap,
                boundingBox.bottom * viewCoordinateScale + verticalGap
        )
        val initialSelected = searchedObject.objectIndex == 0
        val dotView = StaticObjectDotView(this, initialSelected)
        val layoutParams = FrameLayout.LayoutParams(dotViewSize, dotViewSize)
        val dotCenter = PointF(
                (boxInViewCoordinate.right + boxInViewCoordinate.left) / 2,
                (boxInViewCoordinate.bottom + boxInViewCoordinate.top) / 2)
        layoutParams.setMargins(
                (dotCenter.x - dotViewSize / 2f).toInt(),
                (dotCenter.y - dotViewSize / 2f).toInt(),
                0,
                0
        )
        dotView.layoutParams = layoutParams
        return dotView
    }

    private fun selectNewObject(objectIndex: Int) {
        val dotViewToDeselect = binding.dotViewContainer.getChildAt(currentSelectedObjectIndex) as StaticObjectDotView
        dotViewToDeselect.playAnimationWithSelectedState(false)

        currentSelectedObjectIndex = objectIndex

        val selectedDotView = binding.dotViewContainer.getChildAt(currentSelectedObjectIndex) as StaticObjectDotView
        selectedDotView.playAnimationWithSelectedState(true)
    }

    private fun showBottomPromptChip(message: String) {
        binding.bottomPromptChip.visibility = View.VISIBLE
        binding.bottomPromptChip.text = message
    }

    companion object {
        private const val TAG = "StaticObjectActivity"
        private const val MAX_IMAGE_DIMENSION = 1024
    }
}
