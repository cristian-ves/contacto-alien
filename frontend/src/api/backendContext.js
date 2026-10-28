import { createContext } from 'react'

export const BackendContext = createContext({ status: 'checking', report: () => {}, recheck: () => {} })
