package com.rohan.progresstracker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.rohan.progresstracker.BuildConfig
import com.rohan.progresstracker.R

/** Contents of the side menu: profile header, then links to every section of the app. */
@Composable
fun AppDrawerContent(currentRoute: String?, onNavigate: (String) -> Unit) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ProfileHeader(onClick = { onNavigate(Routes.PROFILE) })
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

            DrawerItem("Progress", rememberVectorPainter(Icons.Filled.CheckCircle), Routes.PROGRESS, currentRoute, onNavigate)
            DrawerItem("Todo", rememberVectorPainter(Icons.AutoMirrored.Filled.List), Routes.TODO, currentRoute, onNavigate)

            HorizontalDivider(modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp))

            DrawerItem("Profile", rememberVectorPainter(Icons.Filled.AccountCircle), Routes.PROFILE, currentRoute, onNavigate)
            DrawerItem("Import & export", painterResource(R.drawable.ic_import_export), Routes.DATA_TRANSFER, currentRoute, onNavigate)
            DrawerItem("About", rememberVectorPainter(Icons.Filled.Info), Routes.ABOUT, currentRoute, onNavigate)

            Spacer(Modifier.weight(1f))
            Text(
                "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
            )
        }
    }
}

@Composable
private fun ProfileHeader(onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileAvatar(Modifier.size(56.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Guest", style = MaterialTheme.typography.titleMedium)
                Text(
                    "View profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Round placeholder picture used until profiles exist. */
@Composable
fun ProfileAvatar(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.fillMaxSize(0.6f))
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: Painter,
    route: String,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = currentRoute == route,
        onClick = { onNavigate(route) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}
