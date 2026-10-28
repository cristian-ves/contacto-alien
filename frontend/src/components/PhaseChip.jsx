import { phaseInfo } from '../api/diagnostics'

export function PhaseChip({ phase }) {
  const { label, tone } = phaseInfo(phase)
  return <span className={`phase ${tone}`}>{label}</span>
}
