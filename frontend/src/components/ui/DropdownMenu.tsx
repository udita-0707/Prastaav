import clsx from 'clsx';
import { MoreVertical } from 'lucide-react';
import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
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

/** Menu button pattern: Enter, Space or ArrowDown opens; arrows move; Escape closes and returns focus. */
export function DropdownMenu({ items, label, trigger, align = 'right', triggerClassName }: DropdownMenuProps) {
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const menuId = useId();
  const root = useRef<HTMLDivElement>(null);
  const button = useRef<HTMLButtonElement>(null);
  const itemRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const navigate = useNavigate();

  useEffect(() => {
    if (!open) return;
    const onPointer = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('pointerdown', onPointer);
    return () => document.removeEventListener('pointerdown', onPointer);
  }, [open]);

  useEffect(() => {
    if (open) itemRefs.current[active]?.focus();
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
    else if (event.key === 'Tab') setOpen(false);
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
      {open && (
        <div
          id={menuId}
          role="menu"
          aria-label={label}
          onKeyDown={onMenuKey}
          className={clsx('absolute z-30 mt-2 min-w-[11rem] rounded-xl border border-line bg-white p-1.5 shadow-pop', align === 'right' ? 'right-0' : 'left-0')}
        >
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
        </div>
      )}
    </div>
  );
}
