package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PixiLavender
import com.example.ui.theme.PixiLavenderSoft
import com.example.ui.theme.PixiLightSearch
import com.example.ui.theme.PixiYellow
import com.example.ui.theme.rememberPixiDimens

// Shared shape language from the reference screenshots
val PixiCardShape = RoundedCornerShape(24.dp)
val PixiCardShapeSm = RoundedCornerShape(18.dp)
val PixiPillShape = RoundedCornerShape(50)
val PixiChipShape = RoundedCornerShape(50)
val PixiFieldShape = RoundedCornerShape(18.dp)
val PixiSheetShape = RoundedCornerShape(28.dp)

/** Soft white card — hairline border, zero elevation (reference list cards). */
@Composable
fun PixiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f),
    content: @Composable () -> Unit
) {
    val shape = PixiCardShape
    Card(
        modifier = if (onClick != null) {
            modifier
                .clip(shape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
        } else {
            modifier
        },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
    }
}

/** Soft elevated card for summary/hero blocks. */
@Composable
fun PixiSoftCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = PixiCardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        content()
    }
}

/** Full-width capsule primary CTA — solid lavender (idea2). */
@Composable
fun PixiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val d = rememberPixiDimens()
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(d.buttonHeight),
        shape = PixiPillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = d.body,
            maxLines = 1
        )
    }
}

/** Soft filled secondary capsule — light lavender (idea2 secondary CTAs). */
@Composable
fun PixiSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val d = rememberPixiDimens()
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(d.buttonHeight),
        shape = PixiPillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = d.body)
    }
}

/** Ghost outline pill. */
@Composable
fun PixiOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = rememberPixiDimens()
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(d.buttonHeight),
        shape = PixiPillShape,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(text = text, fontWeight = FontWeight.SemiBold, fontSize = d.body)
    }
}

/**
 * Selectable filter / category chip matching idea2 tags:
 * selected = solid lavender + white text
 * unselected = soft lavender-mist fill + muted text
 */
@Composable
fun PixiChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onPrimaryContainer
    val selectPop = rememberPopScale(selected)

    PixiPopClickable(
        onClick = onClick,
        modifier = modifier.graphicsLayer {
            scaleX = if (selected) selectPop else 1f
            scaleY = if (selected) selectPop else 1f
        },
        pressedScale = 0.92f
    ) {
        Box(
            modifier = Modifier
                .clip(PixiChipShape)
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = fg,
                maxLines = 1
            )
        }
    }
}

/** Soft search field shell used on list screens (idea2 messages search). */
@Composable
fun PixiSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    val isLight = MaterialTheme.colorScheme.background.red > 0.9f &&
        MaterialTheme.colorScheme.background.green > 0.9f
    val searchBg = if (isLight) PixiLightSearch
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(PixiPillShape)
            .background(searchBg)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(10.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                inner()
            }
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

/** Circular close / back affordance from reference modals. */
@Composable
fun PixiCircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 40.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun PixiCloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PixiCircleIconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** Yellow circular + FAB matching reference bottom nav — press pops. */
@Composable
fun PixiYellowFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    PixiPopClickable(
        onClick = onClick,
        modifier = modifier.size(size),
        pressedScale = 0.90f
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    ambientColor = PixiYellow.copy(alpha = 0.35f),
                    spotColor = PixiYellow.copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(PixiYellow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add",
                tint = Color(0xFF1C1C1E),
                modifier = Modifier.size(size * 0.48f)
            )
        }
    }
}

/** Empty / error state with doodle illustration (idea2 empty states). */
@Composable
fun PixiEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    @DrawableRes doodleRes: Int? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val d = rememberPixiDimens()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = d.sectionGap * 2, horizontal = d.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (doodleRes != null) {
            PixiDoodle3D(
                resId = doodleRes,
                size = d.emptyDoodle,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }
        Text(
            text = title,
            fontSize = d.headline,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            fontSize = d.body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 4
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            PixiPrimaryButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(if (d.isCompact) 1f else 0.72f)
            )
        }
    }
}

/** Screen title block — large bold headline + optional subtitle. */
@Composable
fun PixiScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null
) {
    val d = rememberPixiDimens()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                fontSize = d.title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = d.caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) trailing()
    }
}

/** Soft status / count badge pill. */
@Composable
fun PixiBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .clip(PixiChipShape)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1
        )
    }
}

/** Soft section label above lists. */
@Composable
fun PixiSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAction
                )
            )
        }
    }
}

/** Accent color helpers exposed for chips outside MaterialTheme. */
object PixiAccents {
    val lavender = PixiLavender
    val lavenderSoft = PixiLavenderSoft
    val yellow = PixiYellow
    val pink = Color(0xFFFF6BA8)
    val mint = Color(0xFF34D399)
    val coral = Color(0xFFFF7A8A)
    val sky = Color(0xFF67D4E8)
    val amber = Color(0xFFFBBF24)
}
