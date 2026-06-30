import {useTranslation} from 'react-i18next'
import type {PersonalityTraitDto} from '../../api/types'
import {labelBadDay, labelGoodDay, labelSucceedThrough, labelTopStrength,} from './personalityLabels'
import {PersonalityTraitScale} from './PersonalityTraitScale'

interface PersonalityTraitAccordionProps {
  trait: PersonalityTraitDto
  isOpen: boolean
  onToggle: () => void
}

export function PersonalityTraitAccordion({trait, isOpen, onToggle}: PersonalityTraitAccordionProps) {
  const {t} = useTranslation()

  return (
    <div className="rounded-2xl border border-border-subtle bg-surface-muted">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={isOpen}
        className="flex w-full cursor-pointer items-start justify-between gap-3 p-4 text-left"
      >
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2">
          <span className="text-sm font-semibold text-foreground">{trait.label}</span>
          {trait.isTopStrength === true && (
            <span className="shrink-0 rounded-full bg-surface-elevated px-2.5 py-0.5 text-xs font-semibold text-brand-800 dark:text-brand-300">
              {labelTopStrength(t)}
            </span>
          )}
        </div>
        <span
          className={`mt-0.5 shrink-0 text-brand-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          aria-hidden
        >
          ▾
        </span>
      </button>
      {isOpen && (
        <div className="border-t border-border-subtle px-4 pb-4 pt-3">
          <p className="text-sm leading-relaxed text-foreground">{trait.details.description}</p>
          <PersonalityTraitScale
            leftPole={trait.leftPole}
            rightPole={trait.rightPole}
            scalePosition={trait.scalePosition}
          />
          <div className="mt-4 flex flex-col gap-3 text-sm">
            <div className="flex flex-col gap-3 md:flex-row md:gap-4">
              <div className="min-w-0 flex-1 rounded-xl border border-border-subtle bg-surface p-3">
                <p className="font-medium text-foreground">{labelGoodDay(t)}</p>
                <p className="mt-0.5 text-muted">{trait.details.goodDay}</p>
              </div>
              <div className="min-w-0 flex-1 rounded-xl border border-border-subtle bg-surface p-3">
                <p className="font-medium text-foreground">{labelBadDay(t)}</p>
                <p className="mt-0.5 text-muted">{trait.details.badDay}</p>
              </div>
            </div>
            <div className="rounded-xl border border-border-subtle bg-surface p-3">
              <p className="font-medium text-foreground">{labelSucceedThrough(t)}</p>
              <ul className="mt-1 list-disc space-y-0.5 pl-5 text-muted">
                {[
                  trait.details.succeedThrough.point0,
                  trait.details.succeedThrough.point1,
                  trait.details.succeedThrough.point2,
                ].map((item) => (
                  <li key={item}>{item}</li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
