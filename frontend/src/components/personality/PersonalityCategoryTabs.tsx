import { useEffect, useMemo, useState } from 'react'
import type { PersonalityCategoryDto } from '../../api/types'
import { PersonalityTraitAccordion } from './PersonalityTraitAccordion'
import { CATEGORY_LABELS, CATEGORY_ORDER } from './personalityLabels'

interface PersonalityCategoryTabsProps {
  categories: PersonalityCategoryDto[]
}

export function PersonalityCategoryTabs({ categories }: PersonalityCategoryTabsProps) {
  const ordered = useMemo(() => {
    const byKey = new Map(categories.map((c) => [c.key, c]))
    return CATEGORY_ORDER.map((key) => byKey.get(key)).filter(
      (c): c is PersonalityCategoryDto => c != null,
    )
  }, [categories])

  const [activeKey, setActiveKey] = useState(ordered[0]?.key ?? '')
  const [expandedTraitKey, setExpandedTraitKey] = useState('')

  const active = ordered.find((c) => c.key === activeKey) ?? ordered[0]

  useEffect(() => {
    const category = ordered.find((c) => c.key === activeKey) ?? ordered[0]
    setExpandedTraitKey(category?.traits[0]?.key ?? '')
  }, [activeKey, ordered])

  if (ordered.length === 0 || active == null) return null

  return (
    <section className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm">
      <div
        className="flex flex-wrap gap-1 border-b border-neutral-200 pb-3"
        role="tablist"
        aria-label="Разделы личностного профиля"
      >
        {ordered.map((category) => {
          const isActive = category.key === active.key
          const label = CATEGORY_LABELS[category.key] ?? category.key
          return (
            <button
              key={category.key}
              type="button"
              role="tab"
              aria-selected={isActive}
              onClick={() => setActiveKey(category.key)}
              className={
                isActive
                  ? 'rounded-lg bg-neutral-900 px-4 py-2 text-sm font-medium text-white'
                  : 'rounded-lg px-4 py-2 text-sm font-medium text-neutral-600 hover:bg-neutral-100'
              }
            >
              {label}
            </button>
          )
        })}
      </div>
      <div role="tabpanel" className="mt-4">
        <h2 className="text-lg font-semibold">
          {CATEGORY_LABELS[active.key] ?? active.key}
        </h2>
        <p className="mt-2 text-sm text-neutral-600">{active.description}</p>
        <div className="mt-4 flex flex-col gap-3">
          {active.traits.map((trait) => (
            <PersonalityTraitAccordion
              key={trait.key}
              trait={trait}
              isOpen={expandedTraitKey === trait.key}
              onToggle={() =>
                setExpandedTraitKey((current) => (current === trait.key ? '' : trait.key))
              }
            />
          ))}
        </div>
      </div>
    </section>
  )
}
