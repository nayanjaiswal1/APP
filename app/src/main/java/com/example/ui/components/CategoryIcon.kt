package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CatEducationBg
import com.example.ui.theme.CatEducationIcon
import com.example.ui.theme.CatEntertainmentBg
import com.example.ui.theme.CatEntertainmentIcon
import com.example.ui.theme.CatFoodBg
import com.example.ui.theme.CatFoodIcon
import com.example.ui.theme.CatGeneralBg
import com.example.ui.theme.CatGeneralIcon
import com.example.ui.theme.CatGroceriesBg
import com.example.ui.theme.CatGroceriesIcon
import com.example.ui.theme.CatHealthcareBg
import com.example.ui.theme.CatHealthcareIcon
import com.example.ui.theme.CatPersonalBg
import com.example.ui.theme.CatPersonalIcon
import com.example.ui.theme.CatRentBg
import com.example.ui.theme.CatRentIcon
import com.example.ui.theme.CatShoppingBg
import com.example.ui.theme.CatShoppingIcon
import com.example.ui.theme.CatTransportBg
import com.example.ui.theme.CatTransportIcon
import com.example.ui.theme.CatUtilitiesBg
import com.example.ui.theme.CatUtilitiesIcon

@Composable
fun CategoryIcon(
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val visual = getCategoryVisual(category)

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(visual.backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = visual.icon,
            contentDescription = category,
            tint = visual.iconColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

data class CategoryVisualData(
    val icon: ImageVector,
    val iconColor: Color,
    val backgroundColor: Color
)

fun getCategoryVisual(category: String): CategoryVisualData {
    return when (category.uppercase()) {
        "FOOD", "DINING", "RESTAURANT" -> CategoryVisualData(Icons.Default.Restaurant, CatFoodIcon, CatFoodBg)
        "GROCERIES", "GROCERY" -> CategoryVisualData(Icons.Default.ShoppingCart, CatGroceriesIcon, CatGroceriesBg)
        "TRAVEL", "TRANSPORT", "FLIGHTS", "UBER" -> CategoryVisualData(Icons.Default.DirectionsCar, CatTransportIcon, CatTransportBg)
        "SHOPPING" -> CategoryVisualData(Icons.Default.ShoppingBag, CatShoppingIcon, CatShoppingBg)
        "ENTERTAINMENT", "MOVIES", "GAMES" -> CategoryVisualData(Icons.Default.Celebration, CatEntertainmentIcon, CatEntertainmentBg)
        "UTILITIES", "BILLS", "INTERNET" -> CategoryVisualData(Icons.Default.Wifi, CatUtilitiesIcon, CatUtilitiesBg)
        "RENT", "HOUSING" -> CategoryVisualData(Icons.Default.Home, CatRentIcon, CatRentBg)
        "HEALTHCARE", "MEDICAL", "HEALTH", "PHARMACY" -> CategoryVisualData(Icons.Default.LocalHospital, CatHealthcareIcon, CatHealthcareBg)
        "PERSONAL" -> CategoryVisualData(Icons.Default.Person, CatPersonalIcon, CatPersonalBg)
        "EDUCATION" -> CategoryVisualData(Icons.Default.School, CatEducationIcon, CatEducationBg)
        else -> CategoryVisualData(Icons.Default.Receipt, CatGeneralIcon, CatGeneralBg)
    }
}

