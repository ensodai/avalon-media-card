package org.ensodai.avalonmediacard.presentation.screens.person

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import avalonmediacard.client.generated.resources.*
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.User
import org.jetbrains.compose.resources.stringResource
import org.ensodai.avalonmediacard.contract.slot.Action
import org.ensodai.avalonmediacard.contract.slot.SlotData
import org.ensodai.avalonmediacard.presentation.components.MovieCarousel
import org.ensodai.avalonmediacard.presentation.components.ShimmerImage
import org.ensodai.avalonmediacard.presentation.components.shimmerPlaceholder
import org.ensodai.avalonmediacard.presentation.core.SduiSlot
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.LocalDeviceTarget
import org.ensodai.avalonmediacard.presentation.screens.commonComponents.tvAndWebHoverEffect

@Composable
fun PersonContent(
    header: SduiSlot<SlotData.Header>?,
    bio: SduiSlot<SlotData.Text>?,
    credits: List<SduiSlot<SlotData.Carousel>>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceTarget = LocalDeviceTarget.current
    val isMobile = !deviceTarget.isDesktop && !deviceTarget.isTablet && !deviceTarget.isTv
    val scrollState = rememberScrollState()

    val headerState = header?.state
    val headerData = headerState?.data
    val isHeaderLoading = headerState?.isLoading == true

    val bioState = bio?.state
    val bioData = bioState?.data
    val isBioLoading = bioState?.isLoading == true

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Центральный контейнер с ограничением ширины
            Column(
                modifier = Modifier
                    .widthIn(max = 1320.dp)
                    .fillMaxWidth()
                    .padding(horizontal = if (isMobile) 16.dp else 40.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // 1. Шапка персоны (Hero Profile)
                if (isMobile) {
                    PersonHeaderMobile(
                        headerData = headerData,
                        isLoading = isHeaderLoading
                    )
                } else {
                    PersonHeaderDesktop(
                        headerData = headerData,
                        isLoading = isHeaderLoading
                    )
                }

                // 2. Секция Биографии
                PersonBioSection(
                    bioData = bioData,
                    isLoading = isBioLoading
                )

                // 3. Секция Фильмографии (Карусели проектов)
                if (credits.isNotEmpty()) {
                    PersonCreditsSection(
                        credits = credits,
                        onAction = onAction
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonHeaderDesktop(
    headerData: SlotData.Header?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val posterUrl = headerData?.posterUrl
    val title = headerData?.title ?: ""
    val department = headerData?.subtitle
    val tagline = headerData?.tagline

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(36.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Портрет персоны с пропорцией 2:3
        Box(
            modifier = Modifier
                .width(220.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .shimmerPlaceholder(isLoading, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!isLoading) {
                ShimmerImage(
                    model = posterUrl?.takeIf { it.isNotBlank() },
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(16.dp),
                    errorIcon = Lucide.User,
                    errorIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Информационный блок
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Имя
            Crossfade(
                targetState = isLoading,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) { loading ->
                if (loading) {
                    Box(
                        modifier = Modifier
                            .width(360.dp)
                            .height(44.dp)
                            .shimmerPlaceholder(true, RoundedCornerShape(8.dp))
                    )
                } else {
                    Text(
                        text = title.ifBlank { stringResource(Res.string.person_unknown) },
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        lineHeight = 44.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Амплуа / Департамент
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(28.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(6.dp))
                )
            } else if (!department.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                ) {
                    Text(
                        text = department,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Метаданные (Дата и место рождения)
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .width(260.dp)
                        .height(20.dp)
                        .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                )
            } else if (!tagline.isNullOrBlank()) {
                Text(
                    text = tagline,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@Composable
private fun PersonHeaderMobile(
    headerData: SlotData.Header?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val posterUrl = headerData?.posterUrl
    val title = headerData?.title ?: ""
    val department = headerData?.subtitle
    val tagline = headerData?.tagline

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Портрет персоны
        Box(
            modifier = Modifier
                .width(160.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                .shimmerPlaceholder(isLoading, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!isLoading) {
                ShimmerImage(
                    model = posterUrl?.takeIf { it.isNotBlank() },
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(14.dp),
                    errorIcon = Lucide.User,
                    errorIconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        // Имя
        if (isLoading) {
            Box(
                modifier = Modifier
                    .width(220.dp)
                    .height(30.dp)
                    .shimmerPlaceholder(true, RoundedCornerShape(6.dp))
            )
        } else {
            Text(
                text = title.ifBlank { stringResource(Res.string.person_unknown) },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 30.sp
            )
        }

        // Амплуа
        if (!isLoading && !department.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = department,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // Метаданные (Дата/место)
        if (!isLoading && !tagline.isNullOrBlank()) {
            Text(
                text = tagline,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PersonBioSection(
    bioData: SlotData.Text?,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val rawText = bioData?.content
    if (!isLoading && rawText.isNullOrBlank()) return

    val cleanedText = remember(rawText) {
        rawText?.replace(Regex("(@Википедия|@Wikipedia).*$", RegexOption.IGNORE_CASE), "")?.trim() ?: ""
    }

    if (!isLoading && cleanedText.isBlank()) return

    var isExpanded by remember { mutableStateOf(false) }
    var hasVisualOverflow by remember { mutableStateOf(false) }
    var isHovered by remember { mutableStateOf(false) }

    val textColor by animateColorAsState(
        targetValue = if (isHovered) Color.White else Color.White.copy(alpha = 0.78f)
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Cineby-стилистика: акцентный заголовок секции
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(Color.White, RoundedCornerShape(2.dp))
            )
            Text(
                text = stringResource(Res.string.person_biography),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Текст биографии со сворачиванием/разворачиванием
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tvAndWebHoverEffect(
                    scaleTarget = 1.0f,
                    activeBorderColor = Color.Transparent,
                    activeBorderWidth = 0.dp,
                    defaultBorderWidth = 0.dp,
                    shape = RoundedCornerShape(8.dp),
                    clickEnabled = hasVisualOverflow || isExpanded,
                    onStateChange = { isHovered = it }
                )
                .clip(RoundedCornerShape(8.dp))
                .then(
                    if (hasVisualOverflow || isExpanded) {
                        Modifier.clickable(enabled = !isLoading) { isExpanded = !isExpanded }
                    } else Modifier
                )
        ) {
            if (isLoading) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(16.dp)
                            .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(16.dp)
                            .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .height(16.dp)
                            .shimmerPlaceholder(true, RoundedCornerShape(4.dp))
                    )
                }
            } else {
                Text(
                    text = cleanedText,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 5,
                    overflow = TextOverflow.Ellipsis,
                    onTextLayout = { textLayoutResult ->
                        if (!isExpanded) {
                            hasVisualOverflow = textLayoutResult.hasVisualOverflow
                        }
                    }
                )

                if (hasVisualOverflow || isExpanded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isExpanded) {
                            stringResource(Res.string.details_desc_collapse)
                        } else {
                            stringResource(Res.string.details_desc_expand)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (isHovered) 1f else 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonCreditsSection(
    credits: List<SduiSlot<SlotData.Carousel>>,
    onAction: (Action) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        credits.forEach { carousel ->
            MovieCarousel(
                state = carousel.state,
                onAction = onAction
            )
        }
    }
}
