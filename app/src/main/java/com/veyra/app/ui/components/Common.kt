package com.veyra.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BakeryDining
import androidx.compose.material.icons.outlined.DinnerDining
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material.icons.outlined.LunchDining
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.veyra.app.data.MealType
import com.veyra.app.ui.theme.Accents
import com.veyra.app.ui.theme.overline
import com.veyra.app.ui.theme.tabular

/** Accent colours for the three macros. */
object MacroColors {
    val Protein = Accents.Protein
    val Carbs = Accents.Carbs
    val Fat = Accents.Fat
}

val MealType.icon: ImageVector
    get() = when (this) {
        MealType.BREAKFAST -> Icons.Outlined.FreeBreakfast
        MealType.LUNCH -> Icons.Outlined.LunchDining
        MealType.DINNER -> Icons.Outlined.DinnerDining
        MealType.SNACK -> Icons.Outlined.BakeryDining
    }

/** Standard page padding and spacing. */
object Spacing {
    val page = 20.dp
    val section = 16.dp
    val item = 12.dp
}

/** A flat card: raised surface, hairline border, no shadow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large
    val border = BorderStroke(1.dp, colors.outlineVariant)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, color = colors.surfaceContainerLow, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = shape, color = colors.surfaceContainerLow, border = border) {
            Column(Modifier.padding(contentPadding), content = content)
        }
    }
}

/** An all-caps label above a group of content, with an optional action on the right. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall.overline,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        action()
    }
}

/** A card with a title, optional subtitle and content. */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AppCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(16.dp))
        content()
    }
}

/** A selectable tile (activity level, goal, meal type…). */
@Composable
fun OptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primary.copy(alpha = 0.10f) else colors.surfaceContainerLow,
        contentColor = colors.onSurface,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), content = content)
    }
}

/** A rounded square with a tinted background holding an icon. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 40.dp) {
    Box(
        Modifier.size(size).background(tint.copy(alpha = 0.14f), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

/** A labelled number, e.g. "PROTEIN · 39 / 176 g · 137 g left". */
@Composable
fun StatBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color? = null,
    valueSuffix: String? = null,
    detail: String? = null,
) {
    Column(modifier) {
        if (label.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (color != null) {
                    Box(Modifier.size(8.dp).background(color, CircleShape))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
        }
        val suffixColor = MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            buildAnnotatedString {
                append(value)
                if (valueSuffix != null) {
                    withStyle(SpanStyle(fontSize = MaterialTheme.typography.bodySmall.fontSize, fontWeight = FontWeight.Medium, color = suffixColor)) {
                        append(" $valueSuffix")
                    }
                }
            },
            style = MaterialTheme.typography.titleLarge.tabular(),
            maxLines = 1,
        )
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall.tabular(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** The main call-to-action: solid accent, no shadow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = MaterialTheme.shapes.medium,
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            // Keep the accent while loading so the button doesn't flash grey.
            disabledContainerColor = if (loading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = if (loading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        modifier = modifier.height(52.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            when {
                loading -> {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                }
                icon != null -> {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
            }
            Text(text, style = MaterialTheme.typography.titleSmall)
        }
    }
}

/** A thin rule between rows inside a card. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}
