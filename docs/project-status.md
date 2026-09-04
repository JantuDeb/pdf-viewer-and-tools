# Project Status and Next Implementation Steps

## Checklist
- [x] Summarize what is already implemented in the app
- [x] Capture PDFBox integration status
- [x] Record build verification status
- [x] List the next recommended implementation steps

## Current status

The project is no longer just a viewer demo foundation. The app now has the first milestone structure needed to grow into a full PDF management and scanner app.

## What has been implemented

### 1. Product planning and implementation doc
The project now includes a detailed implementation plan for the PDF management and scanner roadmap:

- `docs/pdf-management-scanner-implementation.md`

That document covers:
- Files / Tools / Sign / Scanner app structure
- Phase 1 PDF management features
- Phase 2 scanner and OCR features
- dependency recommendations
- feature-to-library coverage guidance

### 2. App shell with bottom navigation
The app now has a workspace shell with four root tabs:

- `Files`
- `Tools`
- `Sign`
- `Scanner`

Main files involved:
- `app/src/main/java/com/thestudypath/pdfviewer/MainActivity.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/navigation/AppDestination.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/navigation/PdfViewerNavHost.kt`

This establishes the long-term navigation structure needed for document workflows.

### 3. Reusable document catalog abstraction
A document catalog layer now sits on top of the current recent-files storage so multiple features can reuse the same document model without forcing a risky database migration yet.

Files added:
- `app/src/main/java/com/thestudypath/pdfviewer/catalog/DocumentItem.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/catalog/DocumentCatalogRepository.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/catalog/DocumentCatalogUiState.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/catalog/DocumentCatalogViewModel.kt`

This is the base for:
- Files tab browsing
- tool input selection
- sign input selection
- future scanner outputs

### 4. Shared document picker and browsing UI
A reusable shared document UI foundation is now in place.

Main file:
- `app/src/main/java/com/thestudypath/pdfviewer/ui/documents/DocumentPickerComponents.kt`

Current shared capabilities include:
- search
- grid/list switch
- single-select and multi-select behavior
- thumbnails
- empty/loading/error states
- reusable picker sheet

This is the UI foundation for future tools like merge, split, sign, and extract text.

### 5. Route scaffolding for each tab
Each tab now has a dedicated route file:

- `app/src/main/java/com/thestudypath/pdfviewer/ui/files/FilesRoute.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/ui/tools/ToolsRoute.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/ui/sign/SignRoute.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/ui/scanner/ScannerRoute.kt`

Current maturity:
- `Files` is functional and uses the shared document layer
- `Tools` is functional and now opens dedicated detail screens for each shipped tool
- `Sign` now supports a visible-signing MVP with document selection, signature drawing/import, placement presets, and signed PDF export
- `Scanner` is reserved as the future camera-first entry point

### 6. Existing viewer flow preserved
The app still supports the existing PDF import/open flow and viewer launch behavior.

Relevant file:
- `app/src/main/java/com/thestudypath/pdfviewer/PdfViewerActivity.kt`

The new workspace structure was added without removing the current viewer capability.

### 7. Local PDFBox module integrated
The requested PDFBox Android fork has been added and integrated as a project module.

Relevant files:
- `.gitmodules`
- `settings.gradle.kts`
- `app/build.gradle.kts`
- `gradle.properties`
- `pdfbox/build.gradle`

Current integration state:
- `pdfbox/` exists as a local git submodule
- Gradle includes `:pdfbox`
- `app` depends on `implementation(project(":pdfbox"))`
- the imported `pdfbox` build script was patched to remove a legacy AGP-incompatible AAR output block

### 8. Processing engine and real tool flows added
A processing abstraction is now present, along with a PDFBox-backed implementation that powers the current tool set.

Files:
- `app/src/main/java/com/thestudypath/pdfviewer/processing/PdfProcessingEngine.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/processing/PdfBoxProcessingEngine.kt`

Current implemented capabilities:
- inspect a PDF document
- merge PDFs
- compress PDFs
- create PDFs from images
- split PDFs by generated page ranges
- extract text
- add password protection
- remove password protection
- export selected PDF pages as PNG or JPEG images
- extract embedded PDF images into standalone image files
- crop selected PDF page regions into PNG or JPEG images
- stamp a visible signature image onto a selected PDF page
- add text watermarks to selected PDF pages
- rotate selected PDF pages
- extract selected PDF pages into a new PDF
- delete selected PDF pages and save the remaining pages as a new PDF

These capabilities now back the actual shipped tool pages instead of only serving as a future foundation.

### 9. Tools implemented end to end
The current `Tools` surface now shows a simple list of tools, and each entry opens a dedicated detail screen with the actual workflow UI.

Currently implemented tools:
- `Compress PDF`
- `Images to PDF`
- `PDF to Images`
- `Extract Embedded Images`
- `Crop Page Region`
- `Merge PDFs`
- `Split PDF`
- `Extract Text`
- `Add Password`
- `Remove Password`
- `Watermark PDF`
- `Rotate Pages`
- `Extract Pages`
- `Delete Pages`

### 10. Signing MVP implemented
The `Sign` tab is no longer only a staging surface. It now supports a first visible-signing workflow.

Currently implemented sign capabilities:
- choose a PDF from the shared document picker
- draw a signature directly in-app
- import a signature image from device storage
- choose a target page
- choose a placement preset and signature size
- export a signed PDF copy
- open, share, or save-copy the signed result

### 11. Shared single-file result/export flow implemented
Single-file outputs now follow a more consistent result pattern across the shipped PDF and text tools.

Current shared result capabilities include:
- keep tool outputs in app-private storage by default
- open the generated result immediately
- share the generated result with Android sharesheets
- save a copy to a user-chosen SAF destination via `ACTION_CREATE_DOCUMENT`

This shared flow now covers the current single-file result surfaces such as:
- merge/compress/images-to-PDF outputs
- extract text `.txt` outputs
- password add/remove outputs
- watermarking outputs
- rotate/extract/delete page outputs
- visible-signing outputs

### 12. Dependency and feature strategy documented
The implementation doc now explicitly describes where each feature should live:

- `:pdf` for viewer and annotation behavior
- Android `PdfRenderer` for thumbnails and preview rendering
- `:pdfbox` for processing operations
- CameraX for scanning camera flows
- OpenCV for edge detection and perspective correction
- ML Kit for OCR
- WorkManager for long-running background jobs
- DocumentFile and SAF flows for file import/export
- Security Crypto for protecting sensitive local assets

## What is not implemented yet

The following are still planned, not shipped:

### PDF management tools
- page reordering
- image watermarking

### Signing
- saved signature assets
- reusable signature library/asset management across sessions
- advanced drag/resize/rotate placement editor
- certificate-backed digital signature flow

### Scanner
- CameraX live preview
- OpenCV edge detection
- auto capture
- perspective correction
- scan cleanup filters
- OCR flow
- scan-to-PDF export

### Background jobs and production workflow
- WorkManager job orchestration
- progress reporting
- job history
- richer folder/batch output export flow for multi-image and multi-file result sets

## Build verification status

The project has been verified to build successfully after the module and app-shell changes.

Verified build flow:

```powershell
Set-Location "D:\ANDROID-NEW\PDFViewer"
.\gradlew.bat :app:testDebugUnitTest :app:compileDebugKotlin :app:assembleDebug --console=plain --no-daemon
```

Verified result:
- `BUILD SUCCESSFUL`

## Recommended next implementation steps

## Step 1 — add the next organize/export tool wave
Recommended next tools:
- page reordering
- image watermarking
- richer folder/batch export polish for image-heavy tools

These features build directly on the same processing boundary, page inspection flow, and result pattern already proven by the current tool set.

## Step 2 — deepen the signing milestone
Recommended follow-ups for `Sign`:
- persistent saved signature assets
- richer drag/resize/rotate placement editor
- recent signature management

## Step 3 — extend result flow to folder and batch outputs
Finish standardizing how the multi-output tools finish:
- export image folders to richer SAF destinations
- handle batch save/share flows consistently
- add clearer result summaries for larger output sets
- keep open/share/save-copy behavior aligned with the single-file tools

## Step 4 — add background job handling
Introduce `WorkManager` for heavier operations such as:
- merge on large files
- compression
- signing on larger documents
- OCR
- scan export

## Step 5 — start scanner MVP
After the current document tools and signing flow are stable, implement scanner MVP with:
- CameraX preview
- OpenCV edge detection
- manual capture
- perspective correction
- PDF export

## Recommended execution order
1. Shared export/result flow polish and page reordering
2. Persistent signature assets and richer placement
3. Image watermarking and richer page picking
4. Scanner MVP

## Short version

### Already done
- app shell foundation
- bottom navigation
- shared document layer
- shared picker UI
- PDFBox module integration
- processing engine with real PDF operations
- visible-signing MVP
- shared single-file result/export flow with SAF save-copy
- text watermarking MVP
- PDF-to-images MVP
- embedded-image extraction MVP
- page-region crop/export MVP
- successful build verification

### Next best step
Build the next organize/export wave on top of the now-functional tool detail pattern, processing engine, shared single-file result flow, signing MVP, watermarking MVP, PDF-to-images MVP, embedded-image extraction MVP, page-region crop/export MVP, and page-organization tools, starting with page reordering, image watermarking, and richer folder/batch export polish.

