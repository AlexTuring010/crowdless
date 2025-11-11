package com.example.crowdless

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar

class MainActivity : ComponentActivity() {
    private lateinit var mapView: MapView
    private lateinit var searchBar: EditText   // <--- add this
    private lateinit var searchResultsList: ListView

    // Use the same crowdPoints list (move it out of addCrowdPoints so we can reuse it)
    private val crowdPoints = listOf(
        Place("Syntagma", 23.734877081600565, 37.975547358690115, 0.8),
        Place("Pagkrati", 23.747794219452437, 37.967786130169294, 0.5),
        Place("Nea Smyrni", 23.712611526289123, 37.94392848817007, 0.2),
        Place("Kallithea", 23.699697099466754, 37.95576852924123, 0.25),
        Place("Piraeus", 23.6471836178194, 37.94331496240374, 0.3),
        Place("Zografou", 23.768781502196898, 37.97735788767318, 0.1)
    )

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Change status bar color (top)
        window.statusBarColor = Color.parseColor("#000000") // Replace with your color

        // Change navigation bar color (bottom)
        window.navigationBarColor = Color.parseColor("#000000") // Replace with your color

        // Reference the container
        val mapContainer = findViewById<FrameLayout>(R.id.map_container)

        // Create the map view with your custom style
        mapView = MapView(
            this,
            MapInitOptions(
                context = this,
                styleUri = "mapbox://styles/alexgkiafis/cmhtfrhbu00ck01qu0n1a7frl"
            )
        )

        // Add MapView to the container
        mapContainer.addView(
            mapView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // place this after you add the mapView to the container
        mapView.compass.updateSettings {
            enabled = false
        }

        mapView.logo.updateSettings {
            enabled = false
        }

        mapView.attribution.updateSettings {
            enabled = false
        }

        mapView.scalebar.updateSettings {
            enabled = false
        }

        // Check and request location permission
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableUserLocation()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        mapView.mapboxMap.getStyle { style ->
            addCrowdPoints(style)
        }

        val bottomSheet: LinearLayout = findViewById(R.id.bottom_sheet)
        val behavior = BottomSheetBehavior.from(bottomSheet)

        // 3 snapping positions
        behavior.isFitToContents = false
        behavior.peekHeight = 200                     // collapsed
        behavior.halfExpandedRatio = 0.5f            // middle
        behavior.isHideable = false
        behavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED

        // Screen & height calculations
        val screenHeight = resources.displayMetrics.heightPixels
        val halfHeight = screenHeight * 0.5
        val peekHeight = 200

        // ✅ Set initial camera padding to match middle state
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .padding(com.mapbox.maps.EdgeInsets(0.0, 0.0, halfHeight.toDouble(), 0.0))
                .build()
        )

        // Reference the container and inner button
        val btnMyLocationContainer = findViewById<FrameLayout>(R.id.btn_my_location)
        val btnMyLocationInner = findViewById<ImageButton>(R.id.btn_my_location_inner)

        // Compute initial position for the button (half-expanded)
        val initialBottomSheetTop = halfHeight.toInt()
        val containerParams = btnMyLocationContainer.layoutParams as FrameLayout.LayoutParams
        containerParams.bottomMargin = initialBottomSheetTop + 16 // 16px extra margin
        btnMyLocationContainer.layoutParams = containerParams

        val dragHandle = findViewById<View>(R.id.drag_handle)

        // Create a MaterialShapeDrawable
        val shapeDrawable = MaterialShapeDrawable().apply {
            // Fill color
            fillColor = ColorStateList.valueOf(Color.parseColor("#032C47"))

            // Stroke (border)
            strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                1f,
                resources.displayMetrics
            )
            strokeColor = ColorStateList.valueOf(Color.parseColor("#00D4FF")) // replace with your border color

            // Shape with top corners 36dp
            shapeAppearanceModel = ShapeAppearanceModel.builder()
                .setTopLeftCorner(CornerFamily.ROUNDED, TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    36f,
                    resources.displayMetrics
                ))
                .setTopRightCorner(CornerFamily.ROUNDED, TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    36f,
                    resources.displayMetrics
                ))
                .build()
        }


        bottomSheet.background = shapeDrawable

        val initialRadiusPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            36f,
            resources.displayMetrics
        )

        // Adjust map camera padding dynamically as the sheet moves
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Calculate the current bottom of visible map
                val collapsedHeight = peekHeight.toFloat()
                val halfHeightPx = halfHeight.toFloat()
                val bottomSheetTop = when {
                    slideOffset <= 0.5f -> {
                        // interpolate between collapsed and half
                        collapsedHeight + (halfHeightPx - collapsedHeight) * (slideOffset / 0.5f)
                    }
                    else -> halfHeightPx
                }

                // Update container bottom margin dynamically
                val params = btnMyLocationContainer.layoutParams as FrameLayout.LayoutParams
                params.bottomMargin = bottomSheetTop.toInt() + 100 // adjust extra margin if needed
                btnMyLocationContainer.layoutParams = params

                // Apply map padding as before
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .padding(com.mapbox.maps.EdgeInsets(0.0, 0.0, bottomSheetTop.toDouble(), 0.0))
                        .build()
                )

                if (slideOffset > 0.5f) {
                    // Remap slideOffset from 0.5→1 to 0→1
                    val progress = ((slideOffset - 0.5f) / 0.5f).coerceIn(0f, 1f)

                    // Animate corners: 36dp → 0dp
                    val radius = initialRadiusPx * (1 - progress)
                    shapeDrawable.shapeAppearanceModel = shapeDrawable.shapeAppearanceModel.toBuilder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, radius)
                        .setTopRightCorner(CornerFamily.ROUNDED, radius)
                        .build()

                    // Animate handle
                    val initialWidthPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        40f,
                        resources.displayMetrics
                    )
                    val minWidthPx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        16f,
                        resources.displayMetrics
                    )
                    val newWidth = (initialWidthPx * (1 - progress) + minWidthPx * progress).toInt()
                    dragHandle.layoutParams = dragHandle.layoutParams.apply { width = newWidth }
                    dragHandle.requestLayout()

                    dragHandle.alpha = 1 - progress
                } else {
                    // Keep corners and handle at original values until middle
                    shapeDrawable.shapeAppearanceModel = shapeDrawable.shapeAppearanceModel.toBuilder()
                        .setTopLeftCorner(CornerFamily.ROUNDED, initialRadiusPx)
                        .setTopRightCorner(CornerFamily.ROUNDED, initialRadiusPx)
                        .build()

                    dragHandle.layoutParams = dragHandle.layoutParams.apply {
                        width = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            40f,
                            resources.displayMetrics
                        ).toInt()
                    }
                    dragHandle.requestLayout()
                    dragHandle.alpha = 1f
                }
            }
        })

        // After you’ve loaded style and ensured location permission is granted
        val locationComponent = mapView.location  // LocationComponentPlugin
        locationComponent.updateSettings {
            enabled = true
            pulsingEnabled = true
        }

        // Prepare a var to hold latest point
        var lastUserPoint: com.mapbox.geojson.Point? = null

        // Create the listener
        val positionListener = com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener { point ->
            lastUserPoint = point
            // Do NOT auto-center here — only when button clicked
        }

        // Add the listener
        locationComponent.addOnIndicatorPositionChangedListener(positionListener)

        // Now set up the inner button with animated camera movement
        btnMyLocationInner.setOnClickListener {
            lastUserPoint?.let { userPoint ->
                // Build camera options for your target location
                val cameraOptions = CameraOptions.Builder()
                    .center(userPoint)
                    .zoom(14.0)
                    .build()

                // Build animation options (1.5 seconds duration)
                val animationOptions = com.mapbox.maps.plugin.animation.MapAnimationOptions.Builder()
                    .duration(3500L)
                    .build()

                // Animate the camera to user's location
                mapView.mapboxMap.flyTo(cameraOptions, animationOptions)
            }
        }

        // 🟢 Search bar setup
        searchBar = findViewById<EditText>(R.id.search_bar)
        searchResultsList = findViewById<ListView>(R.id.search_results_list)

        val allNames = crowdPoints.map { it.name }
        val adapter = ArrayAdapter(this, R.layout.list_item_white, R.id.list_item_text, mutableListOf<String>())
        searchResultsList.adapter = adapter

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim().orEmpty()
                val filtered = if (query.isNotEmpty()) {
                    allNames.filter { it.contains(query, ignoreCase = true) }
                } else emptyList()

                adapter.clear()
                adapter.addAll(filtered)
                adapter.notifyDataSetChanged()

                searchResultsList.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE

                // Adjust ListView height dynamically (max 400px)
                adjustListViewHeight(searchResultsList, 500)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle selection from dropdown
        searchResultsList.setOnItemClickListener { _, _, position, _ ->
            val selectedName = adapter.getItem(position)
            val selectedPlace = crowdPoints.find { it.name == selectedName }

            if (selectedPlace != null) {
                // Fly to the place
                val cameraOptions = CameraOptions.Builder()
                    .center(Point.fromLngLat(selectedPlace.lng, selectedPlace.lat))
                    .zoom(13.5)
                    .build()

                val animationOptions = com.mapbox.maps.plugin.animation.MapAnimationOptions.Builder()
                    .duration(3500L)
                    .build()

                mapView.mapboxMap.flyTo(cameraOptions, animationOptions)
            }

            // Hide the dropdown and clear focus
            searchResultsList.visibility = View.GONE
            searchBar.clearFocus()
            searchBar.setText("")

            // Hide the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchBar.windowToken, 0)
        }

        searchResultsList.bringToFront()
    }

    private fun adjustListViewHeight(listView: ListView, maxHeight: Int) {
        val adapter = listView.adapter ?: return
        var totalHeight = 0
        for (i in 0 until adapter.count) {
            val item = adapter.getView(i, null, listView)
            item.measure(
                View.MeasureSpec.makeMeasureSpec(listView.width, View.MeasureSpec.AT_MOST),
                View.MeasureSpec.UNSPECIFIED
            )
            totalHeight += item.measuredHeight
        }

        val finalHeight = if (totalHeight > maxHeight) maxHeight else totalHeight
        val params = listView.layoutParams
        params.height = finalHeight
        listView.layoutParams = params
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (view is EditText) {
                val outRect = Rect()
                view.getGlobalVisibleRect(outRect)

                val listRect = Rect()
                searchResultsList.getGlobalVisibleRect(listRect)

                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt()) &&
                    !listRect.contains(ev.rawX.toInt(), ev.rawY.toInt())
                ) {
                    view.clearFocus()
                    searchBar.setText("") // remove text
                    searchResultsList.visibility = View.GONE // hide dropdown
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                enableUserLocation()
            }
        }

    private fun enableUserLocation() {
        val locationComponent = mapView.location
        locationComponent.updateSettings {
            enabled = true
            pulsingEnabled = true
        }
    }

    private fun addCrowdPoints(style: com.mapbox.maps.Style) {
        val features: List<Feature> = crowdPoints.map { place ->
            Feature.fromGeometry(Point.fromLngLat(place.lng, place.lat)).apply {
                addStringProperty("name", place.name)
                addNumberProperty("crowdLevel", place.crowdLevel)
            }
        }

        // 🟢 Step 2: Add source
        style.addSource(
            geoJsonSource("crowd-points-source") {
                featureCollection(FeatureCollection.fromFeatures(features))
            }
        )

        // 🟢 Step 3: Add circle layer (colored by crowd level)
        style.addLayer(
            circleLayer("crowd-points-layer", "crowd-points-source") {
                circleRadius(8.0)
                circleColor(
                    Expression.interpolate {
                        linear()
                        get { literal("crowdLevel") }
                        stop { literal(0.0); rgba(0.0, 255.0, 0.0, 1.0) }     // green
                        stop { literal(0.5); rgba(255.0, 255.0, 0.0, 1.0) }   // yellow
                        stop { literal(1.0); rgba(255.0, 0.0, 0.0, 1.0) }     // red
                    }
                )
                circleStrokeColor("white")
                circleStrokeWidth(1.5)
            }
        )

        // 🟢 Step 4: Add text labels
        style.addLayer(
            symbolLayer("crowd-labels-layer", "crowd-points-source") {
                textField(Expression.get("name"))
                textSize(12.0)
                textOffset(listOf(0.0, 1.5)) // position label above point
                textColor("black")
                textHaloColor("white")
                textHaloWidth(1.5)
            }
        )
    }

    data class Place(
        val name: String,
        val lng: Double,
        val lat: Double,
        val crowdLevel: Double // 0.0 = green, 1.0 = red
    )

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
