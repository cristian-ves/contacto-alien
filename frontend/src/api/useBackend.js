import { useContext } from 'react'
import { BackendContext } from './backendContext'

export function useBackend() {
  return useContext(BackendContext)
}
