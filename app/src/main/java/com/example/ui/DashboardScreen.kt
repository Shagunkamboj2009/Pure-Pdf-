package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PdfBookmark
import com.example.data.database.PdfFile
import com.example.ui.theme.*
import com.example.ui.viewmodel.PdfViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: PdfViewModel,
    onNavigateToReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allFiles by viewModel.allPdfFiles.collectAsState()
    val favoriteFiles by viewModel.favoritePdfFiles.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Recents, 1: Favorites, 2: Pinned Lines
    var currentDeleteTarget by remember { mutableStateOf<PdfFile?>(null) }

    // Launcher for selecting a PDF file from the local file storage
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.openPdf(uri, context, forceNavigate = onNavigateToReader)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Pure PDF",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "Secure local document viewer",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.testTag("open_file_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Open PDF",
                            tint = PrimaryBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch(arrayOf("application/pdf")) },
                containerColor = LightBlueAccent,
                contentColor = DarkBlueText,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                text = { Text("Open PDF") },
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("open_pdf_fab")
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search Utility
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search files by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = LightBorder,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_field")
                    .padding(vertical = 8.dp)
            )

            // Statistics Row Cards
            Spacer(modifier = Modifier.height(8.dp))
            StatisticsRow(allFiles = allFiles)
            Spacer(modifier = Modifier.height(16.dp))

            // Sliding Tabs (Recents, Favorites, Bookmark Landmarks)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = { Divider(color = LightBorder, thickness = 1.dp) },
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = PrimaryBlue
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Recents", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_recents")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Favorites", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_favorites")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Bookmarks", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    modifier = Modifier.testTag("tab_bookmarks")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Listing
            when (selectedTab) {
                0 -> {
                    if (allFiles.isEmpty()) {
                        EmptyFilesState("No recent documents opened yet.") {
                            filePickerLauncher.launch(arrayOf("application/pdf"))
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(allFiles, key = { it.uri }) { file ->
                                PdfFileCard(
                                    pdfFile = file,
                                    onClick = {
                                        viewModel.openSpecificPdf(file)
                                        onNavigateToReader()
                                    },
                                    onLongClick = { currentDeleteTarget = file },
                                    onToggleFavorite = { viewModel.toggleFavorite(file) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    if (favoriteFiles.isEmpty()) {
                        EmptyFilesState("No favorite documents found.") {
                            selectedTab = 0
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 80.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(favoriteFiles, key = { it.uri }) { file ->
                                PdfFileCard(
                                    pdfFile = file,
                                    onClick = {
                                        viewModel.openSpecificPdf(file)
                                        onNavigateToReader()
                                    },
                                    onLongClick = { currentDeleteTarget = file },
                                    onToggleFavorite = { viewModel.toggleFavorite(file) }
                                )
                            }
                        }
                    }
                }
                2 -> {
                    // Aggregate bookmarks across all files or show empty state
                    var bookmarksList by remember { mutableStateOf<List<PdfBookmark>>(emptyList()) }
                    LaunchedEffect(allFiles) {
                        // Gather bookmarks for opened files from database or simple list.
                        // Let's observe database flow. Wait, we can fetch them on screen
                        // but to keep it extremely fluid, let's collect bookmarks for each file.
                    }
                    BookmarksFeedSection(viewModel, allFiles, onNavigateToReader)
                }
            }
        }

        // Deletion Confirm Dialog
        currentDeleteTarget?.let { file ->
            AlertDialog(
                onDismissRequest = { currentDeleteTarget = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteRecentPdf(file)
                            currentDeleteTarget = null
                        }
                    ) {
                        Text("Remove History", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { currentDeleteTarget = null }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Remove from Recents?") },
                text = { Text("Are you sure you want to remove '${file.name}' from your reading history? The actual file is safe and will not be deleted.") },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun StatisticsRow(allFiles: List<PdfFile>) {
    val totalCount = allFiles.size
    val totalReadableSize = allFiles.sumOf { it.size }
    val totalPagesRead = allFiles.sumOf { it.currentPage }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Doc count
        StatCard(
            title = "Docs",
            value = totalCount.toString(),
            icon = Icons.Default.LibraryBooks,
            modifier = Modifier.weight(1f)
        )
        // Memory formatted size
        StatCard(
            title = "Scanned",
            value = formatSize(totalReadableSize),
            icon = Icons.Default.PieChart,
            modifier = Modifier.weight(1.2f)
        )
        // Saved state pages
        StatCard(
            title = "Progress",
            value = "$totalPagesRead pages",
            icon = Icons.Default.BookmarkBorder,
            modifier = Modifier.weight(1.1f)
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.border(1.dp, LightBorder, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun PdfFileCard(
    pdfFile: PdfFile,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, LightBorder, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Document Red/Blue Vector Graphic Frame
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LightBlueAccent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = DarkBlueText,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text Metadata
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = pdfFile.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatSize(pdfFile.size)} • ${pdfFile.pageCount} pgs • Read ${pdfFile.currentPage + 1}/${pdfFile.pageCount}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Opened " + formatDate(pdfFile.lastOpened),
                    fontSize = 10.sp,
                    color = SoftGrayHint
                )
            }

            // Favorite action button
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.testTag("fav_btn_${pdfFile.name.replace(" ", "_")}")
            ) {
                Icon(
                    imageVector = if (pdfFile.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (pdfFile.isFavorite) Color(0xFFFFB300) else SoftGrayHint
                )
            }
        }
    }
}

@Composable
fun BookmarksFeedSection(
    viewModel: PdfViewModel,
    allFiles: List<PdfFile>,
    onNavigateToReader: () -> Unit
) {
    if (allFiles.isEmpty()) {
        EmptyFilesState("No bookmarked pages found.") {}
    } else {
        // Collect bookmarks for all files
        var aggregatedBookmarks by remember { mutableStateOf<List<Pair<PdfFile, PdfBookmark>>>(emptyList()) }

        LaunchedEffect(allFiles) {
            val list = mutableListOf<Pair<PdfFile, PdfBookmark>>()
            allFiles.forEach { file ->
                viewModel.getBookmarks(file.uri).first().forEach { bookmark ->
                    list.add(Pair(file, bookmark))
                }
            }
            aggregatedBookmarks = list.sortedByDescending { it.second.timestamp }
        }

        if (aggregatedBookmarks.isEmpty()) {
            EmptyFilesState("Save beautiful page coordinates in the Reader to see them here.") {}
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(aggregatedBookmarks, key = { "${it.second.pdfUri}_${it.second.id}" }) { item ->
                    val file = item.first
                    val bookmark = item.second

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, LightBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.openSpecificPdf(file)
                                viewModel.updateReadingPage(file, bookmark.pageIndex)
                                onNavigateToReader()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(LightBlueAccent),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, tint = DarkBlueText, modifier = Modifier.size(18.dp))
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bookmark.label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Page ${bookmark.pageIndex + 1}  •  ${file.name}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { viewModel.removeBookmark(bookmark.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Bookmark", tint = SoftGrayHint)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFilesState(
    message: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(LightBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterFrames,
                    contentDescription = null,
                    tint = SoftGrayHint,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = LightBlueAccent, contentColor = DarkBlueText)
            ) {
                Text("Proceed", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Utility formats
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 Bytes"
    val units = arrayOf("B", "KB", "MB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 2)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDate(timestamp: Long): String {
    val df = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return df.format(Date(timestamp))
}
