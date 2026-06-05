package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryList(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = listOf(
        CategoryItem("Todos", "🌐 Todos"),
        CategoryItem("Costa Rica", "🇨🇷 Costa Rica"),
        CategoryItem("Perú", "🇵🇪 Perú"),
        CategoryItem("Argentina", "🇦🇷 Argentina"),
        CategoryItem("Favoritos", "⭐ Favoritos")
    )

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 12.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { item ->
                val isSelected = selectedCategory == item.id
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    },
                    label = "bgColorAnim"
                )

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    label = "textColorAnim"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(backgroundColor)
                        .clickable { onCategorySelected(item.id) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .testTag("category_tab_${item.id.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (item.id == "Favoritos" && isSelected) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Favoritos",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = item.label,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private data class CategoryItem(val id: String, val label: String)
