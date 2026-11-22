package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.PrayerName
import com.example.ui.components.IslamicPatternBackground
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MuezzinLibraryScreen
import com.example.ui.screens.QiblaScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NoorTheme
import com.example.ui.viewmodel.NoorViewModel
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object QiblaRoute
@Serializable data object MuezzinsRoute
@Serializable data object SettingsRoute

private data class NoorNavTab(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoorTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NoorAppRoot()
                }
            }
        }
    }
}

@Composable
fun NoorAppRoot(viewModel: NoorViewModel = viewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val compassReading by viewModel.compassReading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    val adhanAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomAdhanAudio(uri)
        }
    }

    val iqamahAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importCustomIqamahAudio(uri)
        }
    }

    var pendingPrePrayerAudioPrayer by remember { mutableStateOf<PrayerName?>(null) }
    val prePrayerAudioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val targetPrayer = pendingPrePrayerAudioPrayer
        pendingPrePrayerAudioPrayer = null
        if (uri != null && targetPrayer != null) {
            viewModel.importCustomPrePrayerAudio(targetPrayer, uri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (locationGranted) {
            viewModel.refreshGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        val toRequest = mutableListOf<String>()
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse) {
            toRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            toRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotif = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotif) {
                toRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (toRequest.isNotEmpty()) {
            permissionLauncher.launch(toRequest.toTypedArray())
        } else if (uiState.settings.useGpsLocation) {
            viewModel.refreshGpsLocation()
        }
        viewModel.refreshExactAlarmStatus()
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val tabs = remember {
        listOf(
            NoorNavTab(HomeRoute, "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
            NoorNavTab(QiblaRoute, "القبلة", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_qibla"),
            NoorNavTab(MuezzinsRoute, "المؤذنون", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "tab_muezzins"),
            NoorNavTab(SettingsRoute, "الإعدادات", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = backStackEntry?.destination

    fun navigateToTab(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        IslamicPatternBackground {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar {
                            tabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail {
                            tabs.forEach { tab ->
                                val selected = currentDest?.hasRoute(tab.route::class) == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        NavHost(
                            navController = navController,
                            startDestination = HomeRoute
                        ) {
                            composable<HomeRoute> {
                                HomeScreen(
                                    uiState = uiState,
                                    onRefreshGps = {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            viewModel.refreshGpsLocation()
                                        } else {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    onRefreshWeather = { viewModel.refreshWeather() },
                                    onStopAdhan = { viewModel.stopActiveAdhan() },
                                    onOpenMuezzins = { navigateToTab(MuezzinsRoute) }
                                )
                            }

                            composable<QiblaRoute> {
                                QiblaScreen(
                                    qiblaDegrees = uiState.schedule?.qiblaDegrees ?: 0.0,
                                    cityName = uiState.settings.cityName,
                                    compassReading = compassReading
                                )
                            }

                            composable<MuezzinsRoute> {
                                MuezzinLibraryScreen(
                                    uiState = uiState,
                                    onDownload = { viewModel.downloadMuezzin(it) },
                                    onDelete = { viewModel.deleteMuezzinAudio(it) },
                                    onSelect = { viewModel.selectMuezzin(it) },
                                    onTogglePreview = { viewModel.togglePreviewMuezzin(it) },
                                    onImportCustomAudio = {
                                        adhanAudioPickerLauncher.launch(arrayOf("audio/*"))
                                    },
                                    formatSize = { viewModel.formatFileSize(it) }
                                )
                            }

                            composable<SettingsRoute> {
                                SettingsScreen(
                                    uiState = uiState,
                                    onCalculationMethodChange = { viewModel.updateCalculationMethod(it) },
                                    onMadhabChange = { viewModel.updateMadhab(it) },
                                    onAdhanEnabledChange = { viewModel.setAdhanEnabled(it) },
                                    onPrayerAdhanEnabledChange = { prayer, enabled ->
                                        viewModel.setPrayerAdhanEnabled(prayer, enabled)
                                    },
                                    onIqamahEnabledChange = { viewModel.setIqamahEnabled(it) },
                                    onPrayerIqamahEnabledChange = { prayer, enabled ->
                                        viewModel.setPrayerIqamahEnabled(prayer, enabled)
                                    },
                                    onIqamahSoundModeChange = { viewModel.setIqamahSoundMode(it) },
                                    onPickCustomIqamahAudio = {
                                        iqamahAudioPickerLauncher.launch(arrayOf("audio/*"))
                                    },
                                    onTestIqamahAlert = { viewModel.triggerTestIqamahAlert() },
                                    onPrayerIqamahOffsetChange = { prayer, mins ->
                                        viewModel.setPrayerIqamahOffset(prayer, mins)
                                    },
                                    onPrePrayerEnabledChange = { viewModel.setPrePrayerEnabled(it) },
                                    onPrayerPrePrayerEnabledChange = { prayer, enabled ->
                                        viewModel.setPrayerPrePrayerEnabled(prayer, enabled)
                                    },
                                    onPickPrayerPreAudio = { prayer ->
                                        pendingPrePrayerAudioPrayer = prayer
                                        prePrayerAudioPickerLauncher.launch(arrayOf("audio/*"))
                                    },
                                    onClearPrayerPreAudio = { prayer ->
                                        viewModel.clearCustomPrePrayerAudio(prayer)
                                    },
                                    onTestPrayerPreAudio = { prayer ->
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                        } else {
                                            viewModel.triggerTestPrePrayerForPrayer(prayer)
                                        }
                                    },
                                    onPrePrayerSpeakChange = { viewModel.setPrePrayerSpeakAnnouncement(it) },
                                    onPrePrayerMinutesChange = { viewModel.setPrePrayerMinutes(it) },
                                    onPrePrayerToneChange = { viewModel.setPrePrayerTone(it) },
                                    onTemperatureUnitChange = { viewModel.setTemperatureUnit(it) },
                                    onUseGpsClick = {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            viewModel.refreshGpsLocation()
                                        } else {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                                    Manifest.permission.ACCESS_FINE_LOCATION
                                                )
                                            )
                                        }
                                    },
                                    onSelectManualCity = { city, country ->
                                        viewModel.selectManualCity(city, country)
                                    },
                                    onSearchGlobalCities = { viewModel.searchGlobalCities(it) },
                                    onSelectResolvedLocation = { viewModel.selectResolvedLocation(it) },
                                    onClearGlobalSearch = { viewModel.clearGlobalSearch() },
                                    onOpenMuezzinLibrary = { navigateToTab(MuezzinsRoute) },
                                    onTestAdhan = { viewModel.triggerTestAdhan() },
                                    onTestPrePrayerNotification = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                        } else {
                                            viewModel.triggerTestPrePrayerNotification()
                                        }
                                    },
                                    onRequestExactAlarmPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                                data = Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        }
                                    },
                                    onWidgetAppearanceChange = { key, size, font, showClock, showGreg ->
                                        viewModel.updateWidgetAppearance(key, size, font, showClock, showGreg)
                                    },
                                    onResetWidgetAppearances = { viewModel.resetWidgetAppearances() },
                                    onForceUpdateWidgets = { viewModel.forceUpdateWidgets() },
                                    onShowStatusMessage = { viewModel.showStatusMessage(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
