import type { SuperpowerAndTalentDto } from '../../api/types'
import { FormSection } from '../FormSection'
import { LABEL_PRONOUNCED } from './personalityLabels'

interface SuperpowersAndTalentsSectionProps {
  items: SuperpowerAndTalentDto[]
}

export function SuperpowersAndTalentsSection({ items }: SuperpowersAndTalentsSectionProps) {
  return (
    <FormSection title="Суперсилы и таланты">
      <ul className="flex flex-wrap gap-2">
        {items.map((item) => (
          <li
            key={item.id}
            className="inline-flex items-center gap-2 rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2"
          >
            <span className="text-sm font-medium text-neutral-900">{item.name}</span>
            {item.isPronounced === true && (
              <span className="shrink-0 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-900">
                {LABEL_PRONOUNCED}
              </span>
            )}
          </li>
        ))}
      </ul>
    </FormSection>
  )
}
