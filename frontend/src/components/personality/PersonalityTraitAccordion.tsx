import type { PersonalityTraitDto } from '../../api/types'
import {
  LABEL_BAD_DAY,
  LABEL_GOOD_DAY,
  LABEL_SUCCEED_THROUGH,
  LABEL_TOP_STRENGTH,
} from './personalityLabels'
import { PersonalityTraitScale } from './PersonalityTraitScale'

interface PersonalityTraitAccordionProps {
  trait: PersonalityTraitDto
  isOpen: boolean
  onToggle: () => void
}

export function PersonalityTraitAccordion({
  trait,
  isOpen,
  onToggle,
}: PersonalityTraitAccordionProps) {
  return (
    <div className="rounded-lg border border-neutral-200 bg-neutral-50">
      <button
        type="button"
        onClick={onToggle}
        aria-expanded={isOpen}
        className="flex w-full cursor-pointer items-start justify-between gap-3 p-4 text-left"
      >
        <div className="flex min-w-0 flex-1 flex-wrap items-center gap-2">
          <span className="text-sm font-semibold text-neutral-900">{trait.label}</span>
          {trait.isTopStrength === true && (
            <span className="shrink-0 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-900">
              {LABEL_TOP_STRENGTH}
            </span>
          )}
        </div>
        <span
          className={`mt-0.5 shrink-0 text-neutral-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          aria-hidden
        >
          ▾
        </span>
      </button>
      {isOpen && (
        <div className="border-t border-neutral-200 px-4 pb-4 pt-3">
          <p className="text-sm text-neutral-700">{trait.details.description}</p>
          <PersonalityTraitScale
            leftPole={trait.leftPole}
            rightPole={trait.rightPole}
            scalePosition={trait.scalePosition}
          />
          <div className="mt-4 flex flex-col gap-3 text-sm">
            <div className="flex flex-col gap-3 md:flex-row md:gap-4">
              <div className="min-w-0 flex-1">
                <p className="font-medium text-neutral-800">{LABEL_GOOD_DAY}</p>
                <p className="mt-0.5 text-neutral-600">{trait.details.goodDay}</p>
              </div>
              <div className="min-w-0 flex-1">
                <p className="font-medium text-neutral-800">{LABEL_BAD_DAY}</p>
                <p className="mt-0.5 text-neutral-600">{trait.details.badDay}</p>
              </div>
            </div>
            <div>
              <p className="font-medium text-neutral-800">{LABEL_SUCCEED_THROUGH}</p>
              <ul className="mt-1 list-disc space-y-0.5 pl-5 text-neutral-600">
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
