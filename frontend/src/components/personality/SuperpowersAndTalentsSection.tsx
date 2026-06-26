import type {SuperpowerAndTalentDto} from '../../api/types'
import {FormSection} from '../FormSection'
import {LABEL_PRONOUNCED} from './personalityLabels'

interface SuperpowersAndTalentsSectionProps {
  items: SuperpowerAndTalentDto[]
}

export function SuperpowersAndTalentsSection({items}: SuperpowersAndTalentsSectionProps) {
  return (
    <FormSection title="Суперсилы и таланты">
      <ul className="flex flex-wrap gap-2">
        {items.map((item) => (
          <li
            key={item.id}
            className="inline-flex items-center gap-2 rounded-full border border-brand-200 bg-brand-50 px-3 py-2"
          >
            <span className="text-sm font-medium text-stone-900">{item.name}</span>
            {item.isPronounced === true && (
              <span className="shrink-0 rounded-full bg-brand-200 px-2 py-0.5 text-xs font-semibold text-brand-800">
                {LABEL_PRONOUNCED}
              </span>
            )}
          </li>
        ))}
      </ul>
    </FormSection>
  )
}
