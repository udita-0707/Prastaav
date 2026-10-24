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

Mockup content that has no backing in the code is left out or reworded: fake statistics, testimonials, "trusted by" numbers, social icons, "Watch Demo", "Forgot password?", "Learn more" links, deadline widgets and month-over-month deltas. People appear as initials avatars, never as photos.
