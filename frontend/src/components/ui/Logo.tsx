import clsx from 'clsx';
import { brand } from '../../assets';
import { PRODUCT_NAME, TAGLINE } from '../../config/brand';

export function Logo({ withTagline = false, size = 'md', className }: { withTagline?: boolean; size?: 'md' | 'lg'; className?: string }) {
  return (
    <span className={clsx('inline-flex items-center gap-2.5', className)}>
      <img src={brand.logoMark} alt="" className={size === 'lg' ? 'h-11 w-11' : 'h-9 w-9'} />
      <span className="flex flex-col leading-none">
        <span className={clsx('font-bold tracking-tight text-ink', size === 'lg' ? 'text-[1.75rem]' : 'text-[1.4rem]')}>{PRODUCT_NAME}</span>
        {withTagline && <span className="mt-1 text-[11px] font-medium text-muted">{TAGLINE}</span>}
      </span>
    </span>
  );
}
