# PDF Management + Scanner Implementation Plan

## Checklist
- [x] Define the target product shape beyond the current viewer-only app
- [x] Design the bottom navigation shell: `Files`, `Tools`, `Sign`, `Scanner`
- [x] Propose a reusable document/recent-files abstraction for all flows
- [x] Map Phase 1 PDF management features end to end
- [x] Map Phase 2 intelligent scanner flows end to end
- [x] Evaluate what the current stack can cover vs what new libraries are needed
- [x] Call out performance, storage, security, testing, and rollout concerns

## 1. Current baseline in this repo

Today the project already has two strong foundations:

- `app` is a demo app that opens PDFs and stores recent files in Room.
- `pdf` is a reusable viewer module built around `androidx.pdf` / `pdf-ink` plus the existing host extension points in:
  - `pdf/src/main/java/com/thestudypath/pdf/PdfConfig.kt`
  - `pdf/src/main/java/com/thestudypath/pdf/interfaces/PdfInterfaces.kt`
  - `pdf/src/main/java/com/thestudypath/pdf/PdfActivityCompose.kt`

Current app strengths:

- PDF open/import flow already exists in `MainActivity`
- recent files are persisted in Room
- thumbnails are rendered with `PdfRenderer`
- annotations/edit/save hooks already exist in the `pdf` module

Current gap:

- the workspace shell now exists, but advanced document workflows are still only partially implemented
- shared document picker and document catalog foundations are now in place
- a processing engine now powers merge/split/compress/images-to-pdf/pdf-to-images/extract-text/password tools, visible signing, watermarking, and page-organization flows
- there is no camera/scanner pipeline yet
- single-file output flows now support open/share/SAF save-copy, but advanced folder/batch export, background job orchestration, and some post-processing tools are still pending

---

## 2. Product target

Turn the app from **viewer-first** into a **document workspace** with four bottom tabs:

1. `Files`
2. `Tools`
3. `Sign`
4. `Scanner`

### Tab responsibilities

#### `Files`
Primary document hub.

Responsibilities:
- recent files
- imported files
- search/filter/sort
- quick actions: open, share, rename, duplicate, delete
- entry point into tool actions from a selected file

#### `Tools`
Grid/list of all PDF tools.

Responsibilities:
- show all available tools grouped by category
- launch a reusable file-selection flow per tool
- show recent compatible files for each tool
- show job progress/history for long-running operations

Suggested groups:
- Organize: merge, split, reorder, rotate, delete pages, extract pages
- Optimize: compress, flatten annotations, remove unused objects, grayscale scans
- Convert: images to PDF, PDF to images, text export
- Security: add password, remove password, permissions
- Content: watermark, extract images, crop area, extract text, metadata cleanup

#### `Sign`
Signature-focused workflow.

Responsibilities:
- create signature by drawing with pen
- import transparent PNG signature
- manage saved signatures
- place/resize/rotate signature on PDF
- optional certificate-backed digital signature later

#### `Scanner`
Camera-first document capture.

Responsibilities:
- open camera by default when tab is selected
- detect page edges live
- auto capture when stable
- manual capture fallback
- multi-page scan session
- cleanup filters
- export scanned PDF
- optional OCR layer / searchable PDF

---

## 3. Recommended architecture direction

## 3.1 Keep the existing `pdf` module viewer-focused

The current `pdf` module should remain responsible for:
- viewing
- annotation/ink editing
- save/save-as hooks
- page persistence
- viewer chrome

That keeps the module reusable and avoids mixing camera, OCR, and heavy processing concerns into the viewer layer.

## 3.2 Add a processing layer for document operations

Recommended direction:

- keep `:pdf` as the viewer module
- add a new processing-focused module later, e.g. `:pdf-processing`
- keep scanner UI in `app` first, with the option to extract a `:scanner` module later

Suggested future module shape:

- `app` → navigation, screens, feature orchestration
- `pdf` → viewing and annotation
- `pdf-processing` → split/merge/password/text/image/sign/export operations
- `scanner` (optional later) → camera, OpenCV, scan session pipeline, OCR

If you want to move slower, Phase 1 can still start inside `app`, but the document-processing API should be designed as if it will be extracted.

## 3.3 Introduce a unified document domain model

Current model: `RecentFile`

Recommended replacement for cross-feature use:

- `DocumentItem`
  - `id`
  - `displayName`
  - `filePath` and/or persisted `uri`
  - `mimeType`
  - `sizeBytes`
  - `pageCount`
  - `lastOpenedAt`
  - `lastModifiedAt`
  - `sourceType` (`imported`, `scanned`, `created`, `tool_output`)
  - `isPasswordProtected`
  - `thumbnailKey`
  - `tags` / `collection` later

Related supporting models:

- `ToolDefinition`
- `ToolInputSpec` (`single`, `multiple`, `images`, `pdf_only`, `password_required`)
- `ProcessingJob`
- `SignatureAsset`
- `ScanSession`

This is important because the same file list will be reused in:
- `Files` tab
- tool source picker
- sign source picker
- output destination relaunch flow
- scanner output list

---

## 4. Shared flows that should exist before adding many tools

Before implementing many features, build these reusable flows once.

## 4.1 Shared document picker surface

Create a reusable picker component/dialog/screen that supports:

- recent documents section
- search
- single-select / multi-select modes
- file-type filters
- import from system picker
- empty state and missing-file cleanup

Use this in:
- merge
- split
- compress
- sign
- password tools
- extract text
- image export/crop

## 4.2 Shared output flow

Every tool should end with the same output decisions:

- overwrite existing internal copy? usually **no** by default
- save as new file in app storage
- export to SAF destination
- open result immediately
- share result

Current shipped state:
- the single-file result surfaces now follow this pattern with app-private output, open/share actions, and a SAF `Save copy` flow
- folder-style and multi-file outputs such as PDF-to-images, embedded-image extraction, crop-export, and split batches still need richer destination handling and more unified result summaries

## 4.3 Shared background job engine

Use a consistent execution layer for all heavy operations.

Recommended:
- `WorkManager` for resilient jobs that may continue if app goes to background
- foreground notifications for long-running tasks
- in-app progress states for shorter jobs
- cancellation support where possible

This matters especially for:
- merge of large PDFs
- compression
- OCR
- scan PDF generation
- text extraction on big files

## 4.4 Shared error model

Common error categories:
- invalid password
- unsupported/corrupt PDF
- output path unavailable
- insufficient storage
- OOM / large file memory pressure
- permission denied
- job cancelled

---

## 5. Bottom navigation implementation plan

## 5.1 Navigation shell

Use typed routes for the four root destinations:

- `FilesRoute`
- `ToolsRoute`
- `SignRoute`
- `ScannerRoute`

Recommended approach:
- root `Scaffold`
- `NavigationBar` at bottom
- one `NavHost` for the four tabs
- nested routes for subflows such as tool detail, file picker, crop editor, sign placement, scan review

## 5.2 Tab behavior details

### Files tab
Default start destination on app launch.

Subsections:
- Recent
- All imported
- Scanned
- Created by tools

Quick actions per item:
- Open
- Share
- Rename
- Duplicate
- Delete from app
- Use in tool
- Sign

### Tools tab
Grid of tool cards with status badges like:
- ready
- beta
- planned
- premium/future if needed later

Each tool card should define:
- supported input types
- single vs multi file
- output type
- expected runtime
- whether password prompt may appear

### Sign tab
Two main sections:
- `My Signatures`
- `Sign a Document`

The Sign tab should not only be a one-off flow; it should also store reusable signature assets and recent sign jobs.

### Scanner tab
When opened, it should land directly in camera preview if camera permission is already granted.

If permission is not granted:
- show education screen
- request permission
- on deny, show fallback with import images option

---

## 6. Phase 1: PDF management features

Below is the recommended Phase 1 scope, grouped by priority.

## 6.1 Core Phase 1 features

### A. Merge PDFs
**Status:** implemented

**User value:** combine multiple documents into one final PDF.

End-to-end flow:
1. Open `Tools` → `Merge PDFs`
2. Shared document picker in multi-select mode
3. Reorder selected files
4. Optional settings:
   - keep bookmarks later
   - import password for protected inputs
5. Start job
6. Preview result summary
7. Save/export/open/share

Backend notes:
- strong fit for PDFBox-style processing
- handle duplicate filenames and password-protected inputs

### B. Split PDF
**Status:** implemented

Support both:
- split every N pages
- extract custom page ranges
- split by selected pages visually later

End-to-end flow:
1. Select one PDF
2. Choose split mode
3. Preview resulting chunks/page ranges
4. Start job
5. Save all outputs into app folder or user-selected destination

Backend notes:
- strong PDFBox fit
- app should generate a batch result screen

### C. Compress PDF
**Status:** implemented

Compression needs to be honest and profile-based.

Suggested presets:
- `Low compression / best quality`
- `Balanced`
- `Strong compression / small size`
- `Scan cleanup`

End-to-end flow:
1. Select PDF
2. Analyze document first:
   - image-heavy
   - text/vector-heavy
   - mixed
3. Show estimated recommendation
4. Run compression
5. Show before/after size and quality note

Reality check:
- PDFBox can help rewrite PDFs and recompress embedded resources
- PDFBox alone may not deliver excellent size reduction for every PDF type
- for scanned PDFs, image recompression gives the biggest wins
- for digitally-generated PDFs, aggressive compression may have limited effect

### D. Images to PDF (`png`, `jpg`, `jpeg`)
**Status:** implemented

Must support:
- multi-image import
- reordering
- page size choice: A4, Letter, original image size, fit-to-page
- margins/background choice
- optional OCR later

End-to-end flow:
1. Open `Tools` → `Images to PDF`
2. Pick one or more images from gallery/files
3. Reorder pages
4. Choose layout settings
5. Generate PDF
6. Open result in viewer

Backend notes:
- easy with PDFBox or native PDF generation APIs
- should preserve EXIF rotation correctly

### E. Add password / Remove password
**Status:** implemented

End-to-end flow for add password:
1. Select PDF
2. Enter owner/user password rules
3. Optional permissions preset later
4. Save encrypted copy

End-to-end flow for remove password:
1. Select encrypted PDF
2. Prompt for current password
3. Save decrypted copy

Important note:
- based on your note that your PDFBox fork removed some encryption-related pieces for size, this feature must be validated early
- if encryption handlers/providers were stripped, re-add only the minimal standard password support needed for production

### F. Extract text
**Status:** implemented

Modes:
- plain text export
- page-range text export
- copy to clipboard
- save `.txt`
- future: structured extraction with page headers/coordinates

End-to-end flow:
1. Select PDF
2. Optional password prompt
3. Choose all pages or page range
4. Extract
5. View/export/share text

Backend notes:
- PDFBox is a strong fit for basic text extraction
- scanned PDFs need OCR fallback because text extraction alone will return little or nothing

### G. Sign PDF
**Status:** MVP implemented

There are really **three** sign-related capabilities:

1. visual signature drawing with finger/stylus
2. import signature image (`png`) and place it on a page
3. cryptographic digital signature with certificate

For Phase 1, prioritize:
- visual signature drawing
- imported signature image
- visible signature placement

Current shipped MVP covers:
- choose a PDF from the shared picker
- draw a signature in-app
- import a signature image
- choose a page, placement preset, and size
- export a signed copy
- open, share, or save-copy the result

Still pending for the full milestone:
- reusable saved signature assets across sessions
- richer drag/resize/rotate placement editor
- certificate-backed digital signatures

End-to-end flow:
1. Open `Sign` tab
2. Choose a document from recent/shared picker
3. Choose signature source:
   - draw signature
   - import transparent PNG
   - pick saved signature asset
4. Select target page
5. Drag/resize/rotate signature visually
6. Save as signed copy
7. Open result in viewer

Production note:
- visible signature placement is much easier than full certificate-backed digital signing
- certificate-backed signing should be a Phase 1.5 or Phase 2 item unless required immediately

### H. Crop image/area from PDF with visual UI
**Status:** MVP implemented

Clarify expected output:
- crop a visible page region and export as image
- extract embedded images from a PDF
- both

Current shipped MVP behavior:
1. Select PDF
2. Choose all pages or page ranges
3. Pick PNG/JPEG export and a quality level
4. Choose a crop preset or adjust normalized crop percentages
5. Export the cropped region from each selected page as image files

Still pending for the fuller milestone:
- live visual crop box editing on a rendered page preview
- per-page crop overrides
- export cropped regions back into PDF if needed later

Implementation note:
- render page preview with `PdfRenderer`
- keep a robust coordinate mapping between preview space and PDF page space
- if exact embedded-image extraction is desired, PDFBox image extraction is a separate tool and should not be confused with screen-space cropping

---

## 6.2 Highly recommended extra Phase 1 tools

These are useful enough to include in the roadmap now.

### Organize
- Reorder pages
- Delete pages (**implemented**)
- Extract selected pages to new PDF (**implemented**)
- Rotate pages (**implemented**)
- Duplicate pages

### Convert / Export
- PDF to images (**implemented**)
- Extract embedded images (**implemented**)
- Crop visible page region (**implemented MVP**)
- Export page thumbnails/contact sheet

### Cleanup / Productivity
- Add watermark (text **implemented**, image watermark planned)
- Flatten annotations
- Rename document
- Duplicate document
- Document metadata viewer/editor
- Remove metadata

### Reader/utility enhancements
- Favorites/pinned documents
- Recent tool history
- Batch queue support
- Share sheet integration for imports and outputs
- Open from other apps via `ACTION_VIEW` / file intents

---

## 7. Phase 1 library coverage assessment

## 7.1 What the current stack already covers well

### `androidx.pdf` + `pdf-ink`
Good for:
- viewing
- annotation/ink editing
- search inside viewer
- viewer UI and interaction

Not enough for:
- merge/split/reorder/export pipelines
- password add/remove
- text extraction as a standalone tool
- PDF generation workflows
- image extraction and document manipulation
- scanner assembly pipeline

### Android `PdfRenderer`
Good for:
- thumbnails
- rendering pages to bitmap
- crop/export preview workflows
- scan preview/result thumbnails

Not enough for:
- editing/writing PDFs
- merge/split/encryption/text extraction

## 7.2 What a PDFBox-based engine can likely cover

A PDFBox Android fork is the best candidate for most Phase 1 processing features.

Strong fit:
- merge PDFs
- split PDFs
- add/remove pages
- rotate/reorder pages
- image to PDF generation
- password add/remove, if encryption support is present in the fork
- text extraction from text-based PDFs
- extract embedded images
- watermark/stamp workflows
- visible signature appearance composition
- metadata reading/writing

Partial / needs care:
- compression quality and speed
- large-file memory behavior on Android devices
- cryptographic digital signatures on Android
- encryption if parts of the crypto stack were removed from the fork

## 7.3 Where PDFBox is not enough by itself

### Compression
PDF compression is not one thing.

You will likely need:
- image downscaling and JPEG recompression logic
- object/resource deduplication where possible
- separate strategies for scan PDFs vs digital PDFs

### Scanner pipeline
PDFBox is not a camera/scanner library.

You still need:
- camera capture
- edge detection
- perspective correction
- image cleanup
- OCR

### OCR
PDFBox cannot solve OCR. It can only package the results into PDF outputs.

### Certificate-backed digital signing
PDFBox can support PDF signing workflows conceptually, but on Android this usually also requires:
- crypto provider validation
- certificate storage strategy
- secure private key access
- interoperability testing with Acrobat and other readers

## 7.4 Recommended feature-to-library coverage matrix

| Feature / Capability | Primary library / module | Supporting library / platform API | Notes |
| --- | --- | --- | --- |
| PDF viewing, search, annotation ink, save-as | existing `:pdf` module using `androidx.pdf` + `pdf-ink` | SAF (`OpenDocument`, `CreateDocument`) | Keep this viewer-focused; do not move processing into this module. |
| PDF thumbnails and rendered page previews | Android `PdfRenderer` | existing `ThumbnailCache` | Best for page-0 thumbnails, crop previews, and lightweight page-to-bitmap flows. |
| Merge, split, reorder, rotate, extract pages | local `:pdfbox` module | WorkManager for long-running jobs | This is a strong PDFBox fit and should become the base of the processing engine. |
| Images to PDF | local `:pdfbox` module | `androidx.exifinterface:exifinterface` | Use EXIF correction before writing image pages to PDF. |
| PDF to images / page export | `PdfRenderer` for visible pages | local `:pdfbox` module only when embedded-image extraction is needed | `PdfRenderer` is better for page screenshots; PDFBox is better for true embedded image extraction. |
| Page-region crop from PDF with visual UI | `PdfRenderer` + Compose gesture UI | optional `coil-compose` for preview polish | No extra crop library is mandatory for page-region crop; render page, map coordinates, export bitmap region. |
| Embedded image extraction | local `:pdfbox` module | — | This is a PDF parsing task, not a viewer task. |
| Text extraction from text PDFs | local `:pdfbox` module | Room/FTS for indexing later | PDFBox handles text PDFs well. |
| OCR for scans / image PDFs | ML Kit Text Recognition | OpenCV preprocessing | ML Kit is the OCR engine; OpenCV improves image quality before OCR. |
| Compression of scanned PDFs | local `:pdfbox` module | `BitmapFactory`/`ImageDecoder`, JPEG recompression, `ExifInterface` | PDFBox alone is not enough; image recompression is the real win. |
| Add / remove password | local `:pdfbox` module | optional crypto provider additions | Validate that this fork still supports the required encryption flows. |
| Visible signature stamp (draw/import PNG) | local `:pdfbox` module | Compose Canvas / pointer input for signature capture UI | No extra signature library is required if custom Compose capture is acceptable. |
| Certificate-backed digital signatures | local `:pdfbox` module | BouncyCastle, Android Keystore, certificate storage | Treat as advanced scope; requires compatibility testing outside the app. |
| Scanner live preview and capture | CameraX | app camera permission flow | CameraX should own preview, capture, and lifecycle integration. |
| Edge detection / auto crop / perspective correction | OpenCV Android SDK | CameraX analysis pipeline | OpenCV is the right tool for contour detection and transforms. |
| Auto capture heuristics | CameraX + custom analyzer | accelerometer/device motion if needed later | This is app logic, not something PDFBox or the viewer can provide. |
| Scanner PDF export | local `:pdfbox` module or Android `PdfDocument` | ML Kit text results optionally embedded later | Prefer PDFBox for consistency with the broader processing pipeline. |
| Document picker, import/export, URI handling | SAF + `androidx.documentfile:documentfile` | existing document catalog | Needed because many tools will operate on user-selected external files. |
| Long-running tool jobs | WorkManager | foreground notifications | Recommended for merge, compression, OCR, and scan export. |
| Saved signature asset protection | `androidx.security:security-crypto` | app-private storage | Recommended because signatures are sensitive user data. |
| Searchable extracted text index | Room + optional FTS tables | PDFBox / ML Kit outputs | Useful for future in-app content search across files. |

## 7.5 What still needs custom app/UI work

Some capabilities are not primarily a library problem and still need app-level UX implementation:

- bottom navigation and flow orchestration → app Compose/navigation layer
- file picker UX, multi-select UX, reorder UX → shared document picker and tool screens
- crop box editing UI → custom Compose gesture/editor surface
- signature drawing canvas → custom Compose Canvas/pointer input, unless an external signature-pad library is adopted
- page placement editor for signatures/watermarks → custom drag/resize/rotate interaction layer
- tool result summary, batch result screens, overwrite/export/share decisions → app workflow code
- auto-capture confidence heuristics → camera analysis logic tuned on target devices

---

## 8. Additional libraries recommended for production readiness

## 8.1 Camera and scanner
### Required
- `androidx.camera` (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`)

### Recommended
- OpenCV Android SDK for:
  - edge detection
  - contour detection
  - perspective transform
  - cleanup filters
  - document boundary confidence scoring

## 8.2 OCR
Recommended first choice:
- Google ML Kit Text Recognition

Why:
- on-device
- easier Android integration
- lighter productization than bringing a full custom OCR stack first

Possible fallback/advanced option:
- Tesseract, only if you need offline custom language packs/control beyond ML Kit

Recommendation:
- start with ML Kit for Phase 2
- add language pack and accuracy testing before considering Tesseract

## 8.3 Image handling
Likely useful:
- `androidx.exifinterface` for rotation/orientation correctness
- `coil-compose` or equivalent for image previews in Compose

## 8.4 Document/file access
Recommended:
- `androidx.documentfile:documentfile`

Why:
- safer URI-based file handling with SAF
- easier tree/document destination management for exports
- better fit for non-app-private file workflows

## 8.5 Background work
Recommended:
- `androidx.work:work-runtime-ktx`

## 8.6 Cryptography / signing
Depending on the exact PDFBox fork state, you may need:
- BouncyCastle or compatible crypto provider pieces
- Android Keystore integration for future certificate-backed signing
- `androidx.security:security-crypto` for protecting saved signature assets or sensitive local metadata

This should be validated with a small spike before committing to digital signature scope.

## 8.7 Optional helper libraries

These are optional rather than mandatory:

- a reorderable Compose list/grid helper if page or image reordering UX becomes too time-consuming to build manually
- a signature-pad library only if custom Compose signature capture is not smooth enough
- an image crop helper such as `uCrop` only for standalone exported image editing, not for direct PDF page-region crop

---

## 9. Phase 2: Intelligent scanner implementation plan

## 9.1 Scanner goals

Target experience:
- tab opens directly into live camera
- app detects document edges automatically
- app auto-captures when page is stable and well framed
- user can manually correct corners
- multi-page capture is easy
- output becomes a clean PDF
- OCR can make the PDF searchable

## 9.2 Scanner flow end to end

### Flow A: first launch
1. User taps `Scanner`
2. Permission explainer if camera not granted
3. Request camera permission
4. If denied, offer:
   - open settings
   - import existing images instead

### Flow B: live scan capture
1. Full-screen camera preview opens
2. Real-time analyzer detects document contour
3. UI overlays edge polygon
4. When confidence + stability threshold is met:
   - optional auto capture countdown
5. Capture image
6. Show quick review with detected crop
7. User confirms or adjusts corners
8. Image is enhanced and added to session
9. Loop for next page

### Flow C: scan review and export
1. Multi-page review screen shows captured pages
2. Reorder / delete / rotate pages
3. Choose filter preset:
   - color
   - grayscale
   - black & white
   - enhanced document
4. Choose OCR on/off
5. Export to PDF
6. Save to document catalog and open/share

## 9.3 Scanner technical pipeline

Suggested processing stages:

1. CameraX preview + image capture
2. Analysis pipeline on reduced-resolution frames
3. OpenCV:
   - grayscale conversion
   - blur / threshold
   - contour detection
   - quadrilateral selection
   - perspective transform
4. Post-processing:
   - denoise
   - contrast correction
   - shadow reduction if feasible
5. PDF assembly
6. OCR pass
7. Save searchable PDF or sidecar text data

## 9.4 OCR strategy

Recommended Phase 2 behavior:
- OCR is optional per scan session
- default on for documents, off for photos/receipts only if performance becomes an issue
- store extracted text for search indexing later

Possible output levels:
- Level 1: extract text and show/share it
- Level 2: save searchable metadata in app database
- Level 3: embed searchable text layer into exported PDF

Start with Levels 1 and 2 if Level 3 becomes too complex in the first iteration.

## 9.5 Auto capture heuristics

Do not auto capture only because edges are found.
Use combined conditions:
- document contour confidence high enough
- contour stable across N frames
- blur below threshold
- brightness acceptable
- device movement low enough

Always keep manual shutter available.

---

## 10. Sign feature design notes

## 10.1 Signature asset model

Store reusable signatures as separate assets:
- `id`
- `name`
- `type` (`drawn`, `imported_png`)
- `filePath`
- `createdAt`
- `lastUsedAt`
- optional trim bounds / default aspect ratio

## 10.2 Sign UX states

Required screens:
- signature list
- create signature canvas
- import PNG picker
- document picker
- page placement editor
- success/export screen

## 10.3 Placement editor requirements

Must support:
- drag
- pinch resize
- rotate if easy to ship safely
- page switch
- duplicate signature on multiple pages later

## 10.4 Digital signature scope decision

Decide early whether `Sign` means only:
- visible handwritten mark

or also:
- certificate-backed legally verifiable digital signature

Recommended rollout:
- ship visible signing first
- treat certificate signing as a separate milestone with a dedicated spike

---

## 11. Data layer changes needed

## 11.1 Replace one-purpose recent-files storage with a document catalog

Current DB only stores recent files. That is too narrow for the planned app.

Recommended tables/entities:

- `documents`
- `processing_jobs`
- `signature_assets`
- `scan_sessions` or `scan_page_assets`
- `favorites` later if needed

### `documents` example responsibilities
- canonical list of app-managed documents
- source tracking
- thumbnails
- last opened time
- last tool used
- derived metadata like page count and encryption state

## 11.2 Persist SAF access where needed

If files can live outside app-private storage, plan for:
- persisted URI permissions
- URI-based processing entry points
- safe copying to temp working files for tools that require file access

## 11.3 Output naming strategy

Standardize names early, e.g.:
- `originalname_merged_20260425.pdf`
- `originalname_split_p01-p05.pdf`
- `scan_20260425_1432.pdf`
- `originalname_signed.pdf`

---

## 12. Performance and memory strategy

This is critical for production.

## 12.1 General rules
- never load whole large PDFs into memory if avoidable
- use temp working files for write operations
- stream when libraries allow it
- clean up temp files aggressively
- downsample previews separate from export quality

## 12.2 Scanner-specific rules
- analyze low-res preview frames, not full capture frames
- apply heavy transforms only to captured frame
- cap scan resolution profiles to avoid OOM on weaker devices

## 12.3 Compression-specific rules
- expose quality presets instead of one magic button
- show when estimated gains are low
- avoid destructive default behavior

---

## 13. Security and privacy considerations

## 13.1 Password-protected documents
- do not log passwords
- keep decrypted temp files in app-private storage only
- wipe temp files after success/failure when possible
- be explicit when saving an unprotected copy

## 13.2 Scanner privacy
- all capture and OCR should stay on device by default
- if any cloud OCR is ever introduced, it must be opt-in and clearly disclosed

## 13.3 Signatures
- signature assets are sensitive personal data
- store in app-private storage
- consider encrypted storage for saved signature assets

---

## 14. Testing strategy

## 14.1 Unit tests
- page range parser
- naming rules
- tool input validators
- compression preset selection
- signature placement math
- document catalog mapping

## 14.2 Instrumentation / integration tests
- import/open flow
- merge/split happy path
- invalid password path
- scanner permission flow
- OCR result persistence
- open result after tool completion

## 14.3 Golden/manual test set

Build a document test corpus containing:
- small text PDF
- large scanned PDF
- encrypted PDF
- image-heavy PDF
- malformed/corrupt PDF
- rotated pages
- mixed page sizes
- PDFs with annotations

This corpus is required before calling the app production ready.

---

## 15. Recommended implementation order

## Milestone 0 — app shell foundation
- bottom navigation with 4 tabs
- route scaffolding
- document catalog abstraction replacing one-purpose recent list usage
- reusable shared document picker
- reusable output flow

## Milestone 1 — highest-value PDF tools
- merge
- split
- images to PDF
- extract text
- add/remove password spike

## Milestone 2 — signing
- expand the shipped signing MVP with persistent signature assets
- upgrade placement from presets to full drag/resize/rotate editing
- refine signing export and result flows

## Milestone 3 — organize/cleanup tools
- reorder pages
- extend the shipped delete/extract/rotate page flows with richer page picking
- extend the shipped text watermarking MVP with image watermark support and richer placement
- extend the shipped PDF-to-images MVP with richer export destinations and settings
- extend the shipped embedded-image extraction MVP with richer destination/preservation options
- extend the shipped crop-page-region MVP with a visual crop editor and per-page adjustments
- metadata tools
- flatten annotations

## Milestone 4 — intelligent scanner MVP
- CameraX live preview
- manual capture
- OpenCV edge detection
- perspective correction
- multi-page scan export to PDF

## Milestone 5 — scanner advanced
- auto capture
- filters/presets
- OCR text extraction
- searchable scan output
- scanner output indexing in document catalog

## Milestone 6 — advanced security and enterprise polish
- digital certificate signing spike
- permission presets
- better encryption compatibility
- large-file optimization
- background job resilience improvements

---

## 16. Suggested dependency additions

These are the most likely additions once implementation starts.

### Processing core
- local module dependency: `implementation(project(":pdfbox"))`
- keep the existing viewer dependency: `implementation(project(":pdf"))`

Project structure note:
- the project should treat `:pdfbox` as the PDF processing engine module
- the project should treat `:pdf` as the viewer/annotation module

### For app shell and jobs
- `androidx.navigation:navigation-compose`
- `androidx.work:work-runtime-ktx`

### For scanner
- `androidx.camera:camera-core`
- `androidx.camera:camera-camera2`
- `androidx.camera:camera-lifecycle`
- `androidx.camera:camera-view`
- OpenCV Android SDK as a local SDK/AAR integration
- `androidx.exifinterface:exifinterface`

### For OCR
- `com.google.mlkit:text-recognition`

### For image previews/utilities
- `io.coil-kt:coil-compose`
- `androidx.documentfile:documentfile`

### For storage and security
- `androidx.security:security-crypto`

### For advanced signing/encryption compatibility
- optional BouncyCastle provider dependencies if password/signing support requires them

### Already present / keep using
- `androidx.pdf:pdf-viewer-fragment`
- `androidx.pdf:pdf-ink`
- Android `PdfRenderer`

---

## 17. Key technical decisions to validate immediately

These should be answered with quick spikes before large implementation work.

1. **PDFBox fork capability check**
   - verify the local `:pdfbox` submodule builds and stays compatible with AGP/Kotlin upgrades
   - merge
   - split
   - image to PDF
   - text extraction
   - password add/remove
   - visible signature stamping
   - memory behavior on 100MB+ sample docs

2. **Compression realism check**
   - how much size reduction is achievable on-device for:
     - scan PDFs
     - digital text PDFs
     - mixed PDFs

3. **Scanner quality spike**
   - CameraX + OpenCV live edge detection latency on mid-range device
   - manual corner correction UX

4. **Signing scope check**
   - visible signature only vs certificate-backed signing

5. **OCR choice check**
   - ML Kit accuracy/speed on your target languages and document types

---

## 18. Recommended first deliverable after this doc

After approving this plan, the best next implementation step is:

1. add bottom navigation shell
2. refactor current recent-files flow into a reusable `DocumentCatalog`
3. add a shared file picker surface used by `Files`, `Tools`, and `Sign`
4. integrate your PDFBox fork behind a `PdfProcessingEngine` interface
5. implement first three tools: merge, split, images-to-PDF

That sequence gives the app the right foundation instead of shipping each feature as a one-off flow.

---

## 19. Short recommendation summary

### Use existing stack for
- viewing
- annotation editing
- thumbnails/previews

### Use your PDFBox fork for
- merge/split/page operations
- image-to-PDF
- text extraction
- password tools if crypto support is present
- watermark/stamping/export tasks
- embedded image extraction
- visible signature composition

### Add new libraries for
- CameraX → camera experience
- OpenCV → edge detection and scan cleanup
- ML Kit → OCR
- WorkManager → resilient long-running jobs
- DocumentFile → SAF-based import/export flows
- Security Crypto → protect saved signature assets

### Ship order
- foundation first
- high-value tools next
- signing next
- scanner MVP after that

This keeps the app coherent, reusable, and production-ready instead of growing as a set of disconnected utility screens.

