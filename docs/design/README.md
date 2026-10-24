# Prastaav design notes

The six PNGs in this folder are the reference mockups for the web app. Names, numbers, dates and photos in them are sample content; the app renders real data from the API.

| File | Screen | Route |
| --- | --- | --- |
| `01-login.png` | Sign in | `/login` |
| `02-student-home.png` | Student home | `/app` (student) |
| `03-create-project.png` | Create project wizard, step 1 | `/app/projects/new` |
| `04-coordinator-projects.png` | All projects (coordinator) | `/app/projects` (coordinator) |
| `05-faculty-home.png` | Faculty home | `/app` (faculty) |
| `06-landing.png` | Public landing page | `/` |

## Visual language

**Colour.** A light, airy palette: a pale blue-grey page (`#F5F9FC`) washed with soft teal and blue radial gradients, white cards, navy headings (`#0B1F4D`) and slate body text (`#64748B`). Teal (`#0F9D8F`) is the primary action colour; blue (`#2563EB`) is the accent for links and secondary emphasis. The brand gradient runs teal to blue and is used sparingly: the main sign-in button and the highlighted second line of hero headings.

**Shape and depth.** Cards have a 16px radius, a 1px `#E2E8F0` border and a very soft, wide shadow. Inputs and buttons use a 12px radius. Icon tiles are rounded squares or circles with a pale tint of the icon colour.

**Type.** Plus Jakarta Sans throughout. Page titles are bold and large (about 32 to 36px), card titles semibold (18 to 20px), table text 14px, captions and table headers 12 to 13px in slate. Eyebrow labels ("WELCOME BACK", "WHY PRASTAAV") are small, uppercase and letter-spaced in teal.

**Icons.** Outline icons with rounded joins at a 1.5 to 2px stroke, placed inside tinted tiles on stat cards and table rows.

**Status pills.** Fully rounded, tinted background with a darker text and a leading icon. One colour per status: Draft amber, Submitted sky, Under Review violet, Changes Requested rose, Approved green, Rejected slate.

**Tables.** Header row on a pale tinted band, generous row height (about 64px), thin dividers, a leading icon tile per row, a light outline "View" button and a kebab menu in the actions column.

**Layout.** A 260px white sidebar with the logo, role navigation (active item on a pale teal pill) and a user card at the bottom; a top bar with a wide search field, a bell and the user menu. Content sits on a 1440px desktop canvas with 24 to 32px gutters. Dashboards open with a greeting and a primary action, then a row of stat cards, then a table card, then a call-to-action band with an illustration.

## Content rules

Mockup content that has no backing in the code is left out or reworded: fake statistics, testimonials, "trusted by" numbers, social icons, "Watch Demo", "Forgot password?", "Learn more" links, deadline widgets and month-over-month deltas. People appear as initials avatars, never as photos. The notification bell and a Notifications page are left out because the API has no notification endpoints.

## Design tokens

The tokens live in `frontend/tailwind.config.js` and are used as Tailwind classes (`bg-brand-600`, `text-ink`, `rounded-card`, `shadow-card`, …).

| Token | Value | Use |
| --- | --- | --- |
| `brand-500` (`brand`) | `#0F9D8F` | Primary teal: icons, focus ring, highlights |
| `brand-600` | `#0C8478` | Primary button background and teal text (AA on white) |
| `brand-700` | `#0A6B62` | Primary button hover, eyebrow labels, active navigation text |
| `brand-50` … `brand-900` | teal scale | Tints for tiles, active navigation, the CTA band |
| `accent` (`accent-600`) | `#2563EB` | Accent blue for links and outline buttons |
| `ink` | `#0B1F4D` | Headings and body text |
| `muted` | `#64748B` | Secondary text |
| `line` | `#E2E8F0` | Borders and dividers |
| `canvas` | `#F5F9FC` | Page background, with the `bg-canvas-glow` radial gradients |
| `bg-brand-gradient` | `#0C8478` → `#2563EB` | Main sign-in and submit buttons |
| `bg-brand-text` | `#0F9D8F` → `#2563EB` | Highlighted second line of hero headings |
| `rounded-card` / `rounded-field` | 16px / 12px | Cards / inputs and buttons |
| `shadow-card` / `shadow-pop` | soft, wide shadows | Cards / menus, dialogs, toasts |
| `shadow-focus` | 4px teal ring at 18% | Focused inputs |
| `font-sans` | Plus Jakarta Sans Variable | Every text; self-hosted through `@fontsource-variable/plus-jakarta-sans` |

Buttons and teal text use the 600 and 700 steps rather than `#0F9D8F` itself, because white text on `#0F9D8F` is below the WCAG AA contrast ratio for normal text.

### Status pills

`StatusPill` holds the only status-to-colour mapping: Draft amber, Submitted sky, Under Review violet, Changes Requested rose (`REVISION_REQUESTED`), Approved green, Rejected slate. Each pill is a tinted background with a darker text step chosen for AA contrast, plus an icon, so the colour is never the only signal.

## Components

Shared components live in `frontend/src/components/ui` and are exported from its `index.ts`.

| Component | Purpose |
| --- | --- |
| `Button`, `ButtonLink` | Variants `primary`, `gradient`, `outline`, `ghost`, `danger`; sizes `sm`, `md`, `lg`; `loading` shows a spinner and disables the button |
| `Input`, `Textarea`, `Select` | Label, hint, error (announced), required marker, optional leading icon and live character counter |
| `Card`, `CardHeader` | White surface with the card radius and shadow; header with title, description and an action |
| `StatCard`, `IconTile` | Headline number with a tinted icon tile; optionally links to the list it counts |
| `StatusPill` | Project status (see above) |
| `Avatar`, `AvatarStack` | Initials with a colour derived from the name; overlapping stack with "+N" |
| `DataTable` | Sticky header, horizontal scroll on narrow screens, skeleton rows, empty state |
| `Pagination`, `FilterBar`, `SearchField` | List paging and the toolbar above tables |
| `Tabs`, `TabPanel` | WAI-ARIA tabs with arrow-key navigation |
| `Stepper` | Accessible wizard progress (`aria-current="step"`); completed steps are clickable |
| `DropdownMenu` | Menu button (kebab or custom trigger) with keyboard navigation |
| `Modal` | Dialog with focus trap, Escape and backdrop to close |
| `ToastProvider`, `useToast` | Success, info and error toasts in live regions |
| `Skeleton`, `EmptyState` | Loading placeholders and empty lists |
| `PageHeader`, `CtaBand`, `Logo` | Page title block, call-to-action band, logo with optional tagline |

Domain components in `frontend/src/components` (`WorkflowRail`, `ReviewPanel`, `AssignReviewerPanel`, `ProposalFields`, `ErrorNote`) are built from these.

## Replacing images

Every image is referenced from `frontend/src/assets/index.ts`. To replace one, overwrite the file with the same name (an SVG keeps the same import; for a PNG or WebP change only the import line in `index.ts`). The list of files, their sizes and where each appears is in [`ASSETS.md`](ASSETS.md). The current files are clean SVG placeholders in the brand palette; no artwork was taken from the mockups.
