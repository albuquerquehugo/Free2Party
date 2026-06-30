package com.free2party.ui.screens.profile

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Domain
import androidx.compose.material.icons.filled.DownhillSkiing
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GolfCourse
import androidx.compose.material.icons.filled.Hiking
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.Sailing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Skateboarding
import androidx.compose.material.icons.filled.Snowboarding
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sports
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SportsFootball
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.SportsHockey
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsRugby
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Surfing
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.free2party.R
import com.free2party.ui.components.TopBar
import kotlinx.coroutines.flow.collectLatest

data class InterestCategory(
    val id: String,
    val icon: Any, // ImageVector or DrawableRes Int
    val labelResId: Int
)

val InterestCategories = listOf(
    InterestCategory(
        "ADRENALINE_SPORTS",
        Icons.Default.FlashOn,
        R.string.interest_adrenaline_sports
    ),
    InterestCategory(
        "AMERICAN_FOOTBALL",
        Icons.Default.SportsFootball,
        R.string.interest_american_football
    ),
    InterestCategory("ANIMALS", Icons.Default.Pets, R.string.interest_animals),
    InterestCategory("ANIME", Icons.Default.Tv, R.string.interest_anime),
    InterestCategory("ARCHERY", R.drawable.outline_target_24, R.string.interest_archery),
    InterestCategory("ARCHITECTURE", Icons.Default.Domain, R.string.interest_architecture),
    InterestCategory("ART", Icons.Default.Palette, R.string.interest_art),
    InterestCategory("ARTISANAL_CRAFTS", Icons.Default.Brush, R.string.interest_artisanal_crafts),
    InterestCategory("AVIATION", Icons.Default.Flight, R.string.interest_aviation),
    InterestCategory("BADMINTON", R.drawable.outline_badminton_24, R.string.interest_badminton),
    InterestCategory("BASEBALL", Icons.Default.SportsBaseball, R.string.interest_baseball),
    InterestCategory("BASKETBALL", Icons.Default.SportsBasketball, R.string.interest_basketball),
    InterestCategory("BASQUE_PELOTA", Icons.Default.SportsCricket, R.string.interest_basque_pelota),
    InterestCategory("BILLIARDS", Icons.Default.Circle, R.string.interest_billiards),
    InterestCategory("BOARD_GAMES", Icons.Default.Casino, R.string.interest_board_games),
    InterestCategory("BOBSLEDDING", R.drawable.outline_sledding_24, R.string.interest_bobsledding),
    InterestCategory("BOCCE_BALL", Icons.Default.Circle, R.string.interest_bocce_ball),
    InterestCategory("BOWLING", Icons.Default.Circle, R.string.interest_bowling),
    InterestCategory("BOXING", Icons.Default.SportsMma, R.string.interest_boxing),
    InterestCategory("BRIDGE", Icons.Default.Style, R.string.interest_bridge),
    InterestCategory("BUILDING_THINGS", Icons.Default.Build, R.string.interest_building_things),
    InterestCategory("CAMPING", R.drawable.outline_camping_24, R.string.interest_camping),
    InterestCategory("CANOEING", R.drawable.outline_kayaking_24, R.string.interest_canoeing),
    InterestCategory("CARD_GAMES", Icons.Default.Style, R.string.interest_card_games),
    InterestCategory("CARS", Icons.Default.DirectionsCar, R.string.interest_cars),
    InterestCategory("CHARRERIA", R.drawable.outline_chess_knight_24, R.string.interest_charreria),
    InterestCategory("CHEERLEADING", R.drawable.outline_cheer_24, R.string.interest_cheerleading),
    InterestCategory("CHESS", R.drawable.outline_chess_24, R.string.interest_chess),
    InterestCategory("CLIMBING", Icons.Default.Terrain, R.string.interest_climbing),
    InterestCategory("COCKTAILS", Icons.Default.LocalBar, R.string.interest_cocktails),
    InterestCategory("COFFEE", Icons.Default.LocalCafe, R.string.interest_coffee),
    InterestCategory("COMEDY", Icons.Default.TheaterComedy, R.string.interest_comedy),
    InterestCategory(
        "CONTENT_CREATION",
        Icons.Default.Videocam,
        R.string.interest_content_creation
    ),
    InterestCategory("COOKING", Icons.Default.Restaurant, R.string.interest_cooking),
    InterestCategory("CRAFTING", Icons.Default.Brush, R.string.interest_crafting),
    InterestCategory("CRICKET", Icons.Default.SportsCricket, R.string.interest_cricket),
    InterestCategory(
        "CULTURAL_HERITAGE",
        Icons.Default.Museum,
        R.string.interest_cultural_heritage
    ),
    InterestCategory("CURLING", R.drawable.outline_mop_24, R.string.interest_curling),
    InterestCategory(
        "CYCLING",
        Icons.AutoMirrored.Filled.DirectionsBike,
        R.string.interest_cycling
    ),
    InterestCategory("DANCE", Icons.Default.MusicNote, R.string.interest_dance),
    InterestCategory("DARTS", R.drawable.outline_target_24, R.string.interest_darts),
    InterestCategory("DESIGN", Icons.Default.DesignServices, R.string.interest_design),
    InterestCategory("DIVING", R.drawable.outline_scuba_diving_24, R.string.interest_diving),
    InterestCategory("DODGEBALL", Icons.Default.Circle, R.string.interest_dodgeball),
    InterestCategory(
        "EQUESTRIAN_SPORTS",
        R.drawable.outline_chess_knight_24,
        R.string.interest_equestrian_sports
    ),
    InterestCategory(
        "FANTASY_SPORTS",
        Icons.Default.SportsEsports,
        R.string.interest_fantasy_sports
    ),
    InterestCategory("FASHION", Icons.Default.Checkroom, R.string.interest_fashion),
    InterestCategory("FENCING", R.drawable.outline_swords_24, R.string.interest_fencing),
    InterestCategory("FIELD_HOCKEY", Icons.Default.SportsHockey, R.string.interest_field_hockey),
    InterestCategory(
        "FIGURE_SKATING",
        R.drawable.outline_ice_skating_24,
        R.string.interest_figure_skating
    ),
    InterestCategory("FISHING", Icons.Default.Phishing, R.string.interest_fishing),
    InterestCategory("FITNESS", Icons.Default.FitnessCenter, R.string.interest_fitness),
    InterestCategory("FOODIE", Icons.Default.Restaurant, R.string.interest_foodie),
    InterestCategory(
        "FOOTBALL/SOCCER",
        Icons.Default.SportsSoccer,
        R.string.interest_football_soccer
    ),
    InterestCategory("GARDENING", Icons.Default.Yard, R.string.interest_gardening),
    InterestCategory("GOLF", Icons.Default.GolfCourse, R.string.interest_golf),
    InterestCategory(
        "GYMNASTICS",
        R.drawable.outline_sports_gymnastics_24,
        R.string.interest_gymnastics
    ),
    InterestCategory("HAIR", R.drawable.outline_health_and_beauty_24, R.string.interest_hair),
    InterestCategory("HANDBALL", Icons.Default.SportsHandball, R.string.interest_handball),
    InterestCategory("HIKING", Icons.Default.Hiking, R.string.interest_hiking),
    InterestCategory("HISTORY", Icons.Default.HistoryEdu, R.string.interest_history),
    InterestCategory("HOCKEY", Icons.Default.SportsHockey, R.string.interest_hockey),
    InterestCategory(
        "HOME_IMPROVEMENTS",
        Icons.Default.HomeWork,
        R.string.interest_home_improvements
    ),
    InterestCategory(
        "HORSE_RACING",
        R.drawable.outline_chess_knight_24,
        R.string.interest_horse_racing
    ),
    InterestCategory("JUDO", R.drawable.outline_sports_kabaddi_24, R.string.interest_judo),
    InterestCategory("KARATE", R.drawable.outline_sports_martial_arts_24, R.string.interest_karate),
    InterestCategory("KAYAKING", R.drawable.outline_kayaking_24, R.string.interest_kayaking),
    InterestCategory(
        "KICKBOXING",
        R.drawable.outline_sports_martial_arts_24,
        R.string.interest_kickboxing
    ),
    InterestCategory(
        "KUNG_FU",
        R.drawable.outline_sports_martial_arts_24,
        R.string.interest_kung_fu
    ),
    InterestCategory("LACROSSE", Icons.Default.SportsCricket, R.string.interest_lacrosse),
    InterestCategory("LIVE_MUSIC", Icons.Default.MusicNote, R.string.interest_live_music),
    InterestCategory("LIVE_SPORTS", Icons.Default.LiveTv, R.string.interest_live_sports),
    InterestCategory("LOCAL_CULTURE", Icons.Default.Public, R.string.interest_local_culture),
    InterestCategory("LUGE", R.drawable.outline_sledding_24, R.string.interest_luge),
    InterestCategory("MAKEUP", Icons.Default.Face, R.string.interest_makeup),
    InterestCategory("MEDITATION", Icons.Default.SelfImprovement, R.string.interest_meditation),
    InterestCategory("MOTOR_SPORTS", Icons.Default.DirectionsCar, R.string.interest_motor_sports),
    InterestCategory("MOVIES", Icons.Default.Movie, R.string.interest_movies),
    InterestCategory("MUSEUMS", Icons.Default.Museum, R.string.interest_museums),
    InterestCategory("NETBALL", Icons.Default.SportsBasketball, R.string.interest_netball),
    InterestCategory("NIGHTLIFE", Icons.Default.Celebration, R.string.interest_nightlife),
    InterestCategory("OUTDOORS", R.drawable.outline_nature_people_24, R.string.interest_outdoors),
    InterestCategory("PADEL", R.drawable.outline_padel_24, R.string.interest_padel),
    InterestCategory("PENTATHLON", R.drawable.outline_forward_5_24, R.string.interest_pentathlon),
    InterestCategory("PHOTOGRAPHY", Icons.Default.PhotoCamera, R.string.interest_photography),
    InterestCategory("PICKLEBALL", R.drawable.outline_pickleball_24, R.string.interest_pickleball),
    InterestCategory("PLANTS", Icons.Default.LocalFlorist, R.string.interest_plants),
    InterestCategory("PLAYING_MUSIC", Icons.Default.LibraryMusic, R.string.interest_playing_music),
    InterestCategory("PODCASTS", Icons.Default.Podcasts, R.string.interest_podcasts),
    InterestCategory("POKER", Icons.Default.Style, R.string.interest_poker),
    InterestCategory("POLO", R.drawable.outline_chess_knight_24, R.string.interest_polo),
    InterestCategory("PUZZLES", Icons.Default.Extension, R.string.interest_puzzles),
    InterestCategory("RACQUETBALL", Icons.Default.SportsTennis, R.string.interest_racquetball),
    InterestCategory("READING", Icons.Default.Book, R.string.interest_reading),
    InterestCategory("RODEO", R.drawable.outline_chess_knight_24, R.string.interest_rodeo),
    InterestCategory(
        "ROLLER_DERBY",
        R.drawable.outline_roller_skating_24,
        R.string.interest_roller_derby
    ),
    InterestCategory(
        "ROLLER_SKATING",
        R.drawable.outline_roller_skating_24,
        R.string.interest_roller_skating
    ),
    InterestCategory("ROWING", Icons.Default.Rowing, R.string.interest_rowing),
    InterestCategory("RUGBY", Icons.Default.SportsRugby, R.string.interest_rugby),
    InterestCategory("RUNNING", Icons.AutoMirrored.Filled.DirectionsRun, R.string.interest_running),
    InterestCategory("SAILING", Icons.Default.Sailing, R.string.interest_sailing),
    InterestCategory("SELF_CARE", Icons.Default.Spa, R.string.interest_self_care),
    InterestCategory(
        "SHOOTING_SPORTS",
        R.drawable.outline_target_24,
        R.string.interest_shooting_sports
    ),
    InterestCategory("SHOPPING", Icons.Default.ShoppingBag, R.string.interest_shopping),
    InterestCategory("SINGING", Icons.Default.Mic, R.string.interest_singing),
    InterestCategory("SKATEBOARDING", Icons.Default.Skateboarding, R.string.interest_skateboarding),
    InterestCategory("SKIING", Icons.Default.DownhillSkiing, R.string.interest_skiing),
    InterestCategory(
        "SNORKELING",
        R.drawable.outline_scuba_diving_24,
        R.string.interest_snorkeling
    ),
    InterestCategory("SNOWBOARDING", Icons.Default.Snowboarding, R.string.interest_snowboarding),
    InterestCategory(
        "SOCIAL_ACTIVISM",
        Icons.Default.VolunteerActivism,
        R.string.interest_social_activism
    ),
    InterestCategory("SPA", Icons.Default.Spa, R.string.interest_spa),
    InterestCategory("SQUASH", Icons.Default.SportsTennis, R.string.interest_squash),
    InterestCategory(
        "SUMO_WRESTLING",
        R.drawable.outline_sports_kabaddi_24,
        R.string.interest_sumo_wrestling
    ),
    InterestCategory(
        "SURFING",
        Icons.Default.Surfing,
        R.string.interest_surfing
    ), // Surfing icon was missing or can use Skiing/Water, let's use Water
    InterestCategory("SUSTAINABILITY", Icons.Default.Eco, R.string.interest_sustainability),
    InterestCategory("SWIMMING", Icons.Default.Pool, R.string.interest_swimming),
    InterestCategory(
        "TABLE_TENNIS",
        R.drawable.outline_table_restaurant_24,
        R.string.interest_table_tennis
    ),
    InterestCategory(
        "TAEKWONDO",
        R.drawable.outline_sports_martial_arts_24,
        R.string.interest_taekwondo
    ),
    InterestCategory("TAI_CHI", R.drawable.outline_sports_gymnastics_24, R.string.interest_tai_chi),
    InterestCategory("TECHNOLOGY", Icons.Default.Computer, R.string.interest_technology),
    InterestCategory("TENNIS", Icons.Default.SportsTennis, R.string.interest_tennis),
    InterestCategory("THEATER", Icons.Default.TheaterComedy, R.string.interest_theater),
    InterestCategory(
        "TRACK_AND_FIELD",
        Icons.AutoMirrored.Filled.DirectionsRun,
        R.string.interest_track_and_field
    ),
    InterestCategory("TRAVEL", Icons.Default.TravelExplore, R.string.interest_travel),
    InterestCategory(
        "ULTIMATE_FRISBEE",
        R.drawable.outline_circle_circle_24,
        R.string.interest_ultimate_frisbee
    ),
    InterestCategory("VIDEO_GAMES", Icons.Default.SportsEsports, R.string.interest_video_games),
    InterestCategory("VOLLEYBALL", Icons.Default.SportsVolleyball, R.string.interest_volleyball),
    InterestCategory(
        "VOLUNTEERING",
        Icons.Default.VolunteerActivism,
        R.string.interest_volunteering
    ),
    InterestCategory(
        "WALKING",
        Icons.AutoMirrored.Filled.DirectionsWalk,
        R.string.interest_walking
    ),
    InterestCategory("WATCHING_TV", Icons.Default.Tv, R.string.interest_watching_tv),
    InterestCategory("WATER_POLO", Icons.Default.Pool, R.string.interest_water_polo),
    InterestCategory(
        "WATER_SPORTS",
        R.drawable.outline_kitesurfing_24,
        R.string.interest_water_sports
    ),
    InterestCategory(
        "WEIGHT_LIFTING",
        Icons.Default.FitnessCenter,
        R.string.interest_weight_lifting
    ),
    InterestCategory("WINE_TASTING", Icons.Default.LocalBar, R.string.interest_wine_tasting),
    InterestCategory(
        "WRESTLING",
        R.drawable.outline_sports_kabaddi_24,
        R.string.interest_wrestling
    ),
    InterestCategory("WRITING", Icons.Default.Edit, R.string.interest_writing),
    InterestCategory("YOGA", Icons.Default.SelfImprovement, R.string.interest_yoga)
)

@Composable
fun InterestsRoute(
    onBack: () -> Unit,
    viewModel: InterestsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collectLatest { event ->
            when (event) {
                is InterestsUiEvent.ShowToast -> {
                    Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT)
                        .show()
                    if (event.navigateBack) {
                        onBack()
                    }
                }
            }
        }
    }

    InterestsScreen(
        uiState = viewModel.uiState,
        gradientBackground = viewModel.gradientBackground,
        selectedInterests = viewModel.selectedInterests,
        hasChanges = viewModel.hasChanges,
        onBack = onBack,
        onInterestToggle = { viewModel.toggleInterest(it) },
        onDiscard = { viewModel.discardChanges() },
        onSave = { viewModel.saveInterests() }
    )
}

@Composable
fun InterestsScreen(
    uiState: InterestsUiState,
    gradientBackground: Boolean,
    selectedInterests: Set<String>,
    hasChanges: Boolean,
    onBack: () -> Unit,
    onInterestToggle: (String) -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        containerColor = if (gradientBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = stringResource(R.string.label_interests),
                color = MaterialTheme.colorScheme.onSurface,
                onBack = onBack,
                enabled = uiState !is InterestsUiState.Loading
            )
        }
    ) { paddingValues ->
        when (uiState) {
            is InterestsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is InterestsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = uiState.message.asString(), color = MaterialTheme.colorScheme.error)
                }
            }

            is InterestsUiState.Success -> {
                val sortedCategories = remember(selectedInterests) {
                    InterestCategories.sortedByDescending { selectedInterests.contains(it.id) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = paddingValues.calculateTopPadding())
                        .consumeWindowInsets(paddingValues)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 110.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sortedCategories, key = { it.id }) { category ->
                            val isSelected = selectedInterests.contains(category.id)
                            InterestCard(
                                category = category,
                                isSelected = isSelected,
                                onClick = { onInterestToggle(category.id) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (hasChanges) {
                            TextButton(
                                onClick = onDiscard,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .testTag("discard_button"),
                                enabled = !uiState.isSaving
                            ) {
                                Text(
                                    text = stringResource(R.string.label_discard_changes),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = onSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = hasChanges && !uiState.isSaving
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.label_save_changes),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InterestCard(
    category: InterestCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.03f else 1.0f,
        label = "InterestCardScale"
    )
    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        label = "InterestCardBackgroundColor"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "InterestCardContentColor"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "InterestCardBorderColor"
    )

    Box(
        modifier = Modifier
            .scale(scale)
            .aspectRatio(1.5f) // Slightly wider aspect ratio for long names
            .clip(RoundedCornerShape(16.dp))
            .background(cardBgColor)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = when (val icon = category.icon) {
                    is ImageVector -> rememberVectorPainter(icon)
                    is Int -> painterResource(id = icon)
                    else -> rememberVectorPainter(Icons.Default.Sports)
                },
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(category.labelResId),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
