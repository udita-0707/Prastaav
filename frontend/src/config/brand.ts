/** User-facing product identity. The only place the product name and tagline are written. */
export const PRODUCT_NAME = 'Prastaav';
export const TAGLINE = 'Ideas → Proposals → Impact';

/** Shown in the landing page footer; set VITE_INSTITUTION_NAME to your institution. */
export const INSTITUTION_NAME = import.meta.env.VITE_INSTITUTION_NAME?.trim() || 'Your University';

/** Must match prastav.institution.email-domain on the backend; set VITE_INSTITUTION_EMAIL_DOMAIN to change it. */
export const INSTITUTION_EMAIL_DOMAIN = import.meta.env.VITE_INSTITUTION_EMAIL_DOMAIN?.trim() || 'nst.rishihood.edu.in';
