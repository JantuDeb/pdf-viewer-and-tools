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
- `Tools` is scaffolded and already reuses the shared picker flow
- `Sign` is scaffolded and can stage a selected document
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

### 8. Processing engine foundation added
A first processing abstraction is now present, along with a PDFBox-backed implementation.

Files:
- `app/src/main/java/com/thestudypath/pdfviewer/processing/PdfProcessingEngine.kt`
- `app/src/main/java/com/thestudypath/pdfviewer/processing/PdfBoxProcessingEngine.kt`

Current implemented capability:
- inspect a PDF document
- read page count
- detect whether a PDF is encrypted

This is the base layer for real processing features such as merge, split, images-to-PDF, and text extraction.

### 9. Dependency and feature strategy documented
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
- merge PDFs
- split PDFs
- compress PDFs
- images to PDF
- extract text
- add/remove password
- watermarking
- page reordering/rotation/deletion/extraction
- embedded image extraction UI
- page-region crop/export flow

### Signing
- saved signature assets
- draw signature canvas
- import PNG signature
- page placement editor
- signed PDF export flow

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
- shared output/export result flow

## Build verification status

The project has been verified to build successfully after the module and app-shell changes.

Verified build flow:

```powershell
Set-Location "D:\ANDROID-NEW\PDFViewer"
.\gradlew.bat :pdfbox:assemble :app:assembleDebug --console=plain
```

Verified result:
- `BUILD SUCCESSFUL`

## Recommended next implementation steps

## Step 1 — expand `PdfProcessingEngine`
Add actual processing operations such as:
- `mergeDocuments(...)`
- `splitDocument(...)`
- `createPdfFromImages(...)`
- `extractText(...)`

This should remain the main boundary between app UI and PDF processing implementation.

## Step 2 — implement Merge PDF first
Recommended first real tool because it provides high user value and strongly validates the new `:pdfbox` integration.

Expected work:
- tool request model
- merge flow state
- multi-select document handling
- selected file reorder UI
- output file naming
- result open/share flow

## Step 3 — implement Split PDF second
Recommended immediately after merge.

Initial scope:
- split every N pages
- split by custom ranges
- save outputs as a batch

## Step 4 — implement Images to PDF third
This is a strong early feature because it validates image import and PDF generation and will also support future scanner flows.

Initial scope:
- import multiple images
- reorder images
- choose page size/layout
- generate output PDF

## Step 5 — add shared processing result flow
Standardize how every tool finishes:
- save in app storage
- export with SAF
- open result immediately
- share result

## Step 6 — add background job handling
Introduce `WorkManager` for heavier operations such as:
- merge on large files
- compression
- OCR
- scan export

## Step 7 — start the signing milestone
After the first few real tools, implement:
- signature asset storage
- signature drawing UI
- PNG signature import
- visible signature placement on PDFs

## Step 8 — start scanner MVP
After core tools are stable, implement scanner MVP with:
- CameraX preview
- OpenCV edge detection
- manual capture
- perspective correction
- PDF export

## Recommended execution order
1. Merge PDFs
2. Split PDF
3. Images to PDF
4. Extract text
5. Password add/remove validation spike
6. Signing milestone
7. Scanner MVP

## Short version

### Already done
- app shell foundation
- bottom navigation
- shared document layer
- shared picker UI
- PDFBox module integration
- processing engine foundation
- successful build verification

### Next best step
Implement real `Merge PDFs` end to end using the current `Tools` tab and the new `PdfProcessingEngine` abstraction.

