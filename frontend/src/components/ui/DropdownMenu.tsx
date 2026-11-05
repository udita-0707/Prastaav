import clsx from 'clsx';
import { MoreVertical } from 'lucide-react';
import { useEffect, useId, useLayoutEffect, useRef, useState, type CSSProperties, type KeyboardEvent, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { useNavigate } from 'react-router-dom';

export interface MenuItem {
  label: string;
  icon?: ReactNode;
  /** Either a route to open or a callback. */
  to?: string;
  onSelect?: () => void;
}

interface DropdownMenuProps {
  items: MenuItem[];
  /** Accessible name of the trigger, e.g. "Actions for Smart Attendance". */
  label: string;
  trigger?: ReactNode;
  align?: 'left' | 'right';
  triggerClassName?: string;
}

const VIEWPORT_GAP = 8;

/**
 * Menu button pattern: Enter, Space or ArrowDown opens; arrows move; Escape closes and returns focus.
 * The menu is drawn on the body with fixed coordinates, because a table's scroll container would clip it.
 */
export function DropdownMenu({ items, label, trigger, align = 'right', triggerClassName }: DropdownMenuProps) {
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const [position, setPosition] = useState<CSSProperties>({ top: 0, left: 0 });
  const menu = useRef<HTMLDivElement>(null);
  const menuId = useId();
  const root = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const itemRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    if (!open) return;
    const onPointer = (event: PointerEvent) => {
      const target = event.target as Node;
      if (!root.current?.contains(target) && !menu.current?.contains(target)) setOpen(false);
    };
    const dismiss = () => setOpen(false);
    document.addEventListener('pointerdown', onPointer);
    window.addEventListener('scroll', dismiss, true);
    window.addEventListener('resize', dismiss);
    return () => {
      document.removeEventListener('pointerdown', onPointer);
      window.removeEventListener('scroll', dismiss, true);
      window.removeEventListener('resize', dismiss);
    };
  }, [open]);

  // Below the trigger when it fits, otherwise above it; always inside the viewport.
  useLayoutEffect(() => {
    if (!open || !button.current || !menu.current) return;
    const anchor = button.current.getBoundingClientRect();
    const { width, height } = menu.current.getBoundingClientRect();
    const below = anchor.bottom + VIEWPORT_GAP;
    const top = below + height > window.innerHeight ? Math.max(VIEWPORT_GAP, anchor.top - VIEWPORT_GAP - height) : below;
    const wanted = align === 'right' ? anchor.right - width : anchor.left;
    setPosition({ top, left: Math.min(Math.max(VIEWPORT_GAP, wanted), window.innerWidth - width - VIEWPORT_GAP) });
  }, [open, align]);

  useEffect(() => {
    if (open) itemRefs.current[active]?.focus({ preventScroll: true });
  }, [open, active]);

  const close = () => {
    setOpen(false);
    button.current?.focus();
  };

  const choose = (item: MenuItem) => {
    setOpen(false);
    if (item.to) navigate(item.to);
    item.onSelect?.();
  };

  const onMenuKey = (event: KeyboardEvent) => {
    const last = items.length - 1;
    if (event.key === 'ArrowDown') setActive((i) => (i === last ? 0 : i + 1));
    else if (event.key === 'ArrowUp') setActive((i) => (i === 0 ? last : i - 1));
    else if (event.key === 'Home') setActive(0);
    else if (event.key === 'End') setActive(last);
    else if (event.key === 'Escape') close();
    else if (event.key === 'Tab') close();
    else return;
    if (event.key !== 'Tab') event.preventDefault();
  };

  if (items.length === 0) return null;

  return (
    <div ref={root} className="relative inline-block">
      <button
        ref={button}
        type="button"
        aria-label={label}
        aria-haspopup="menu"
        aria-expanded={open}
        aria-controls={open ? menuId : undefined}
        onClick={() => {
          setActive(0);
          setOpen((o) => !o);
        }}
        onKeyDown={(event) => {
          if (event.key === 'ArrowDown') {
            event.preventDefault();
            setActive(0);
            setOpen(true);
          }
        }}
        className={clsx(trigger ? triggerClassName : 'inline-flex h-9 w-9 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100 hover:text-ink', triggerClassName)}
      >
        {trigger ?? <MoreVertical className="h-4 w-4" aria-hidden />}
      </button>
      {open &&
        createPortal(
          <div ref={menu} id={menuId} role="menu" aria-label={label} onKeyDown={onMenuKey} style={position} className="fixed z-50 min-w-[11rem] rounded-xl border border-line bg-white p-1.5 shadow-pop">
            {items.map((item, index) => (
              <button
                key={item.label}
                ref={(el) => (itemRefs.current[index] = el)}
                role="menuitem"
                type="button"
                tabIndex={index === active ? 0 : -1}
                onClick={() => choose(item)}
                onMouseEnter={() => setActive(index)}
                className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left text-sm font-medium text-ink hover:bg-slate-50 focus:bg-slate-50 focus:outline-none"
              >
                {item.icon && <span className="text-slate-500">{item.icon}</span>}
                {item.label}
              </button>
            ))}
          </div>,
          document.body,
        )}
    </div>
  );
}
