package com.example.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.PdfBookmark
import com.example.data.database.PdfFile
import com.example.ui.theme.*
import com.example.ui.viewmodel.PdfViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    viewModel: PdfViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentPdf by viewModel.currentPdf.collectAsState()

    if (currentPdf == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No document loaded.")
        }
        return
    }

    val pdf = currentPdf!!

    // Reading States
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var nightMode by remember { mutableStateOf(false) }
    var isJumpDialogOpen by remember { mutableStateOf(false) }
    var isBookmarkDialogOpen by remember { mutableStateOf(false) }
    var bookmarkLabel by remember { mutableStateOf("") }

    // Lazy List scroll manager loaded at database's currentPage index
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = pdf.currentPage)

    // Save current index on scroll debounced
    val visiblePageIndex = listState.firstVisibleItemIndex
    LaunchedEffect(visiblePageIndex) {
        delay(600) // Debounce db write triggers while fast-scrolling pages
        viewModel.updateReadingPage(pdf, visiblePageIndex)
    }

    // Bookmarks for current PDF
    var pdfBookmarks by remember { mutableStateOf<List<PdfBookmark>>(emptyList()) }
    LaunchedEffect(pdf.uri) {
        viewModel.getBookmarks(pdf.uri).collect { list ->
            pdfBookmarks = list
        }
    }

    val isCurrentContentBookmarked = pdfBookmarks.any { it.pageIndex == visiblePageIndex }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = pdf.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (nightMode) DarkTextPrimary else TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Page ${visiblePageIndex + 1} of ${pdf.pageCount}",
                            fontSize = 11.sp,
                            color = if (nightMode) DarkTextSecondary else TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (nightMode) DarkTextPrimary else TextPrimary
                        )
                    }
                },
                actions = {
                    // Favorite Toggle Action
                    IconButton(
                        onClick = { viewModel.toggleFavorite(pdf) },
                        modifier = Modifier.testTag("reader_fav_toggle")
                    ) {
                        Icon(
                            imageVector = if (pdf.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (pdf.isFavorite) Color(0xFFFFB300) else (if (nightMode) DarkTextSecondary else SoftGrayHint)
                        )
                    }

                    // Bookmark Landmark Action
                    IconButton(
                        onClick = {
                            if (isCurrentContentBookmarked) {
                                viewModel.removeBookmarkAtPage(pdf.uri, visiblePageIndex)
                            } else {
                                bookmarkLabel = "Tag at Page ${visiblePageIndex + 1}"
                                isBookmarkDialogOpen = true
                            }
                        },
                        modifier = Modifier.testTag("reader_bookmark_btn")
                    ) {
                        Icon(
                            imageVector = if (isCurrentContentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Landmark",
                            tint = if (isCurrentContentBookmarked) PrimaryBlue else (if (nightMode) DarkTextSecondary else SoftGrayHint)
                        )
                    }

                    // Night mode Switch
                    IconButton(
                        onClick = { nightMode = !nightMode },
                        modifier = Modifier.testTag("night_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (nightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Night Mode",
                            tint = if (nightMode) Color(0xFFFFD54F) else TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (nightMode) DarkSurface else MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = if (nightMode) DarkBackground else MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Lazy Loaded Render Pages List
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                items(pdf.pageCount) { pageIndex ->
                    PdfPageCard(
                        pdfUri = pdf.uri,
                        pageIndex = pageIndex,
                        zoomScale = zoomScale,
                        nightMode = nightMode
                    )
                }
            }

            // Bottom Floating Controls (Zoom Pill & Slider Progress Container)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Zoom Action Control Row (Aesthetic Pill matching UI specification)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (nightMode) DarkSurface else Color(0xFFE1E2E9))
                        .border(1.dp, if (nightMode) DarkBorder else LightBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { if (zoomScale > 0.5f) zoomScale -= 0.25f },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (nightMode) DarkBackground else Color.White)
                    ) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (nightMode) DarkTextPrimary else TextPrimary,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = { if (zoomScale < 3.0f) zoomScale += 0.25f },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(LightBlueAccent)
                    ) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = DarkBlueText, modifier = Modifier.size(18.dp))
                    }
                }

                // Seek & Page Quick-Jump Indicators (Styled perfectly with Clean Minimalism bounds)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (nightMode) DarkSurface else Color(0xFFE1E2E9)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.AutoStories, contentDescription = null, size = 16.dp)
                                Text(
                                    text = "Reading Tracker",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (nightMode) DarkTextSecondary else TextSecondary
                                )
                            }

                            // Dynamic Click Bubble to open instant jump Dialog
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (nightMode) DarkBackground else Color.White.copy(alpha = 0.5f))
                                    .clickable { isJumpDialogOpen = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${visiblePageIndex + 1} / ${pdf.pageCount}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (nightMode) DarkTextPrimary else TextPrimary
                                )
                            }
                        }

                        // Slider progress line (Custom Seek)
                        Slider(
                            value = visiblePageIndex.toFloat(),
                            onValueChange = { targetPageVal ->
                                scope.launch {
                                    listState.scrollToItem(targetPageVal.toInt())
                                }
                            },
                            valueRange = 0f..((pdf.pageCount - 1).coerceAtLeast(1).toFloat()),
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue,
                                inactiveTrackColor = if (nightMode) DarkBorder else Color.LightGray
                            ),
                            modifier = Modifier.height(18.dp)
                        )
                    }
                }
            }
        }

        // Jump To Page Numeric Input Dialog
        if (isJumpDialogOpen) {
            var inputPage by remember { mutableStateOf((visiblePageIndex + 1).toString()) }
            var isInputErr by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { isJumpDialogOpen = false },
                title = { Text("Go to Page") },
                text = {
                    Column {
                        Text("Enter page number (1 to ${pdf.pageCount}):", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputPage,
                            onValueChange = {
                                inputPage = it
                                isInputErr = false
                            },
                            isError = isInputErr,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isInputErr) {
                            Text("Please enter a valid page number.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val parsed = inputPage.trim().toIntOrNull()
                            if (parsed != null && parsed in 1..pdf.pageCount) {
                                scope.launch {
                                    listState.scrollToItem(parsed - 1)
                                }
                                isJumpDialogOpen = false
                            } else {
                                isInputErr = true
                            }
                        }
                    ) {
                        Text("Jump", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isJumpDialogOpen = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Bookmark landmark customization Dialog
        if (isBookmarkDialogOpen) {
            AlertDialog(
                onDismissRequest = { isBookmarkDialogOpen = false },
                title = { Text("Bookmark Page ${visiblePageIndex + 1}") },
                text = {
                    Column {
                        Text("Add reference label (e.g., 'Chapter 3 Formula'):", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bookmarkLabel,
                            onValueChange = { bookmarkLabel = it },
                            placeholder = { Text("Landmark tag name") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addBookmark(pdf.uri, visiblePageIndex, bookmarkLabel)
                            isBookmarkDialogOpen = false
                        }
                    ) {
                        Text("Save Bookmark", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isBookmarkDialogOpen = false }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun PdfPageCard(
    pdfUri: String,
    pageIndex: Int,
    zoomScale: Float,
    nightMode: Boolean
) {
    val context = LocalContext.current
    var pageBitmap by remember(pdfUri, pageIndex, zoomScale) { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember(pdfUri, pageIndex) { mutableStateOf(true) }
    var widthOriginal by remember { mutableIntStateOf(595) }
    var heightOriginal by remember { mutableIntStateOf(842) }

    // Multi-threaded resource loading
    LaunchedEffect(pdfUri, pageIndex, zoomScale) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(pdfUri)
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (pageIndex in 0 until renderer.pageCount) {
                            val page = renderer.openPage(pageIndex)
                            try {
                                widthOriginal = page.width
                                heightOriginal = page.height

                                // High quality rendering scaling factor
                                val displayDensity = context.resources.displayMetrics.density
                                val baseDpiScale = 1.3f * displayDensity
                                val finalScale = baseDpiScale * zoomScale

                                val widthPx = (page.width * finalScale).toInt().coerceAtMost(2800)
                                val heightPx = (page.height * finalScale).toInt().coerceAtMost(3800)

                                if (widthPx > 0 && heightPx > 0) {
                                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                                    val canvas = android.graphics.Canvas(bitmap)
                                    canvas.drawColor(android.graphics.Color.WHITE) // draw solid base
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    
                                    pageBitmap = bitmap
                                }
                            } catch (e: Exception) {
                                Log.e("PdfPageCard", "Error detail rendering bitmap page $pageIndex", e)
                            } finally {
                                page.close()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PdfPageCard", "Failed to retrieve page $pageIndex descriptor", e)
            } finally {
                isLoading = false
            }
        }
    }

    val aspectRatio = widthOriginal.toFloat() / heightOriginal.toFloat()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (nightMode) DarkSurface else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (nightMode) DarkBorder else LightBorder, RoundedCornerShape(12.dp))
            .aspectRatio(aspectRatio)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading && pageBitmap == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Rendering page ${pageIndex + 1}...", fontSize = 11.sp, color = SoftGrayHint)
                }
            } else {
                pageBitmap?.let { bitmap ->
                    val colorFilter = if (nightMode) {
                        // Eye protective gray shadow matrix inversion
                        val matrix = ColorMatrix(floatArrayOf(
                            -1.0f, 0.0f, 0.0f, 0.0f, 255f, // Red
                            0.0f, -1.0f, 0.0f, 0.0f, 255f, // Green
                            0.0f, 0.0f, -1.0f, 0.0f, 255f, // Blue
                            0.0f, 0.0f, 0.0f, 1.0f, 0.0f    // Alpha
                        ))
                        ColorFilter.colorMatrix(matrix)
                    } else {
                        null
                    }

                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        colorFilter = colorFilter,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// Utility icon sizing helper
@Composable
private fun Icon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String?,
    size: androidx.compose.ui.unit.Dp
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}
