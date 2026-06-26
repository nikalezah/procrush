import {useEffect, useMemo, useState} from 'react'
import type {PersonalityCategoryDto} from '../../api/types'
import {PersonalityTraitAccordion} from './PersonalityTraitAccordion'
import {CATEGORY_LABELS, CATEGORY_ORDER} from './personalityLabels'

interface PersonalityCategoryTabsProps {
  categories: PersonalityCategoryDto[]
}

export function PersonalityCategoryTabs({categories}: PersonalityCategoryTabsProps) {
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
    <section className="rounded-[var(--radius-card)] border border-brand-100/60 bg-white p-5 card-shadow sm:p-6">
      <div
        className="flex flex-wrap gap-1.5 border-b border-brand-100 pb-4"
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
                  ? 'gradient-brand rounded-full px-4 py-2 text-sm font-medium text-white shadow-sm shadow-brand-500/20'
                  : 'rounded-full px-4 py-2 text-sm font-medium text-stone-600 hover:bg-brand-50 hover:text-brand-700'
              }
            >
              {label}
            </button>
          )
        })}
      </div>
      <div role="tabpanel" className="mt-5">
        <h2 className="text-lg font-semibold text-stone-900">
          {CATEGORY_LABELS[active.key] ?? active.key}
        </h2>
        <p className="mt-2 text-sm text-stone-500">{active.description}</p>
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
