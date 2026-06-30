import {useEffect, useMemo, useState} from 'react'
import {useTranslation} from 'react-i18next'
import type {PersonalityCategoryDto} from '../../api/types'
import {PersonalityTraitAccordion} from './PersonalityTraitAccordion'
import {CATEGORY_ORDER, categoryLabel} from './personalityLabels'

interface PersonalityCategoryTabsProps {
  categories: PersonalityCategoryDto[]
}

export function PersonalityCategoryTabs({categories}: PersonalityCategoryTabsProps) {
  const {t} = useTranslation()
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
    <section className="rounded-[var(--radius-card)] border border-border-subtle bg-surface p-5 card-shadow sm:p-6">
      <div
        className="flex flex-wrap gap-1.5 border-b border-border-subtle pb-4"
        role="tablist"
        aria-label={t('seeker.personality.categoryTabsAriaLabel')}
      >
        {ordered.map((category) => {
          const isActive = category.key === active.key
          const label = categoryLabel(category.key, t)
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
                  : 'rounded-full px-4 py-2 text-sm font-medium text-muted hover:bg-surface-muted hover:text-brand-700 dark:hover:text-brand-400'
              }
            >
              {label}
            </button>
          )
        })}
      </div>
      <div role="tabpanel" className="mt-5">
        <h2 className="text-lg font-semibold text-foreground">
          {categoryLabel(active.key, t)}
        </h2>
        <p className="mt-2 text-sm text-muted">{active.description}</p>
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
