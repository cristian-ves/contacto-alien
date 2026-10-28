import { Copy, Download } from 'lucide-react'
import { TipButton } from '../../components/TipButton'

// Title line of one listing pane: what it is, optional extra controls, and the copy and download buttons.
export function ListingToolbar({ title, detail, children, copyTip, downloadTip, onCopy, onDownload, disabled }) {
  return (
    <div className="lbar">
      <span className="lbar-title">{title}</span>
      {detail && <span className="lbar-detail">{detail}</span>}
      <span className="sp" />
      {children && <span className="lbar-extra">{children}</span>}
      <span className="lbar-btns">
        <TipButton label={copyTip} icon={Copy} onClick={onCopy} disabled={disabled} />
        <TipButton label={downloadTip} icon={Download} onClick={onDownload} disabled={disabled} />
      </span>
    </div>
  )
}
