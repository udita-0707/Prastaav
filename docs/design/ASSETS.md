# Image assets

All images are referenced from `frontend/src/assets/index.ts`, so a file can be replaced without touching components. The current files are SVG placeholders drawn in the brand palette. People are never shown as photos or faces; the app uses initials avatars.

## Brand (`frontend/public/brand/`, served as-is)

| File | Size | Used in |
| --- | --- | --- |
| `logo.svg` | 168 × 40 | Full logo with wordmark, for documents and external use (the app renders the mark plus live text through `Logo`) |
| `logo-mark.svg` | 40 × 40 | `Logo` component (sidebar, auth pages, landing header), landing product preview |
| `favicon.svg` | 40 × 40 | Browser tab icon (`index.html`) |

## Illustrations (`frontend/src/assets/illustrations/`, bundled by Vite)

| File | Size | Used in |
| --- | --- | --- |
| `student-laptop.svg` | 320 × 240 | Sign-in and registration side panel, student home header, landing Student card |
| `faculty-laptop.svg` | 320 × 240 | Landing Faculty card |
| `coordinator-laptop.svg` | 320 × 240 | Coordinator home and All Projects headers, landing Coordinator card |
| `idea-bulb.svg` | 200 × 160 | "Have a new idea?" band on the student home, landing About section |
| `assign-faculty.svg` | 280 × 160 | "Need to assign faculty?" bands, Assign Faculty page header and empty state |
| `empty-projects.svg` | 200 × 150 | Default `EmptyState` image |
| `welcome-banner.svg` | 360 × 180 | Faculty home welcome banner |
| `mountain-lake.svg` | 400 × 300 | Faculty home quote card (cropped to cover) |
| `sidebar-leaves.svg` | 260 × 200 | Decoration at the bottom of the sidebar |

## Animations (`frontend/src/assets/animations/`, bundled by Vite)

| File | Size | Used in |
| --- | --- | --- |
| `review.mp4` | 512 × 512, 2.5 s loop, 136 KB | Landing page, How it works, "Review" step tile. Plays muted and inline; its white background is multiplied into the tile tint; it stays on its first frame for visitors who prefer reduced motion |

## Still to replace

Every brand and illustration file above is a placeholder; `review.mp4` is final artwork. Final artwork should keep the file names and roughly the same aspect ratios; keep the background transparent so the illustrations sit on the tinted page.
