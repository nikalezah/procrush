import { useEffect, useMemo, useState } from 'react'

const AXES_COUNT = 6
const GRID_LEVELS = [0.25, 0.5, 0.75, 1]
const DESKTOP_MEDIA_QUERY = '(min-width: 768px)'
const LABEL_LINE_GAP = 14
const COS_30 = Math.sqrt(3) / 2
const SIN_30 = 0.5

interface DiscHexagonChartProps {
  labels: string[]
  values: number[]
}

interface ChartLayout {
  cx: number
  cy: number
  maxRadius: number
  rimGap: number
  viewBoxPadding: number
  textHalfHeight: number
  sideTextExtend: number
  containerClassName: string
  svgClassName: string
  labelClassName: string
  percentClassName: string
  lockRenderWidth: boolean
}

const MOBILE_LAYOUT: ChartLayout = {
  cx: 172,
  cy: 162,
  maxRadius: 86,
  rimGap: 6,
  viewBoxPadding: 4,
  textHalfHeight: 6,
  sideTextExtend: 76,
  containerClassName: '-mx-4 w-[calc(100%+2rem)]',
  svgClassName: 'h-auto w-full',
  labelClassName: 'fill-neutral-600 text-[10px]',
  percentClassName: 'fill-neutral-900 text-[10px] font-semibold tabular-nums',
  lockRenderWidth: false,
}

const DESKTOP_LAYOUT: ChartLayout = {
  cx: 230,
  cy: 180,
  maxRadius: 76,
  rimGap: 10,
  viewBoxPadding: 4,
  textHalfHeight: 7,
  sideTextExtend: 86,
  containerClassName: '-mx-1 w-full',
  svgClassName: 'h-auto max-w-full shrink-0',
  labelClassName: 'fill-neutral-600 text-[11px]',
  percentClassName: 'fill-neutral-900 text-[11px] font-semibold tabular-nums',
  lockRenderWidth: true,
}

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

function getVertex(index: number, radius: number, cx: number, cy: number, count: number) {
  const angle = (Math.PI * 2 * index) / count - Math.PI / 2
  return {
    x: cx + radius * Math.cos(angle),
    y: cy + radius * Math.sin(angle),
    angle,
  }
}

function getLabelPoint(percentPoint: { x: number; y: number; angle: number }): {
  x: number
  y: number
} {
  const verticalOffset = Math.sin(percentPoint.angle) < 0 ? -LABEL_LINE_GAP : LABEL_LINE_GAP
  return {
    x: percentPoint.x,
    y: percentPoint.y + verticalOffset,
  }
}

function getTightViewBox(layout: ChartLayout) {
  const { cx, cy, maxRadius, rimGap, viewBoxPadding, textHalfHeight, sideTextExtend } = layout
  const percentRadius = maxRadius + rimGap

  const minY = cy - percentRadius - LABEL_LINE_GAP - textHalfHeight
  const maxY = cy + percentRadius + LABEL_LINE_GAP + textHalfHeight
  const upperSideY = cy - percentRadius * SIN_30 - LABEL_LINE_GAP - textHalfHeight
  const lowerSideY = cy + percentRadius * SIN_30 + LABEL_LINE_GAP + textHalfHeight

  const minX = cx - percentRadius * COS_30 - sideTextExtend
  const maxX = cx + percentRadius * COS_30 + sideTextExtend

  return {
    x: minX - viewBoxPadding,
    y: Math.min(minY, upperSideY) - viewBoxPadding,
    width: maxX - minX + viewBoxPadding * 2,
    height: Math.max(maxY, lowerSideY) - Math.min(minY, upperSideY) + viewBoxPadding * 2,
  }
}

function polygonPoints(
  radii: number[],
  maxRadius: number,
  cx: number,
  cy: number,
  count: number,
): string {
  return radii
    .map((radius, index) => {
      const { x, y } = getVertex(index, radius * maxRadius, cx, cy, count)
      return `${x},${y}`
    })
    .join(' ')
}

function labelTextAnchor(angle: number): 'start' | 'middle' | 'end' {
  const cos = Math.cos(angle)
  if (cos > 0.3) return 'start'
  if (cos < -0.3) return 'end'
  return 'middle'
}

function useChartLayout(): ChartLayout {
  const [layout, setLayout] = useState<ChartLayout>(() =>
    typeof window !== 'undefined' && window.matchMedia(DESKTOP_MEDIA_QUERY).matches
      ? DESKTOP_LAYOUT
      : MOBILE_LAYOUT,
  )

  useEffect(() => {
    const mediaQuery = window.matchMedia(DESKTOP_MEDIA_QUERY)
    const update = () => setLayout(mediaQuery.matches ? DESKTOP_LAYOUT : MOBILE_LAYOUT)
    update()
    mediaQuery.addEventListener('change', update)
    return () => mediaQuery.removeEventListener('change', update)
  }, [])

  return layout
}

export function DiscHexagonChart({ labels, values }: DiscHexagonChartProps) {
  const layout = useChartLayout()
  const { cx, cy, maxRadius, rimGap } = layout
  const percentRadius = maxRadius + rimGap
  const viewBox = useMemo(() => getTightViewBox(layout), [layout])

  const normalizedValues = values.map((value) => clamp(value, 0, 1))
  const dataPoints = polygonPoints(normalizedValues, maxRadius, cx, cy, AXES_COUNT)

  const svgStyle = layout.lockRenderWidth ? { width: viewBox.width } : undefined

  return (
    <div className={`flex justify-center ${layout.containerClassName}`}>
      <svg
        viewBox={`${viewBox.x} ${viewBox.y} ${viewBox.width} ${viewBox.height}`}
        className={layout.svgClassName}
        style={svgStyle}
        role="img"
        aria-label={`Профиль DISC — ${labels.map((label, index) => `${label} ${Math.round(normalizedValues[index] * 100)}%`).join(', ')}`}
      >
        {GRID_LEVELS.map((level) => (
          <polygon
            key={level}
            points={polygonPoints(
              Array.from({ length: AXES_COUNT }, () => level),
              maxRadius,
              cx,
              cy,
              AXES_COUNT,
            )}
            fill="none"
            stroke="currentColor"
            strokeWidth={1}
            className="text-neutral-200"
          />
        ))}

        {Array.from({ length: AXES_COUNT }, (_, index) => {
          const { x, y } = getVertex(index, maxRadius, cx, cy, AXES_COUNT)
          return (
            <line
              key={index}
              x1={cx}
              y1={cy}
              x2={x}
              y2={y}
              stroke="currentColor"
              strokeWidth={1}
              className="text-neutral-200"
            />
          )
        })}

        <polygon
          points={dataPoints}
          fill="currentColor"
          fillOpacity={0.15}
          stroke="currentColor"
          strokeWidth={2}
          strokeLinejoin="round"
          className="text-neutral-900"
        />

        {normalizedValues.map((value, index) => {
          const { x, y } = getVertex(index, value * maxRadius, cx, cy, AXES_COUNT)
          return (
            <circle
              key={index}
              cx={x}
              cy={y}
              r={4}
              fill="currentColor"
              className="text-neutral-900"
            />
          )
        })}

        {labels.map((label, index) => {
          const percent = Math.round(normalizedValues[index] * 100)
          const percentPoint = getVertex(index, percentRadius, cx, cy, AXES_COUNT)
          const labelPoint = getLabelPoint(percentPoint)
          const textAnchor = labelTextAnchor(percentPoint.angle)

          return (
            <g key={label}>
              <text
                x={percentPoint.x}
                y={percentPoint.y}
                textAnchor={textAnchor}
                dominantBaseline="middle"
                className={layout.percentClassName}
              >
                {percent}%
              </text>
              <text
                x={labelPoint.x}
                y={labelPoint.y}
                textAnchor={textAnchor}
                dominantBaseline="middle"
                className={layout.labelClassName}
              >
                {label}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}
