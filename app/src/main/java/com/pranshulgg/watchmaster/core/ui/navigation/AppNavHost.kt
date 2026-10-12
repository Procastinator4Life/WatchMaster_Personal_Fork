package com.pranshulgg.watchmaster.core.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pranshulgg.watchmaster.R
import com.pranshulgg.watchmaster.core.ui.components.Symbol
import com.pranshulgg.watchmaster.feature.lists.MovieListsScreen
import com.pranshulgg.watchmaster.feature.lists.listEntry.ListEntryScreen
import com.pranshulgg.watchmaster.feature.lists.view.ViewListScreen
import com.pranshulgg.watchmaster.feature.main.MainScreen
import com.pranshulgg.watchmaster.feature.main.MainScreenNavViewModel
import com.pranshulgg.watchmaster.feature.movie.detail.MovieDetailPage
import com.pranshulgg.watchmaster.feature.person.PersonScreen
import com.pranshulgg.watchmaster.feature.search.SearchScreen
import com.pranshulgg.watchmaster.feature.search.SearchType
import com.pranshulgg.watchmaster.feature.setting.SettingsScreen
import com.pranshulgg.watchmaster.feature.tv.detail.TvDetailsScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.layout.statusBars

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    motionScheme: MotionScheme,
) {
    val viewModel: MainScreenNavViewModel = viewModel()

    val selectedItem = viewModel.selectedItem

    val configuration = LocalConfiguration.current

    val isTabletLandscape =
        configuration.smallestScreenWidthDp >= 600 &&
        configuration.screenWidthDp > configuration.screenHeightDp

    Box(
        Modifier.fillMaxSize()
    ) {
        SnackbarHost(
            hostState = snackbarHostState,
            Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = WindowInsets.navigationBars
                        .asPaddingValues()
                        .calculateBottomPadding()
                )
        )

        if (isTabletLandscape) {
            PermanentNavigationDrawer(
                drawerContent = {
                    MainNavigationDrawerContent(
                        selectedItem = selectedItem,
                        onItemSelected = { index ->
                            viewModel.selectedItem = index
                        },
                        navController = navController
                    )
                }
            ) {
                AppNavHostContent(
                    navController = navController,
                    viewModel = viewModel,
                    isTabletLandscape = isTabletLandscape
                )
            }
        } else {
            AppNavHostContent(
                navController = navController,
                viewModel = viewModel,
                isTabletLandscape = isTabletLandscape
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppNavHostContent(
    navController: NavHostController,
    viewModel: MainScreenNavViewModel,
    isTabletLandscape: Boolean,
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.MAIN,
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer),
        enterTransition = { NavTransitions.enter() },
        exitTransition = { NavTransitions.exit() },
        popEnterTransition = { NavTransitions.popEnter() },
        popExitTransition = { NavTransitions.popExit() }
    ) {
        composable(
            NavRoutes.MAIN
        ) {
            MainScreen(
                navController = navController,
                viewModel = viewModel,
                isTablet = isTabletLandscape
            )
        }

        composable(
            NavRoutes.SETTINGS
        ) {
            SettingsScreen(navController)
        }

        composable(
            route = "${NavRoutes.MOVIE_DETAIL_SCREEN}/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            ),
            enterTransition = { NavTransitions.detailEnter() },
            exitTransition = { NavTransitions.detailExit() },
            popEnterTransition = { NavTransitions.detailPopEnter() },
            popExitTransition = { NavTransitions.detailPopExit() }
        ) { backStackEntry ->
            val id = backStackEntry.arguments!!.getLong("id")

            MovieDetailPage(
                id = id,
                navController
            )
        }

        composable(
            route = "${NavRoutes.SEARCH}?searchType={searchType}",
            arguments = listOf(
                navArgument("searchType") {
                    type = NavType.StringType
                    defaultValue = SearchType.MOVIE.name
                }
            )
        ) { backStackEntry ->

            val searchType = backStackEntry.arguments
                ?.getString("searchType")
                ?.let { SearchType.valueOf(it) }
                ?: SearchType.MULTI

            SearchScreen(
                navController = navController,
                searchType = searchType
            )
        }

        composable(
            route = "${NavRoutes.TV_DETAIL_SCREEN}/{id}/{seasonNumber}?episodeNumber={episodeNumber}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                },
                navArgument("seasonNumber") {
                    type = NavType.IntType
                },
                navArgument("episodeNumber") {
                    type = NavType.IntType
                    defaultValue = -1
                }
            ),
            enterTransition = { NavTransitions.detailEnter() },
            exitTransition = { NavTransitions.detailExit() },
            popEnterTransition = { NavTransitions.detailPopEnter() },
            popExitTransition = { NavTransitions.detailPopExit() }
        ) { backStackEntry ->

            val id = backStackEntry.arguments!!.getLong("id")
            val seasonNumber =
                backStackEntry.arguments!!.getInt("seasonNumber")

            val episodeNumber =
                backStackEntry.arguments
                    ?.getInt("episodeNumber")
                    ?.takeIf { it > 0 }

            TvDetailsScreen(
                id = id,
                seasonNumber = seasonNumber,
                targetEpisodeNumber = episodeNumber,
                navController = navController
            )
        }

        composable(
            NavRoutes.LISTS_SCREEN
        ) {
            MovieListsScreen(navController)
        }

        composable(
            route = "${NavRoutes.LISTS_ENTRY_SCREEN}/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->

            val id =
                backStackEntry.arguments?.getLong("id") ?: -1L

            ListEntryScreen(
                id,
                navController
            )
        }

        composable(
            route = "${NavRoutes.LISTS_VIEW_SCREEN}/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->

            val id =
                backStackEntry.arguments!!.getLong("id")

            ViewListScreen(
                navController,
                id
            )
        }

        composable(
            route = "${NavRoutes.PERSON_SCREEN}/{id}",
            arguments = listOf(
                navArgument("id") {
                    type = NavType.LongType
                }
            ),
            enterTransition = { NavTransitions.detailEnter() },
            exitTransition = { NavTransitions.detailExit() },
            popEnterTransition = { NavTransitions.detailPopEnter() },
            popExitTransition = { NavTransitions.detailPopExit() }
        ) { backStackEntry ->

            val id =
                backStackEntry.arguments!!.getLong("id")

            PersonScreen(
                id = id,
                navController
            )
        }
    }
}

@Composable
private fun MainNavigationDrawerContent(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit,
    navController: NavHostController,
) {
    val labels = listOf(
        "Home",
        "Movies",
        "TV series"
    )

    val unselectedIcons = listOf(
        R.drawable.home_24px,
        R.drawable.movie_24px,
        R.drawable.tv_24px
    )

    val selectedIcons = listOf(
        R.drawable.home_filled_24px,
        R.drawable.movie_filled_24px,
        R.drawable.tv_filled_24px
    )

    val currentRoute = navController
        .currentBackStackEntryAsState().value?.destination?.route

    val onListsPage =
        currentRoute?.startsWith(NavRoutes.LISTS_SCREEN) == true ||
        currentRoute?.startsWith(NavRoutes.LISTS_ENTRY_SCREEN) == true ||
        currentRoute?.startsWith(NavRoutes.LISTS_VIEW_SCREEN) == true

    val onSettingsPage = currentRoute == NavRoutes.SETTINGS

    val onUtilityPage = onListsPage || onSettingsPage

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                )
        ) {

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                labels.forEachIndexed { index, label ->

                    val selected = selectedItem == index && !onUtilityPage

                    NavigationDrawerItem(
                        label = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )

                                if (selected && index != 0) {
                                    Spacer(
                                        modifier = Modifier.weight(1f)
                                    )

                                    FilledIconButton(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .offset(x = 15.dp),
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor =
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        onClick = {
                                            navController.navigateSafely(
                                                NavRoutes.search(
                                                    if (index == 1) {
                                                        SearchType.MOVIE
                                                    } else {
                                                        SearchType.TV
                                                    }
                                                )
                                            )
                                        }
                                    ) {
                                        Symbol(
                                            R.drawable.add_24px,
                                            color =
                                                MaterialTheme.colorScheme.primaryContainer
                                        )
                                    }
                                }
                            }
                        },
                        selected = selected,
                        onClick = {
                            onItemSelected(index)
                            val current = navController.currentBackStackEntry?.destination?.route
                            if (current != NavRoutes.MAIN) {
                                navController.navigate(NavRoutes.MAIN) {
                                    popUpTo(NavRoutes.MAIN) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    if (selected) {
                                        selectedIcons[index]
                                    } else {
                                        unselectedIcons[index]
                                    }
                                ),
                                contentDescription = label,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor =
                                MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor =
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor =
                                MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor =
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50.dp)
                    )
                }
            }

            Spacer(
                modifier = Modifier.weight(1f)
            )

            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 12.dp
                )
            )

            NavigationDrawerItem(
                label = {
                    Text("Lists")
                },
                selected = onListsPage,
                onClick = {
                    navController.navigateFromSidebar(NavRoutes.LISTS_SCREEN)
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            R.drawable.lists_24px
                        ),
                        contentDescription = "Lists"
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor =
                        MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor =
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor =
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor =
                        MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp)
            )

            NavigationDrawerItem(
                label = {
                    Text("Settings")
                },
                selected = onSettingsPage,
                onClick = {
                    navController.navigateFromSidebar(NavRoutes.SETTINGS)
                },
                icon = {
                    Icon(
                        painter = painterResource(
                            R.drawable.settings_24px
                        ),
                        contentDescription = "Settings"
                    )
                },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor =
                        MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor =
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor =
                        MaterialTheme.colorScheme.onPrimaryContainer,
                    unselectedIconColor =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor =
                        MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp)
            )
        }
    }
}

private fun NavHostController.navigateFromSidebar(route: String) {
    val current = currentBackStackEntry?.destination?.route
    if (current == route) return

    navigate(route) {
        popUpTo(NavRoutes.MAIN) { inclusive = false }
        launchSingleTop = true
    }
}