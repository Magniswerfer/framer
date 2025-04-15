package com.example.framer.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.framer.R
import com.example.framer.ui.theme.*

@Composable
fun CustomDrawer(
        showDrawer: Boolean,
        onDismiss: () -> Unit,
        onAboutClick: () -> Unit,
        drawerWidth: androidx.compose.ui.unit.Dp
) {
    val drawerOffset by
            animateFloatAsState(
                    targetValue = if (showDrawer) 0f else 1f,
                    animationSpec = tween(durationMillis = 300),
                    label = "drawerOffset"
            )

    if (showDrawer) {
        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .clickable { onDismiss() }
                                .background(
                                        MaterialTheme.colorScheme.scrim.copy(
                                                alpha = 0.32f * (1f - drawerOffset)
                                        )
                                )
        ) {
            Box(
                    modifier =
                            Modifier.align(Alignment.CenterEnd)
                                    .width(drawerWidth)
                                    .fillMaxHeight()
                                    .background(
                                            MaterialTheme.colorScheme.background.copy(
                                                    alpha = 1f - drawerOffset
                                            )
                                    )
                                    .offset(x = (drawerWidth.value * drawerOffset).dp)
                                    .clickable(enabled = false) {} // Prevent click propagation
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Close Button
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close drawer",
                                    tint =
                                            MaterialTheme.colorScheme.onBackground.copy(
                                                    alpha = 1f - drawerOffset
                                            )
                            )
                        }
                    }

                    // Navigation Items
                    Box(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 12.dp)
                                            .background(
                                                    Red.copy(alpha = 1f - drawerOffset),
                                                    RoundedCornerShape(24.dp)
                                            )
                                            .clickable {
                                                onDismiss()
                                                onAboutClick()
                                            }
                                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "About",
                                    tint = White.copy(alpha = 1f - drawerOffset)
                            )
                            Text(
                                    "About",
                                    color = White.copy(alpha = 1f - drawerOffset),
                                    style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    // Spacer to push logo to bottom
                    Spacer(modifier = Modifier.weight(1f))

                    // Logo at bottom
                    Box(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.size(64.dp)) {
                            Image(
                                    painter = painterResource(id = R.drawable.framer_logo),
                                    contentDescription = "Framer Logo",
                                    modifier = Modifier.fillMaxSize().alpha(1f - drawerOffset)
                            )
                        }
                    }
                }
            }
        }
    }
}
