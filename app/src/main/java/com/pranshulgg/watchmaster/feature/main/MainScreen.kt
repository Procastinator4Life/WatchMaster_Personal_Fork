package com.pranshulgg.watchmaster.feature.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection.Companion.Bottom
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.layout.widthIn
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.TooltipIconBtn
import com.pranshulgg.watchmaster.core.ui.navigation.NavRoutes
import com.pranshulgg.watchmaster.core.ui.navigation.navigateSafely
import com.pranshulgg.watchmaster.feature.home.HomeScreen
import com.pranshulgg.watchmaster.feature.main.components.MainFloatingToolbar
import com.pranshulgg.watchmaster.feature.movie.MovieHomeScreen
import com.pranshulgg.watchmaster.feature.tv.TvHomeScreen
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.unit.dp
import com.pranshulgg.watchmaster.core.ui.components.Symbol
import com.pranshulgg.watchmaster.feature.search.SearchType
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.activity.compose.BackHandler

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun MainScreen(
    navController: NavController,
    viewModel: MainScreenNavViewModel,
    isTablet: Boolean
) {

    val selectedItem = viewModel.selectedItem

    MainScreenContent(
        selectedItem = selectedItem,
        onItemSelected = { index ->
            viewModel.selectedItem = index
        },
        navController = navController,
        isTablet = isTablet
    )
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
private fun MainScreenContent(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    navController: NavController,
    isTablet: Boolean,
) {
    val appBarTitles = listOf(
        "Home",
        "Movies",
        "TV series"
    )

    val scrollBehavior =
        FloatingToolbarDefaults.exitAlwaysScrollBehavior(
            exitDirection = Bottom
        )

    val scrollBehaviorTopBar =
        if (isTablet) TopAppBarDefaults.pinnedScrollBehavior()
        else TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Reset the top bar whenever the selected tab changes.
    LaunchedEffect(selectedItem) {
        scrollBehaviorTopBar.state.heightOffset = 0f
        scrollBehaviorTopBar.state.contentOffset = 0f
        scrollBehavior.state.offset = 0f
    }

    BackHandler(enabled = selectedItem != 0) {
        onItemSelected(0)
    }
   
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            if (!isTablet) {
                TopAppBar(
                    scrollBehavior = scrollBehaviorTopBar,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    title = {
                        Text(
                            text = appBarTitles[selectedItem]
                        )
                    },
                    actions = {
                        TooltipIconBtn(
                            onClick = {
                                navController.navigateSafely(
                                    NavRoutes.LISTS_SCREEN
                                )
                            },
                            icon = R.drawable.lists_24px,
                            tooltipText = "Lists"
                        )

                        TooltipIconBtn(
                            onClick = {
                                navController.navigateSafely(
                                    NavRoutes.SETTINGS
                                )
                            },
                            icon = R.drawable.settings_24px,
                            tooltipText = "Settings"
                        )
                    }
                )
            }
        },
        bottomBar = {
            if (!isTablet) {
                MainFloatingToolbar(
                    selectedItem = selectedItem,
                    onItemSelected = onItemSelected,
                    navController = navController,
                    scrollBehavior = scrollBehavior
                )
            }
        }
    ) { innerPadding ->

        val tabletTopGap = innerPadding.calculateTopPadding() + 16.dp

        Box(
            modifier = Modifier
                .padding(
                    top = if (isTablet) {
                        if (selectedItem == 0) 0.dp else tabletTopGap
                    } else {
                        innerPadding.calculateTopPadding()
                    }
                )
                .fillMaxSize()
        ) {
            when (selectedItem) {
                0 -> HomeScreen(
                    navController,
                    topPadding = if (isTablet) tabletTopGap else 0.dp,
                    scrollBehavior = scrollBehavior,
                    scrollBehaviorTopBar = scrollBehaviorTopBar
                )
                1 -> MovieHomeScreen(
                    navController,
                    scrollBehavior,
                    scrollBehaviorTopBar
                )
                2 -> TvHomeScreen(
                    navController,
                    scrollBehavior,
                    scrollBehaviorTopBar
                )
            }
        }
    }
}