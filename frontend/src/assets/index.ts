// Every image the app shows is referenced from here, so a file can be replaced without touching components.
// Files under public/brand are served as-is; illustrations are bundled by Vite. See docs/design/ASSETS.md.
import assignFaculty from './illustrations/assign-faculty.svg';
import coordinatorLaptop from './illustrations/coordinator-laptop.svg';
import emptyProjects from './illustrations/empty-projects.svg';
import facultyLaptop from './illustrations/faculty-laptop.svg';
import ideaBulb from './illustrations/idea-bulb.svg';
import mountainLake from './illustrations/mountain-lake.svg';
import sidebarLeaves from './illustrations/sidebar-leaves.svg';
import studentLaptop from './illustrations/student-laptop.svg';
import welcomeBanner from './illustrations/welcome-banner.svg';

export const brand = {
  logo: '/brand/logo.svg',
  logoMark: '/brand/logo-mark.svg',
  favicon: '/brand/favicon.svg',
} as const;

export const illustrations = {
  studentLaptop,
  facultyLaptop,
  coordinatorLaptop,
  ideaBulb,
  assignFaculty,
  emptyProjects,
  welcomeBanner,
  mountainLake,
  sidebarLeaves,
} as const;
