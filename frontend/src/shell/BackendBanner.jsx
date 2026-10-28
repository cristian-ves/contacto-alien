import Button from '@mui/material/Button'
import { TriangleAlert } from 'lucide-react'
import { useBackend } from '../api/useBackend'
import { MSG } from '../messages'

export function BackendBanner() {
  const { status, recheck } = useBackend()
  if (status !== 'down') return null
  return (
    <div className="banner" role="alert">
      <TriangleAlert size={16} />
      <span>{MSG.backendDown}</span>
      <Button size="small" color="inherit" onClick={recheck}>
        Retry
      </Button>
    </div>
  )
}
